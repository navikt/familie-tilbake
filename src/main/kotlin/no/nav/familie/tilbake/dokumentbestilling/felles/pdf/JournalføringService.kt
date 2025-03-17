package no.nav.familie.tilbake.dokumentbestilling.felles.pdf

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.fritekstbrev.JournalpostIdOgDokumentId
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.kontrakter.BrukerIdType
import no.nav.familie.tilbake.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.familie.tilbake.kontrakter.dokarkiv.Dokumenttype
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Dokument
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Filtype
import no.nav.familie.tilbake.kontrakter.journalpost.AvsenderMottakerIdType
import no.nav.familie.tilbake.kontrakter.journalpost.Bruker
import no.nav.familie.tilbake.kontrakter.journalpost.Journalpost
import no.nav.familie.tilbake.kontrakter.journalpost.JournalposterForBrukerRequest
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.kontrakter.ytelse.Tema
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class JournalføringService(
    private val integrasjonerClient: IntegrasjonerClient,
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
) {
    private val log = TracedLogger.getLogger<JournalføringService>()

    fun hentDokument(
        journalpostId: String,
        dokumentInfoId: String,
    ): ByteArray = integrasjonerClient.hentDokument(dokumentInfoId, journalpostId)

    fun hentJournalposter(behandlingId: UUID): List<Journalpost> {
        val behandling = behandlingRepository.findById(behandlingId).orElseThrow()
        val fagsak = behandling.let { fagsakRepository.findById(it.fagsakId).orElseThrow() }
        val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, behandling.id.toString())
        val journalposter =
            fagsak.let {
                integrasjonerClient.hentJournalposterForBruker(
                    JournalposterForBrukerRequest(
                        antall = 1000,
                        brukerId =
                            Bruker(
                                id = fagsak.bruker.ident,
                                type = BrukerIdType.FNR,
                            ),
                        tema = listOf(hentTema(fagsystem = fagsak.fagsystem)),
                    ),
                    logContext,
                )
            }
        return journalposter.filter { it.sak?.fagsakId == fagsak.eksternFagsakId }
    }

    fun journalførUtgåendeBrev(
        behandling: Behandling,
        fagsak: Fagsak,
        dokumentkategori: Dokumentkategori,
        brevmetadata: Brevmetadata,
        brevmottager: Brevmottager,
        vedleggPdf: ByteArray,
        eksternReferanseId: String?,
        logContext: SecureLog.Context,
    ): JournalpostIdOgDokumentId {
        log.medContext(logContext) {
            info("Starter journalføring av {} til {} for behandlingId={}", dokumentkategori, brevmottager, behandling.id)
        }
        val dokument =
            Dokument(
                dokument = vedleggPdf,
                filtype = Filtype.PDFA,
                filnavn = if (dokumentkategori == Dokumentkategori.VEDTAKSBREV) "vedtak.pdf" else "brev.pdf",
                tittel = brevmetadata.tittel,
                dokumenttype = velgDokumenttype(fagsak, dokumentkategori),
            )
        val request =
            ArkiverDokumentRequest(
                fnr = fagsak.bruker.ident,
                forsøkFerdigstill = true,
                hoveddokumentvarianter = listOf(dokument),
                fagsakId = fagsak.eksternFagsakId,
                journalførendeEnhet = behandling.behandlendeEnhet,
                avsenderMottaker = lagMottager(behandling, brevmottager, brevmetadata),
                eksternReferanseId = eksternReferanseId,
            )

        val response = integrasjonerClient.arkiver(request)

        val dokumentinfoId =
            response.dokumenter?.first()?.dokumentInfoId
                ?: error(
                    "Feil ved Journalføring av $dokumentkategori " +
                        "til $brevmottager for behandlingId=${behandling.id}",
                )
        log.medContext(logContext) {
            info(
                "Journalførte utgående {} til {} for behandlingId={} med journalpostid={}",
                dokumentkategori,
                brevmottager,
                behandling.id,
                response.journalpostId,
            )
        }
        return JournalpostIdOgDokumentId(response.journalpostId, dokumentinfoId)
    }

    private fun lagMottager(
        behandling: Behandling,
        mottager: Brevmottager,
        brevmetadata: Brevmetadata,
    ): AvsenderMottaker {
        val adresseinfo: Adresseinfo = brevmetadata.mottageradresse
        val mottagerIdent = adresseinfo.ident.takeIf { it.isNotBlank() }
        return when (mottager) {
            Brevmottager.BRUKER,
            Brevmottager.MANUELL_BRUKER,
            Brevmottager.MANUELL_TILLEGGSMOTTAKER,
            ->
                AvsenderMottaker(
                    id = mottagerIdent,
                    idType = utledIdType(mottagerIdent),
                    navn = adresseinfo.mottagernavn,
                )
            Brevmottager.VERGE -> lagVergemottager(behandling)
            Brevmottager.INSTITUSJON -> lagInstitusjonmottager(behandling, brevmetadata)
        }
    }

    private fun utledIdType(mottagerIdent: String?) =
        when (mottagerIdent?.length) {
            0, null -> null
            9 -> AvsenderMottakerIdType.ORGNR
            11 -> AvsenderMottakerIdType.FNR
            else -> throw IllegalArgumentException("Ugyldig idType")
        }

    private fun lagInstitusjonmottager(
        behandling: Behandling,
        brevmetadata: Brevmetadata,
    ): AvsenderMottaker {
        val institusjon =
            brevmetadata.institusjon ?: throw IllegalStateException(
                "Brevmottager er institusjon, men institusjon finnes ikke. " +
                    "Fagsak ${behandling.fagsakId} og behandling ${behandling.id}",
            )
        return AvsenderMottaker(
            idType = AvsenderMottakerIdType.ORGNR,
            id = institusjon.organisasjonsnummer,
            navn = institusjon.navn,
        )
    }

    private fun lagVergemottager(behandling: Behandling): AvsenderMottaker {
        val verge: Verge =
            behandling.aktivVerge
                ?: throw IllegalStateException(
                    "Brevmottager er verge, men verge finnes ikke. " +
                        "Behandling ${behandling.id}",
                )
        return if (verge.orgNr != null) {
            AvsenderMottaker(
                idType = AvsenderMottakerIdType.ORGNR,
                id = verge.orgNr,
                navn = verge.navn,
            )
        } else {
            AvsenderMottaker(
                idType = AvsenderMottakerIdType.FNR,
                id = verge.ident!!,
                navn = verge.navn,
            )
        }
    }

    private fun velgDokumenttype(
        fagsak: Fagsak,
        dokumentkategori: Dokumentkategori,
    ): Dokumenttype =
        if (dokumentkategori == Dokumentkategori.VEDTAKSBREV) {
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

    private fun hentTema(fagsystem: Fagsystem): Tema = Tema.valueOf(fagsystem.tema)
}
