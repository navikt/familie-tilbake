package no.nav.tilbakekreving.brev.informasjonsbrev

import no.nav.familie.tilbake.kontrakter.dokarkiv.AvsenderMottaker
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Dokument
import no.nav.familie.tilbake.kontrakter.dokarkiv.v2.Filtype
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstidspunkt
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstype
import no.nav.familie.tilbake.kontrakter.journalpost.AvsenderMottakerIdType
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.callId
import no.nav.tilbakekreving.behov.InfobrevDistribusjonBehov
import no.nav.tilbakekreving.behov.InfobrevJournalføringBehov
import no.nav.tilbakekreving.breeeev.InfobrevData
import no.nav.tilbakekreving.integrasjoner.dokarkiv.DokarkivClient
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.OpprettJournalpostResponse
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.DokdistClient
import no.nav.tilbakekreving.integrasjoner.pdfGen.PdfGenClient
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedavsnittInformasjonsbrevDto
import no.nav.tilbakekreving.kontrakter.frontend.models.InformasjonsbrevDataDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RentekstElementDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SignaturAutomatiskDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UnderavsnittElementDto
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.DokumentKlasse
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@Service
class InformasjonsbrevService(
    private val dokarkivClient: DokarkivClient,
    private val dokdistClient: DokdistClient,
    private val pdfGenClient: PdfGenClient,
) {
    fun journalførInformasjonsbrev(behov: InfobrevJournalføringBehov): OpprettJournalpostResponse {
        val arkiverDokumentRequest = ArkiverDokumentRequest(
            fnr = behov.bruker.ident,
            forsøkFerdigstill = true,
            hoveddokumentvarianter = listOf(
                Dokument(
                    dokument = pdfGenClient.hentPdfForInformasjonsbrev(hentInformasjonsbrevData(behov.infobrevData)),
                    filtype = Filtype.PDFA,
                    filnavn = "informasjonsbrev.pdf",
                    tittel = behov.infobrevData.tittel,
                    dokumenttype = null,
                ),
            ),
            fagsakId = behov.fagsakId,
            journalførendeEnhet = behov.journalførendeEnhet,
            eksternReferanseId = lagEksternReferanseId(behov.behandlingId),
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
            dokuemntkategori = DokumentKlasse.IB,
            behandlingId = behov.behandlingId,
        )
    }

    fun distribuerInformasjonsbrev(behov: InfobrevDistribusjonBehov, logContext: SecureLog.Context): String {
        return dokdistClient.brevTilUtsending(
            behandlingId = behov.behandlingId,
            journalpostId = behov.journalpostId,
            fagsystem = behov.ytelse.tilFagsystemDTO(),
            distribusjonstype = Distribusjonstype.ANNET,
            distribusjonstidspunkt = Distribusjonstidspunkt.KJERNETID,
            adresse = null,
            logContext = logContext,
        ).bestillingsId
    }

    private fun lagEksternReferanseId(behandlingId: UUID): String {
        return "${behandlingId}_informasjonsbrev_bruker_${callId()}"
    }

    companion object {
        private val DATOFORMAT = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.of("nb"))

        fun hentInformasjonsbrevData(infobrevData: InfobrevData): InformasjonsbrevDataDto {
            return InformasjonsbrevDataDto(
                hovedavsnitt = HovedavsnittInformasjonsbrevDto(
                    tittel = infobrevData.tittel,
                    innhold = infobrevData.innhold,
                ),
                underavsnitt = infobrevData.underavsnitt.map { avsnitt ->
                    UnderavsnittElementDto(
                        tittel = avsnitt.tittel,
                        underavsnitt = avsnitt.avsnitt.map(::RentekstElementDto),
                    )
                },
                brevGjelder = infobrevData.brukerdata,
                signatur = SignaturAutomatiskDto(enhetNavn = infobrevData.enhetNavn),
                sendtDato = DATOFORMAT.format(infobrevData.sendtDato),
                saksnummer = infobrevData.tilbakekrevingId,
            )
        }
    }
}
