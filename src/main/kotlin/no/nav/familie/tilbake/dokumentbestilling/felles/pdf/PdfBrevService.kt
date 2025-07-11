package no.nav.familie.tilbake.dokumentbestilling.felles.pdf

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.dokumentbestilling.felles.task.PubliserJournalpostTask
import no.nav.familie.tilbake.dokumentbestilling.felles.task.PubliserJournalpostTaskData
import no.nav.familie.tilbake.http.RessursException
import no.nav.familie.tilbake.kontrakter.dokdist.AdresseType
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstidspunkt
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstype
import no.nav.familie.tilbake.kontrakter.dokdist.ManuellAdresse
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.callId
import no.nav.familie.tilbake.micrometer.TellerService
import no.nav.tilbakekreving.pdf.Dokumentvariant
import no.nav.tilbakekreving.pdf.PdfGenerator
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmottager
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.header.TekstformatererHeader
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.Brevdata
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.DokprodTilHtml
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.Dokumentkategori
import no.nav.tilbakekreving.pdf.dokumentbestilling.fritekstbrev.JournalpostIdOgDokumentId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.Base64
import java.util.Properties

@Service
class PdfBrevService(
    private val journalføringService: JournalføringService,
    private val tellerService: TellerService,
    private val taskService: TracableTaskService,
) {
    private val logger = LoggerFactory.getLogger(PdfBrevService::class.java)
    private val pdfGenerator: PdfGenerator = PdfGenerator()

    fun genererForhåndsvisning(data: Brevdata): ByteArray {
        val html = lagHtml(data)
        return pdfGenerator.genererPDFMedLogo(html, Dokumentvariant.UTKAST, data.tittel ?: data.metadata.tittel ?: data.overskrift)
    }

    fun sendBrev(
        behandling: Behandling,
        fagsak: Fagsak,
        brevtype: Brevtype,
        data: Brevdata,
        varsletBeløp: Long? = null,
        fritekst: String? = null,
    ) {
        valider(brevtype, varsletBeløp)
        val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, behandling.id.toString())
        val dokumentreferanse: JournalpostIdOgDokumentId = lagOgJournalførBrev(behandling, fagsak, brevtype, data, logContext)
        if (data.mottager != Brevmottager.VERGE &&
            !data.metadata.annenMottakersNavn.equals(data.metadata.sakspartsnavn, ignoreCase = true)
        ) {
            // Ikke tell kopier sendt til verge eller fullmektig
            tellerService.tellBrevSendt(fagsak, brevtype)
        }
        lagTaskerForUtsendingOgSporing(behandling, fagsak, brevtype, varsletBeløp, fritekst, data, dokumentreferanse, logContext)
    }

    private fun lagTaskerForUtsendingOgSporing(
        behandling: Behandling,
        fagsak: Fagsak,
        brevtype: Brevtype,
        varsletBeløp: Long?,
        fritekst: String?,
        brevdata: Brevdata,
        dokumentreferanse: JournalpostIdOgDokumentId,
        logContext: SecureLog.Context,
    ) {
        val payload =
            objectMapper.writeValueAsString(
                PubliserJournalpostTaskData(
                    behandlingId = behandling.id,
                    manuellAdresse = brevdata.metadata.mottageradresse.manuellAdresse?.let {
                        ManuellAdresse(
                            adresseType = when (it.land) {
                                "NO" -> AdresseType.norskPostadresse
                                else -> AdresseType.utenlandskPostadresse
                            },
                            adresselinje1 = it.adresselinje1,
                            adresselinje2 = it.adresselinje2,
                            adresselinje3 = it.adresselinje3,
                            postnummer = it.postnummer,
                            poststed = it.poststed,
                            land = it.land,
                        )
                    },
                ),
            )
        val properties: Properties =
            Properties().apply {
                setProperty("journalpostId", dokumentreferanse.journalpostId)
                setProperty(PropertyName.FAGSYSTEM, fagsak.fagsystem.name)
                setProperty("dokumentId", dokumentreferanse.dokumentId)
                setProperty("mottager", brevdata.mottager.name)
                setProperty("brevtype", brevtype.name)
                setProperty("ansvarligSaksbehandler", behandling.ansvarligSaksbehandler)
                setProperty("distribusjonstype", utledDistribusjonstype(brevtype).name)
                setProperty("distribusjonstidspunkt", distribusjonstidspunkt)
                varsletBeløp?.also { setProperty("varselbeløp", varsletBeløp.toString()) }
                fritekst?.also { setProperty("fritekst", Base64.getEncoder().encodeToString(fritekst.toByteArray())) }
                brevdata.tittel?.also { setProperty("tittel", it) }
            }
        logger.info(
            "Oppretter task for publisering av brev for behandlingId=${behandling.id}, eksternFagsakId=${fagsak.eksternFagsakId}",
        )
        taskService.save(Task(PubliserJournalpostTask.TYPE, payload, properties), logContext)
    }

    private fun lagOgJournalførBrev(
        behandling: Behandling,
        fagsak: Fagsak,
        brevtype: Brevtype,
        data: Brevdata,
        logContext: SecureLog.Context,
    ): JournalpostIdOgDokumentId {
        val html = lagHtml(data)

        val pdf =
            try {
                pdfGenerator.genererPDFMedLogo(html, Dokumentvariant.ENDELIG, data.tittel ?: data.metadata.tittel ?: data.overskrift)
            } catch (e: Exception) {
                SecureLog.medContext(logContext) {
                    info("Feil ved generering av brev: brevData=$data, html=$html", e)
                }
                throw e
            }

        val dokumentkategori = mapBrevtypeTilDokumentkategori(brevtype)
        val eksternReferanseId = lagEksternReferanseId(behandling, brevtype, data.mottager)

        try {
            return journalføringService.journalførUtgåendeBrev(
                behandling,
                fagsak,
                dokumentkategori,
                data.metadata,
                data.mottager,
                pdf,
                eksternReferanseId,
                logContext,
            )
        } catch (ressursException: RessursException) {
            if (ressursException.httpStatus == HttpStatus.CONFLICT) {
                logger.info("Dokarkiv svarte med 409 CONFLICT. Forsøker å hente eksisterende journalpost for $dokumentkategori")
                val journalpost =
                    journalføringService.hentJournalposter(behandling.id).find { it.eksternReferanseId == eksternReferanseId }
                        ?: error("Klarte ikke finne igjen opprettet journalpost med eksternReferanseId $eksternReferanseId")

                return JournalpostIdOgDokumentId(
                    journalpostId = journalpost.journalpostId,
                    dokumentId =
                        journalpost.dokumenter?.first()?.dokumentInfoId ?: error(
                            "Feil ved Journalføring av $dokumentkategori til ${data.mottager} for behandlingId=${behandling.id}",
                        ),
                )
            }
            throw ressursException
        }
    }

    private fun lagEksternReferanseId(
        behandling: Behandling,
        brevtype: Brevtype,
        mottager: Brevmottager,
    ): String {
        // alle brev kan potensielt bli sendt til både bruker og kopi verge. 2 av breva kan potensielt bli sendt flere gonger
        val callId = callId()
        return "${behandling.eksternBrukId}_${brevtype.name.lowercase()}_${mottager.name.lowercase()}_$callId"
    }

    private fun mapBrevtypeTilDokumentkategori(brevtype: Brevtype): Dokumentkategori =
        if (Brevtype.VEDTAK === brevtype) {
            Dokumentkategori.VEDTAKSBREV
        } else {
            Dokumentkategori.BREV
        }

    private fun lagHtml(data: Brevdata): String {
        val header = lagHeader(data)
        val innholdHtml = lagInnhold(data)
        return header + innholdHtml + data.vedleggHtml
    }

    private fun lagInnhold(data: Brevdata): String = DokprodTilHtml.dokprodInnholdTilHtml(data.brevtekst)

    private fun lagHeader(data: Brevdata): String =
        TekstformatererHeader.lagHeader(
            brevmetadata = data.metadata,
            overskrift = data.overskrift,
        )

    private fun utledDistribusjonstype(brevtype: Brevtype): Distribusjonstype =
        when (brevtype) {
            Brevtype.VARSEL, Brevtype.KORRIGERT_VARSEL, Brevtype.INNHENT_DOKUMENTASJON -> Distribusjonstype.VIKTIG
            Brevtype.VEDTAK -> Distribusjonstype.VEDTAK
            Brevtype.HENLEGGELSE -> Distribusjonstype.ANNET
        }

    private val distribusjonstidspunkt = Distribusjonstidspunkt.KJERNETID.name

    companion object {
        private fun valider(
            brevtype: Brevtype,
            varsletBeløp: Long?,
        ) {
            val harVarsletBeløp = varsletBeløp != null
            require(brevtype.gjelderVarsel() == harVarsletBeløp) {
                "Utvikler-feil: Varslet beløp skal brukes hvis, og bare hvis, brev gjelder varsel"
            }
        }
    }
}
