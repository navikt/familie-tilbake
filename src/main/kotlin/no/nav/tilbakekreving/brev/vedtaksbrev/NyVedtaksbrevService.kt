package no.nav.tilbakekreving.brev.vedtaksbrev

import no.nav.familie.tilbake.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Dokument
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Filtype
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstidspunkt
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstype
import no.nav.familie.tilbake.kontrakter.journalpost.AvsenderMottakerIdType
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.callId
import no.nav.tilbakekreving.behov.VedtaksbrevDistribusjonBehov
import no.nav.tilbakekreving.behov.VedtaksbrevJournalføringBehov
import no.nav.tilbakekreving.breeeev.VedtaksbrevInfo
import no.nav.tilbakekreving.breeeev.begrunnelse.Forklaringstekster
import no.nav.tilbakekreving.breeeev.begrunnelse.VilkårsvurderingBegrunnelse
import no.nav.tilbakekreving.brev.vedtaksbrev.BrevFormatterer.tilDto
import no.nav.tilbakekreving.integrasjoner.dokarkiv.DokarkivClient
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostResponse
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.DokdistClient
import no.nav.tilbakekreving.kontrakter.frontend.models.AvsnittDto
import no.nav.tilbakekreving.kontrakter.frontend.models.AvsnittUpdateItemDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedavsnittDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedavsnittUpdateDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PakrevdBegrunnelseUpdateItemDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RentekstElementDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RotElementDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RotElementUpdateItemDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SignaturDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevDataDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevRedigerbareDataDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevRedigerbareDataUpdateDto
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmottager
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.DokumentKlasse
import no.tilbakekreving.integrasjoner.pdfGen.PdfGenClient
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class NyVedtaksbrevService(
    private val vedtaksbrevDataRepository: VedtaksbrevDataRepository,
    private val eksterneDataForBrevService: EksterneDataForBrevService,
    private val dokarkivClient: DokarkivClient,
    private val dokdistClient: DokdistClient,
    private val pdfGenClient: PdfGenClient,
) {
    fun hentVedtaksbrevData(behandlingId: UUID, vedtaksbrevInfo: VedtaksbrevInfo): VedtaksbrevDataDto {
        val signatur = SignaturDto(
            vedtaksbrevInfo.signatur.ansvarligEnhet,
            eksterneDataForBrevService.hentSaksbehandlernavn(vedtaksbrevInfo.signatur.ansvarligSaksbehandlerIdent),
            vedtaksbrevInfo.signatur.ansvarligBeslutterIdent?.let(eksterneDataForBrevService::hentSaksbehandlernavn),
        )

        val (sistOppdatert, lagredeData) = vedtaksbrevDataRepository.hentVedtaksbrevData(behandlingId) ?: return VedtaksbrevDataDto(
            hovedavsnitt = HovedavsnittDto(
                tittel = "Du må betale tilbake ${vedtaksbrevInfo.ytelse.bestemtEntall}",
                forklaring = Forklaringstekster.HOVEDAVSNITT,
                underavsnitt = listOf(RentekstElementDto("")),
            ),
            avsnitt = BrevFormatterer.lagAvsnitt(vedtaksbrevInfo.perioder),
            sistOppdatert = OffsetDateTime.now(),
            brevGjelder = vedtaksbrevInfo.brukerdata,
            sendtDato = BrevFormatterer.norskDato(LocalDate.now()),
            ytelse = vedtaksbrevInfo.ytelse,
            signatur = signatur,
        )

        return VedtaksbrevDataDto(
            hovedavsnitt = lagredeData.hovedavsnitt.let(::mapHovedavsnitt),
            avsnitt = lagredeData.avsnitt.map(::mapAvsnitt),
            brevGjelder = vedtaksbrevInfo.brukerdata,
            ytelse = vedtaksbrevInfo.ytelse,
            sendtDato = BrevFormatterer.norskDato(LocalDate.now()),
            sistOppdatert = sistOppdatert.atOffset(ZoneOffset.UTC),
            signatur = signatur,
        )
    }

    fun oppdaterVedtaksbrevData(behandlingId: UUID, data: VedtaksbrevRedigerbareDataUpdateDto): VedtaksbrevRedigerbareDataDto {
        val (sistOppdatert, data) = vedtaksbrevDataRepository.oppdaterVedtaksbrevData(behandlingId, data)
        return VedtaksbrevRedigerbareDataDto(
            hovedavsnitt = mapHovedavsnitt(data.hovedavsnitt),
            avsnitt = data.avsnitt.map(::mapAvsnitt),
            sistOppdatert = sistOppdatert.atOffset(ZoneOffset.UTC),
        )
    }

    fun mapHovedavsnitt(hovedavsnitt: HovedavsnittUpdateDto): HovedavsnittDto {
        return HovedavsnittDto(
            tittel = hovedavsnitt.tittel,
            forklaring = Forklaringstekster.HOVEDAVSNITT,
            underavsnitt = hovedavsnitt.underavsnitt.map(::mapUnderavsnitt),
        )
    }

    fun mapAvsnitt(avsnitt: AvsnittUpdateItemDto): AvsnittDto {
        return AvsnittDto(
            tittel = avsnitt.tittel,
            forklaring = Forklaringstekster.PERIODE_AVSNITT,
            id = avsnitt.id,
            meldingerTilSaksbehandler = emptyList(),
            underavsnitt = avsnitt.underavsnitt.map(::mapUnderavsnitt),
        )
    }

    fun mapUnderavsnitt(avsnitt: RotElementUpdateItemDto): RotElementDto {
        return when (avsnitt) {
            is PakrevdBegrunnelseUpdateItemDto -> VilkårsvurderingBegrunnelse.valueOf(avsnitt.begrunnelseType).tilDto(emptyList(), avsnitt.underavsnitt)
            is RotElementDto -> avsnitt
        }
    }

    fun journalførVedtaksbrev(
        behov: VedtaksbrevJournalføringBehov,
    ): OpprettJournalpostResponse {
        val arkiverDokumentRequest = ArkiverDokumentRequest(
            fnr = behov.bruker.ident,
            forsøkFerdigstill = true,
            hoveddokumentvarianter = listOf(
                Dokument(
                    dokument = pdfGenClient.hentPdfForVedtak(hentVedtaksbrevData(behov.behandlingId, behov.vedtaksbrevInfo)),
                    filtype = Filtype.PDFA,
                    filnavn = "vedtak.pdf",
                ),
            ),
            fagsakId = behov.fagsakId,
            journalførendeEnhet = behov.journalførendeEnhet,
            eksternReferanseId = lagEksternReferanseId(behov.behandlingId, Brevtype.VEDTAK, Brevmottager.BRUKER),
            avsenderMottaker = AvsenderMottaker(
                id = behov.bruker.ident,
                idType = AvsenderMottakerIdType.FNR,
                navn = behov.bruker.navn,
            ),
        )

        return dokarkivClient.opprettOgSendJournalpostRequest(
            arkiverDokument = arkiverDokumentRequest,
            fagsaksystem = behov.ytelse.tilDokarkivFagsaksystem(),
            brevkode = behov.ytelse.tilFagsystemDTO().name + "-TILB",
            tema = behov.ytelse.tilTema(),
            dokuemntkategori = DokumentKlasse.VB,
            behandlingId = behov.behandlingId,
        )
    }

    fun distribuereVedtaksbrev(behov: VedtaksbrevDistribusjonBehov, logContext: SecureLog.Context): String {
        return dokdistClient.brevTilUtsending(
            behandlingId = behov.behandlingId,
            journalpostId = behov.journalpostId,
            fagsystem = behov.fagsystem,
            distribusjonstype = Distribusjonstype.VEDTAK,
            distribusjonstidspunkt = Distribusjonstidspunkt.KJERNETID,
            adresse = null,
            logContext = logContext,
        ).bestillingsId
    }

    private fun lagEksternReferanseId(
        behandlingId: UUID,
        brevtype: Brevtype,
        mottager: Brevmottager,
    ): String {
        val callId = callId()
        return "${behandlingId}_${brevtype.name.lowercase()}_${mottager.name.lowercase()}_$callId"
    }
}
