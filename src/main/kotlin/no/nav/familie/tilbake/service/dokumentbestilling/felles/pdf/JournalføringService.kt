package no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf

import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.service.dokumentbestilling.fritekstbrev.JournalpostIdOgDokumentId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class JournalføringService(private val integrasjonerClient: IntegrasjonerClient) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentDokument(journalpostId: String, dokumentInfoId: String): ByteArray {
        return integrasjonerClient.hentDokument(dokumentInfoId, journalpostId)
    }

    fun journalførUtgåendeBrev(behandling: Behandling,
                               fagsak: Fagsak,
                               dokumentkategori: Dokumentkategori,
                               brevmetadata: Brevmetadata,
                               brevmottager: Brevmottager,
                               vedleggPdf: ByteArray): JournalpostIdOgDokumentId {
        logger.info("Starter journalføring av {} til {} for behandlingId={}", dokumentkategori, brevmottager, behandling.id)
        val dokument = Dokument(dokument = vedleggPdf,
                                filtype = Filtype.PDFA,
                                filnavn = if (dokumentkategori == Dokumentkategori.VEDTAKSBREV) "vedtak.pdf" else "brev.pdf",
                                tittel = brevmetadata.tittel,
                                dokumenttype = velgDokumenttype(fagsak, dokumentkategori))
        val request = ArkiverDokumentRequest(fnr = fagsak.bruker.ident,
                                             forsøkFerdigstill = true,
                                             hoveddokumentvarianter = listOf(dokument),
                                             fagsakId = fagsak.eksternFagsakId,
                                             journalførendeEnhet = behandling.behandlendeEnhet,
                                             avsenderMottaker = lagMottager(behandling, brevmottager, brevmetadata))


        val response = integrasjonerClient.arkiver(request)

        val dokumentinfoId = response.dokumenter?.first()?.dokumentInfoId
                             ?: error("Feil ved Journalføring av $dokumentkategori " +
                                      "til $brevmottager for behandlingId=${behandling.id}")
        logger.info("Journalførte utgående {} til {} for behandlingId={} med journalpostid={}",
                    dokumentkategori,
                    brevmottager,
                    behandling.id,
                    response.journalpostId)
        return JournalpostIdOgDokumentId(response.journalpostId, dokumentinfoId)
    }

    private fun lagMottager(behandling: Behandling, mottager: Brevmottager, brevmetadata: Brevmetadata): AvsenderMottaker {
        val adresseinfo: Adresseinfo = brevmetadata.mottageradresse
        return when (mottager) {
            Brevmottager.BRUKER -> AvsenderMottaker(id = adresseinfo.ident,
                                                    idType = BrukerIdType.FNR,
                                                    navn = adresseinfo.mottagernavn)
            Brevmottager.VERGE -> lagVergemottager(behandling)
        }
    }

    private fun lagVergemottager(behandling: Behandling): AvsenderMottaker {
        val verge: Verge = behandling.aktivVerge
                           ?: throw IllegalStateException("Brevmottager er verge, men verge finnes ikke. " +
                                                          "Behandling ${behandling.id}")
        return if (verge.orgNr != null) {
            AvsenderMottaker(idType = BrukerIdType.ORGNR,
                             id = verge.orgNr,
                             navn = verge.navn)
        } else {
            AvsenderMottaker(idType = BrukerIdType.FNR,
                             id = verge.ident!!,
                             navn = verge.navn)
        }
    }

    private fun velgDokumenttype(fagsak: Fagsak, dokumentkategori: Dokumentkategori): Dokumenttype {
        return if (dokumentkategori == Dokumentkategori.VEDTAKSBREV) {
            when (fagsak.ytelsestype) {
                Ytelsestype.BARNETRYGD -> Dokumenttype.BARNETRYGD_TILBAKEKREVING_VEDTAK
                Ytelsestype.OVERGANGSSTØNAD -> Dokumenttype.OVERGANGSSTØNAD_TILBAKEKREVING_VEDTAK
                Ytelsestype.BARNETILSYN -> Dokumenttype.BARNETILSYN_TILBAKEKREVING_VEDTAK
                Ytelsestype.SKOLEPENGER -> Dokumenttype.SKOLEPENGER_TILBAKEKREVING_VEDTAK
                Ytelsestype.KONTANTSTØTTE -> Dokumenttype.KONTANTSTØTTE_TILBAKEKREVING_VEDTAK
            }
        } else {
            when (fagsak.ytelsestype) {
                Ytelsestype.BARNETRYGD -> Dokumenttype.BARNETRYGD_TILBAKEKREVING_BREV
                Ytelsestype.OVERGANGSSTØNAD -> Dokumenttype.OVERGANGSSTØNAD_TILBAKEKREVING_BREV
                Ytelsestype.BARNETILSYN -> Dokumenttype.BARNETILSYN_TILBAKEKREVING_BREV
                Ytelsestype.SKOLEPENGER -> Dokumenttype.SKOLEPENGER_TILBAKEKREVING_BREV
                Ytelsestype.KONTANTSTØTTE -> Dokumenttype.KONTANTSTØTTE_TILBAKEKREVING_BREV
            }
        }
    }
}
