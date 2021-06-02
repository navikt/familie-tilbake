package no.nav.familie.tilbake.service.dokumentbestilling.varsel

import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto.Handlebarsperiode
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.Scanner

class TekstformatererVarselbrevTest {

    private val metadata = Brevmetadata(sakspartId = "123456",
                                        sakspartsnavn = "Test",
                                        mottageradresse = lagAdresseinfo(),
                                        språkkode = Språkkode.NB,
                                        ytelsestype = Ytelsestype.OVERGANGSSTØNAD,
                                        behandlendeEnhetsNavn = "Skien",
                                        ansvarligSaksbehandler = "Bob")

    private val varselbrevsdokument =
            Varselbrevsdokument(varseltekstFraSaksbehandler = "Dette er fritekst skrevet av saksbehandler.",
                                beløp = 595959L,
                                feilutbetaltePerioder = lagFeilutbetalingerMedKunEnPeriode(),
                                fristdatoForTilbakemelding = LocalDate.of(2020, 4, 4),
                                endringsdato = LocalDate.of(2019, 12, 18),
                                brevmetadata = metadata)


    @Test
    fun `lagVarselbrevsfritekst skal generere varseltekst for flere perioder overgangsstønad`() {
        val metadata = metadata.copy(språkkode = Språkkode.NN)
        val varselbrevsdokument = varselbrevsdokument.copy(brevmetadata = metadata,
                                                           feilutbetaltePerioder = lagFeilutbetalingerMedFlerePerioder())
        val generertBrev = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument)
        val fasit = les("/varselbrev/OS_flere_perioder.txt")
        assertThat(generertBrev).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagVarselbrevsfritekst skal generere varseltekst for enkelt periode overgangsstønad`() {
        val varselbrevsdokument = varselbrevsdokument.copy(feilutbetaltePerioder = lagFeilutbetalingerMedKunEnPeriode())
        val generertBrev = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument)
        val fasit = les("/varselbrev/OS_en_periode.txt")
        assertThat(generertBrev).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagVarselbrevsfritekst skal generere varseltekst for enkelt periode barnetrygd`() {
        val metadata = metadata.copy(ytelsestype = Ytelsestype.BARNETRYGD)
        val varselbrevsdokument = varselbrevsdokument.copy(brevmetadata = metadata,
                                                           feilutbetaltePerioder = lagFeilutbetalingerMedKunEnPeriode())
        val generertBrev = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument)
        val fasit = les("/varselbrev/BA_en_periode.txt")
        assertThat(generertBrev).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagVarselbrevsoverskrift skal generere varselbrevsoverskrift`() {
        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(metadata)
        val fasit = "NAV vurderer om du må betale tilbake overgangsstønad"
        assertThat(overskrift).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagVarselbrevsoverskrift skal generere varselbrevsoverskrift nynorsk`() {
        val brevMetadata = metadata.copy(språkkode = Språkkode.NN)
        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(brevMetadata)
        val fasit = "NAV vurderer om du må betale tilbake overgangsstønad"
        assertThat(overskrift).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagKorrigertVarselbrevsoverskrift skal generere korrigert varselbrevsoverskrift`() {
        val overskrift = TekstformatererVarselbrev.lagKorrigertVarselbrevsoverskrift(metadata)
        val fasit = "Korrigert varsel om feilutbetalt overgangsstønad"
        assertThat(overskrift).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagKorrigertVarselbrevsoverskrift skal generere korrigert varselbrevsoverskrift nynorsk`() {
        val brevMetadata = metadata.copy(språkkode = Språkkode.NN)
        val overskrift = TekstformatererVarselbrev.lagKorrigertVarselbrevsoverskrift(brevMetadata)
        val fasit = "Korrigert varsel om feilutbetalt overgangsstønad"
        assertThat(overskrift).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagVarselbrevsfritekst skal generere varselbrev for verge`() {
        val metadata = metadata.copy(vergenavn = "John Doe",
                                     finnesVerge = true,
                                     språkkode = Språkkode.NB)
        val varselbrevSamletInfo = varselbrevsdokument.copy(brevmetadata = metadata)
        val generertBrev = TekstformatererVarselbrev.lagFritekst(varselbrevSamletInfo)
        val fasit = les("/varselbrev/OS_en_periode.txt")
        val vergeTekst = les("/varselbrev/verge.txt")
        assertThat(generertBrev).isEqualToNormalizingNewlines("$fasit\n\n$vergeTekst")
    }

    private fun lagFeilutbetalingerMedFlerePerioder(): List<Handlebarsperiode> {
        val periode1 = Handlebarsperiode(LocalDate.of(2019, 3, 3),
                                         LocalDate.of(2020, 3, 3))
        val periode2 = Handlebarsperiode(LocalDate.of(2022, 3, 3),
                                         LocalDate.of(2024, 3, 3))
        return listOf(periode1, periode2)
    }


    private fun lagFeilutbetalingerMedKunEnPeriode(): List<Handlebarsperiode> {
        return listOf(Handlebarsperiode(LocalDate.of(2019, 3, 3),
                                        LocalDate.of(2020, 3, 3)))
    }


    private fun les(filnavn: String): String? {
        javaClass.getResourceAsStream(filnavn).use { resource ->
            Scanner(resource, StandardCharsets.UTF_8).use { scanner ->
                scanner.useDelimiter("\\A")
                return if (scanner.hasNext()) scanner.next() else null
            }
        }
    }

    private fun lagAdresseinfo(): Adresseinfo {
        return Adresseinfo("123456", "Test")
    }
}
