package no.nav.familie.tilbake.dokumentbestilling.henleggelse

import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.henleggelse.handlebars.dto.Henleggelsesbrevsdokument
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.Scanner

class TekstformatererHenleggelsesbrevTest {

    private val niendeMars = LocalDate.of(2019, 3, 9)

    private val brevmetadata = Brevmetadata(sakspartId = "12345678901",
                                            sakspartsnavn = "Test",
                                            mottageradresse = Adresseinfo("12345678901", "Test"),
                                            vergenavn = "John Doe",
                                            språkkode = Språkkode.NB,
                                            ytelsestype = Ytelsestype.BARNETILSYN,
                                            behandlendeEnhetsNavn = "NAV Familie- og pensjonsytelser Skien",
                                            ansvarligSaksbehandler = "Bob")


    private val henleggelsesbrevsdokument = Henleggelsesbrevsdokument(brevmetadata,
                                                                      niendeMars,
                                                                      REVURDERING_HENLEGGELSESBREV_FRITEKST)

    @Test
    fun `lagFritekst skal generere henleggelsesbrev`() {
        val generertBrev: String = TekstformatererHenleggelsesbrev.lagFritekst(henleggelsesbrevsdokument)
        val fasit = les("/henleggelsesbrev/henleggelsesbrev.txt")
        Assertions.assertThat(generertBrev).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagRevurderingsfritekst skal generere henleggelsesbrev for tilbakekreving revurdering`() {
        val generertBrev: String =
                TekstformatererHenleggelsesbrev.lagRevurderingsfritekst(henleggelsesbrevsdokument)
        val fasit = les("/henleggelsesbrev/henleggelsesbrev_revurdering.txt")
        Assertions.assertThat(generertBrev).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagFritekst skal generere henleggelsesbrev med verge`() {
        val brevmetadata = brevmetadata.copy(finnesVerge = true)
        val henleggelsesbrevsdokument = henleggelsesbrevsdokument.copy(brevmetadata = brevmetadata)
        val generertBrev: String = TekstformatererHenleggelsesbrev.lagFritekst(henleggelsesbrevsdokument)
        val fasit = les("/henleggelsesbrev/henleggelsesbrev.txt")
        val vergeTekst = les("/varselbrev/verge.txt")
        Assertions.assertThat(generertBrev).isEqualToNormalizingNewlines("$fasit\n\n$vergeTekst")
    }

    @Test
    fun `lagRevurderingsfritekst skal generere henleggelsesbrev for tilbakekreving revurdering med verge`() {
        val brevmetadata = brevmetadata.copy(finnesVerge = true)
        val henleggelsesbrevsdokument = henleggelsesbrevsdokument.copy(brevmetadata = brevmetadata)
        val generertBrev: String =
                TekstformatererHenleggelsesbrev.lagRevurderingsfritekst(henleggelsesbrevsdokument)
        val fasit = les("/henleggelsesbrev/henleggelsesbrev_revurdering.txt")
        val vergeTekst = les("/varselbrev/verge.txt")
        Assertions.assertThat(generertBrev).isEqualToNormalizingNewlines("$fasit\n\n$vergeTekst")
    }

    @Test
    fun `lagFritekst skal generere henleggelsesbrev nynorsk`() {
        val brevmetadata = brevmetadata.copy(språkkode = Språkkode.NN)
        val henleggelsesbrevsdokument = henleggelsesbrevsdokument.copy(brevmetadata = brevmetadata)
        val generertBrev: String = TekstformatererHenleggelsesbrev.lagFritekst(henleggelsesbrevsdokument)
        val fasit = les("/henleggelsesbrev/henleggelsesbrev_nn.txt")
        Assertions.assertThat(generertBrev).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagRevurderingsfritekst skal generere henleggelsesbrev nynorsk for tilbakekreving revurderning`() {
        val brevmetadata = brevmetadata.copy(språkkode = Språkkode.NN)
        val henleggelsesbrevsdokument = henleggelsesbrevsdokument.copy(brevmetadata = brevmetadata)
        val generertBrev: String =
                TekstformatererHenleggelsesbrev.lagRevurderingsfritekst(henleggelsesbrevsdokument)
        val fasit = les("/henleggelsesbrev/henleggelsesbrev_revurdering_nn.txt")
        Assertions.assertThat(generertBrev).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagOverskrift skal generere henleggelsesbrev overskrift`() {
        val overskrift: String = TekstformatererHenleggelsesbrev.lagOverskrift(brevmetadata)
        val fasit = "NAV har avsluttet saken din om tilbakebetaling"
        Assertions.assertThat(overskrift).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagOverskrift skal generere henleggelsesbrev overskrift for tilbakekreving revurdering`() {
        val overskrift: String =
                TekstformatererHenleggelsesbrev.lagRevurderingsoverskrift(brevmetadata)
        val fasit = "Tilbakebetaling stønad til barnetilsyn"
        Assertions.assertThat(overskrift).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagOverskrift skal generere henleggelsesbrev overskrift nynorsk`() {
        val brevmetadata = brevmetadata.copy(språkkode = Språkkode.NN)
        val overskrift: String = TekstformatererHenleggelsesbrev.lagOverskrift(brevmetadata)
        val fasit = "NAV har avslutta saka di om tilbakebetaling"
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

    companion object {

        private const val REVURDERING_HENLEGGELSESBREV_FRITEKST = "Revurderingen ble henlagt"
    }
}