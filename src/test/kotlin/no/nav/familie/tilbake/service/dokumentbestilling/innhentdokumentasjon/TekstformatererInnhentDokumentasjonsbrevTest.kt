package no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon

import no.nav.familie.kontrakter.felles.tilbakekreving.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.Scanner

class TekstformatererInnhentDokumentasjonsbrevTest {

    private val metadata = Brevmetadata(sakspartId = "12345678901",
                                        sakspartsnavn = "Test",
                                        mottageradresse = Adresseinfo("Test", "12345678901"),
                                        språkkode = Språkkode.NB,
                                        ytelsestype = Ytelsestype.BARNETILSYN,
                                        behandlendeEnhetsNavn = "Skien",
                                        ansvarligSaksbehandler = "Bob")
    private val innhentDokumentasjonsbrevSamletInfo =
            InnhentDokumentasjonsbrevSamletInfo(brevmetadata = metadata,
                                                fritekstFraSaksbehandler = "Dette er ein fritekst.",
                                                fristdato = LocalDate.of(2020, 3, 2))


    @Test
    fun `lagInnhentDokumentasjonBrevFritekst skal generere innhentdokumentasjonbrev`() {
        val dokumentasjonBrevSamletInfo =
                InnhentDokumentasjonsbrevSamletInfo(brevmetadata = metadata,
                                                    fritekstFraSaksbehandler = "Dette er ein fritekst.",
                                                    fristdato = LocalDate.of(2020, 3, 2))

        val generertBrev =
                TekstformatererInnhentDokumentasjonsbrev.lagInnhentDokumentasjonsbrevsfritekst(dokumentasjonBrevSamletInfo)

        val fasit = les("/innhentdokumentasjonbrev/innhentdokumentasjonbrev.txt")
        Assertions.assertThat(generertBrev).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagInnhentDokumentasjonBrevFritekst skal generere innhentdokumentasjonbrev for verge`() {
        val brevMetadata = metadata.copy(vergenavn = "John Doe", finnesVerge = true)
        val innhentDokumentasjonBrevSamletInfo = innhentDokumentasjonsbrevSamletInfo.copy(brevmetadata = brevMetadata)

        val generertBrev =
                TekstformatererInnhentDokumentasjonsbrev.lagInnhentDokumentasjonsbrevsfritekst(innhentDokumentasjonBrevSamletInfo)

        val fasit = les("/innhentdokumentasjonbrev/innhentdokumentasjonbrev.txt")
        val vergeTekst = les("/varselbrev/verge.txt")
        Assertions.assertThat(generertBrev).isEqualToNormalizingNewlines("$fasit\n\n$vergeTekst")
    }

    @Test
    fun `lagInnhentDokumentasjonBrevFritekst skal generere innhentdokumentasjonbrev for verge organisasjon`() {
        val brevMetadata = metadata.copy(mottageradresse = Adresseinfo(personIdent = "12345678901",
                                                                       mottagernavn = "Semba AS c/o John Doe"),
                                         sakspartsnavn = "Test",
                                         vergenavn = "John Doe",
                                         finnesVerge = true)
        val innhentDokumentasjonBrevSamletInfo = innhentDokumentasjonsbrevSamletInfo.copy(brevmetadata = brevMetadata)

        val generertBrev =
                TekstformatererInnhentDokumentasjonsbrev.lagInnhentDokumentasjonsbrevsfritekst(innhentDokumentasjonBrevSamletInfo)

        val fasit = les("/innhentdokumentasjonbrev/innhentdokumentasjonbrev.txt")
        val vergeTekst = "Brev med likt innhold er sendt til Test"
        Assertions.assertThat(generertBrev).isEqualToNormalizingNewlines("$fasit\n\n$vergeTekst")
    }

    @Test
    fun `lagInnhentDokumentasjonBrevFritekst skal generere innhentdokumentasjonbrev nynorsk`() {
        val brevMetadata = metadata.copy(språkkode = Språkkode.NN)
        val innhentDokumentasjonBrevSamletInfo = innhentDokumentasjonsbrevSamletInfo.copy(brevmetadata = brevMetadata)

        val generertBrev =
                TekstformatererInnhentDokumentasjonsbrev.lagInnhentDokumentasjonsbrevsfritekst(innhentDokumentasjonBrevSamletInfo)

        val fasit = les("/innhentdokumentasjonbrev/innhentdokumentasjonbrev_nn.txt")
        Assertions.assertThat(generertBrev).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagInnhentDokumentasjonBrevOverskrift skal generere innhentdokumentasjonbrev overskrift`() {
        val overskrift = TekstformatererInnhentDokumentasjonsbrev
                .lagInnhentDokumentasjonsbrevsoverskrift(innhentDokumentasjonsbrevSamletInfo)

        val fasit = "Vi trenger flere opplysninger"
        Assertions.assertThat(overskrift).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagInnhentDokumentasjonBrevOverskrift skal generere innhentdokumentasjonbrev overskrift nynorsk`() {
        val brevMetadata = metadata.copy(språkkode = Språkkode.NN)
        val innhentDokumentasjonBrevSamletInfo = innhentDokumentasjonsbrevSamletInfo.copy(brevmetadata = brevMetadata)

        val overskrift = TekstformatererInnhentDokumentasjonsbrev
                .lagInnhentDokumentasjonsbrevsoverskrift(innhentDokumentasjonBrevSamletInfo)

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
