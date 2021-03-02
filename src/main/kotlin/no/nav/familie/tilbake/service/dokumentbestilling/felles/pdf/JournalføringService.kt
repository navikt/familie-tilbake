package no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf

import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.AvsenderMottaker
import no.nav.familie.kontrakter.felles.dokarkiv.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.FilType
import no.nav.familie.kontrakter.felles.dokarkiv.IdType
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

    // TODO utvid ArkiverDokumentRequest med mottager
    private fun lagMottager(behandling: Behandling, mottager: Brevmottager, brevmetadata: Brevmetadata): AvsenderMottaker {
        val adresseinfo: Adresseinfo = brevmetadata.mottageradresse
        return when (mottager) {
            Brevmottager.BRUKER -> AvsenderMottaker(id = adresseinfo.personIdent,
                                                    idType = IdType.FNR,
                                                    navn = adresseinfo.mottagernavn)
            Brevmottager.VERGE -> lagVergemottager(behandling)
        }
    }

    private fun lagVergemottager(behandling: Behandling): AvsenderMottaker {
        val verge: Verge = behandling.aktivVerge
                           ?: throw IllegalStateException("Brevmottager er verge, men verge finnes ikke. " +
                                                          "Behandling ${behandling.id}")
        return if (verge.orgNr != null) {
            AvsenderMottaker(idType = IdType.ORGNR,
                             id = verge.orgNr,
                             navn = verge.navn)
        } else {
            AvsenderMottaker(idType = IdType.ORGNR,
                             id = verge.ident!!,
                             navn = verge.navn)
        }
    }

    fun journalførUtgåendeBrev(behandling: Behandling,
                               fagsak: Fagsak,
                               dokumentkategori: Dokumentkategori,
                               brevmetadata: Brevmetadata,
                               brevmottager: Brevmottager,
                               vedleggPdf: ByteArray): JournalpostIdOgDokumentId {
        logger.info("Starter journalføring av {} til {} for behandlingId={}", dokumentkategori, brevmottager, behandling.id)
        val dokument = Dokument(dokument = vedleggPdf,
                                filType = FilType.PDFA,
                                filnavn = if (dokumentkategori == Dokumentkategori.VEDTAKSBREV) "vedtak.pdf" else "brev.pdf",
                                tittel = brevmetadata.tittel,
                                dokumentType = brevmetadata.ytelsestype.kode + "-TILB")
        val request = ArkiverDokumentRequest(fnr = fagsak.bruker.ident,
                                             forsøkFerdigstill = true,
                                             hoveddokumentvarianter = listOf(dokument),
                                             fagsakId = fagsak.eksternFagsakId,
                                             journalførendeEnhet = behandling.behandlendeEnhet)

        val response = integrasjonerClient.arkiver(request)
        logger.info("Journalførte utgående {} til {} for behandlingId={} med journalpostid={}",
                    dokumentkategori,
                    brevmottager,
                    behandling.id,
                    response.journalpostId)
        return JournalpostIdOgDokumentId(response.journalpostId)
//  Todo utvide respons fra familie-integrasjoner     response.getDokumenter().get(0).getDokumentInfoId())
    }
}
