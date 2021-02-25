package no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon

import no.nav.familie.kontrakter.felles.tilbakekreving.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevMetadata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.Scanner

class TekstformatererInnhentDokumentasjonbrevTest {

    private val brevMetadata = BrevMetadata(sakspartId = "12345678901",
                                            sakspartNavn = "Test",
                                            mottakerAdresse = Adresseinfo("Test", "12345678901"),
                                            språkkode = Språkkode.NB,
                                            ytelsestype = Ytelsestype.BARNETILSYN,
                                            behandlendeEnhetNavn = "Skien",
                                            ansvarligSaksbehandler = "Bob")
    private val innhentDokumentasjonBrevSamletInfo =
            InnhentDokumentasjonsbrevSamletInfo(brevMetadata = brevMetadata,
                                                fritekstFraSaksbehandler = "Dette er ein fritekst.",
                                                fristDato = LocalDate.of(2020, 3, 2))


    @Test
    fun `skal generere innhentdokumentasjonbrev`() {
        val innhentDokumentasjonBrevSamletInfo =
                InnhentDokumentasjonsbrevSamletInfo(brevMetadata = brevMetadata,
                                                    fritekstFraSaksbehandler = "Dette er ein fritekst.",
                                                    fristDato = LocalDate.of(2020, 3, 2))

        val generertBrev =
                TekstformatererInnhentDokumentasjonbrev.lagInnhentDokumentasjonBrevFritekst(innhentDokumentasjonBrevSamletInfo)

        val fasit = les("/innhentdokumentasjonbrev/innhentdokumentasjonbrev.txt")
        Assertions.assertThat(generertBrev).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagInnhentDokumentasjonBrevFritekst skal generere innhentdokumentasjonbrev for verge`() {
        val brevMetadata = brevMetadata.copy(vergeNavn = "John Doe", finnesVerge = true)
        val innhentDokumentasjonBrevSamletInfo = innhentDokumentasjonBrevSamletInfo.copy(brevMetadata = brevMetadata)

        val generertBrev =
                TekstformatererInnhentDokumentasjonbrev.lagInnhentDokumentasjonBrevFritekst(innhentDokumentasjonBrevSamletInfo)

        val fasit = les("/innhentdokumentasjonbrev/innhentdokumentasjonbrev.txt")
        val vergeTekst = les("/varselbrev/verge.txt")
        Assertions.assertThat(generertBrev).isEqualToNormalizingNewlines("$fasit\n\n$vergeTekst")
    }

    @Test
    fun `lagInnhentDokumentasjonBrevFritekst skal generere innhentdokumentasjonbrev for verge organisasjon`() {
        val brevMetadata = brevMetadata.copy(mottakerAdresse = Adresseinfo(personIdent = "12345678901",
                                                                           mottakerNavn = "Semba AS c/o John Doe"),
                                             sakspartNavn = "Test",
                                             vergeNavn = "John Doe",
                                             finnesVerge = true)
        val innhentDokumentasjonBrevSamletInfo = innhentDokumentasjonBrevSamletInfo.copy(brevMetadata = brevMetadata)

        val generertBrev =
                TekstformatererInnhentDokumentasjonbrev.lagInnhentDokumentasjonBrevFritekst(innhentDokumentasjonBrevSamletInfo)

        val fasit = les("/innhentdokumentasjonbrev/innhentdokumentasjonbrev.txt")
        val vergeTekst = "Brev med likt innhold er sendt til Test"
        Assertions.assertThat(generertBrev).isEqualToNormalizingNewlines("$fasit\n\n$vergeTekst")
    }

    @Test
    fun `lagInnhentDokumentasjonBrevFritekst skal generere innhentdokumentasjonbrev nynorsk`() {
        val brevMetadata = brevMetadata.copy(språkkode = Språkkode.NN)
        val innhentDokumentasjonBrevSamletInfo = innhentDokumentasjonBrevSamletInfo.copy(brevMetadata = brevMetadata)

        val generertBrev =
                TekstformatererInnhentDokumentasjonbrev.lagInnhentDokumentasjonBrevFritekst(innhentDokumentasjonBrevSamletInfo)

        val fasit = les("/innhentdokumentasjonbrev/innhentdokumentasjonbrev_nn.txt")
        Assertions.assertThat(generertBrev).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagInnhentDokumentasjonBrevOverskrift skal generere innhentdokumentasjonbrev overskrift`() {
        val overskrift =
                TekstformatererInnhentDokumentasjonbrev.lagInnhentDokumentasjonBrevOverskrift(innhentDokumentasjonBrevSamletInfo)

        val fasit = "Vi trenger flere opplysninger"
        Assertions.assertThat(overskrift).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagInnhentDokumentasjonBrevOverskrift skal generere innhentdokumentasjonbrev overskrift nynorsk`() {
        val brevMetadata = brevMetadata.copy(språkkode = Språkkode.NN)
        val innhentDokumentasjonBrevSamletInfo = innhentDokumentasjonBrevSamletInfo.copy(brevMetadata = brevMetadata)

        val overskrift =
                TekstformatererInnhentDokumentasjonbrev.lagInnhentDokumentasjonBrevOverskrift(innhentDokumentasjonBrevSamletInfo)

        val fasit = "Vi trenger fleire opplysningar"
        Assertions.assertThat(overskrift).isEqualToNormalizingNewlines(fasit)
    }

    private fun les(filnavn: String): String? {
        javaClass.getResourceAsStream(filnavn).use { resource ->
            Scanner(resource, StandardCharsets.UTF_8).use { scanner ->
                scanner.useDelimiter("\\A")
                return if (scanner.hasNext()) scanner.next() else null
            }
        }
    }


}
