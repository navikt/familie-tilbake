package no.nav.familie.tilbake.dokumentbestilling.varsel

import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.handlebars.dto.Handlebarsperiode
import no.nav.familie.tilbake.dokumentbestilling.varsel.handlebars.dto.FeilutbetaltPeriode
import no.nav.familie.tilbake.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
import no.nav.familie.tilbake.dokumentbestilling.varsel.handlebars.dto.Vedleggsdata
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.YearMonth
import java.util.Scanner

class TekstformatererVarselbrevTest {

    private val metadata = Brevmetadata(
        sakspartId = "123456",
        sakspartsnavn = "Test",
        mottageradresse = lagAdresseinfo(),
        språkkode = Språkkode.NB,
        ytelsestype = Ytelsestype.OVERGANGSSTØNAD,
        behandlendeEnhetsNavn = "NAV Familie- og pensjonsytelser Skien",
        saksnummer = "1232456",
        ansvarligSaksbehandler = "Bob"
    )

    private val varselbrevsdokument =
        Varselbrevsdokument(
            varseltekstFraSaksbehandler = "Dette er fritekst skrevet av saksbehandler.",
            beløp = 595959L,
            feilutbetaltePerioder = lagFeilutbetalingerMedKunEnPeriode(),
            fristdatoForTilbakemelding = LocalDate.of(2020, 4, 4),
            revurderingsvedtaksdato = LocalDate.of(2019, 12, 18),
            brevmetadata = metadata
        )

    @Test
    fun `lagVarselbrevsfritekst skal generere varseltekst for flere perioder overgangsstønad`() {
        val metadata = metadata.copy(språkkode = Språkkode.NN)
        val varselbrevsdokument = varselbrevsdokument.copy(
            brevmetadata = metadata,
            feilutbetaltePerioder = lagFeilutbetalingerMedFlerePerioder()
        )
        val generertBrev = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument, false)
        val fasit = les("/varselbrev/OS_flere_perioder.txt")
        generertBrev shouldBe fasit
    }

    @Test
    fun `lagVarselbrevsfritekst skal generere varseltekst for enkelt periode overgangsstønad`() {
        val varselbrevsdokument = varselbrevsdokument.copy(feilutbetaltePerioder = lagFeilutbetalingerMedKunEnPeriode())
        val generertBrev = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument, false)
        val fasit = les("/varselbrev/OS_en_periode.txt")
        generertBrev shouldBe fasit
    }

    @Test
    fun `lagVarselbrevsfritekst skal generere varseltekst for enkelt periode barnetrygd`() {
        val metadata = metadata.copy(ytelsestype = Ytelsestype.BARNETRYGD)
        val varselbrevsdokument = varselbrevsdokument.copy(
            brevmetadata = metadata,
            feilutbetaltePerioder = lagFeilutbetalingerMedKunEnPeriode()
        )
        val generertBrev = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument, false)
        val fasit = les("/varselbrev/BA_en_periode.txt")
        generertBrev shouldBe fasit
    }

    @Test
    fun `lagVarselbrevsoverskrift skal generere varselbrevsoverskrift`() {
        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(metadata, false)
        val fasit = "NAV vurderer om du må betale tilbake overgangsstønad"
        overskrift shouldBe fasit
    }

    @Test
    fun `lagVarselbrevsoverskrift skal generere varselbrevsoverskrift nynorsk`() {
        val brevMetadata = metadata.copy(språkkode = Språkkode.NN)
        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(brevMetadata, false)
        val fasit = "NAV vurderer om du må betale tilbake overgangsstønad"
        overskrift shouldBe fasit
    }

    @Test
    fun `lagKorrigertVarselbrevsoverskrift skal generere korrigert varselbrevsoverskrift`() {
        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(metadata, true)
        val fasit = "Korrigert varsel om feilutbetalt overgangsstønad"
        overskrift shouldBe fasit
    }

    @Test
    fun `lagKorrigertVarselbrevsoverskrift skal generere korrigert varselbrevsoverskrift nynorsk`() {
        val brevMetadata = metadata.copy(språkkode = Språkkode.NN)
        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(brevMetadata, true)
        val fasit = "Korrigert varsel om feilutbetalt overgangsstønad"
        overskrift shouldBe fasit
    }

    @Test
    fun `lagVarselbrevsfritekst skal generere varselbrev for verge`() {
        val metadata = metadata.copy(
            ytelsestype = Ytelsestype.BARNETRYGD,
            vergenavn = "John Doe",
            finnesVerge = true,
            språkkode = Språkkode.NB
        )
        val varselbrevsdokument = varselbrevsdokument.copy(
            brevmetadata = metadata,
            feilutbetaltePerioder = lagFeilutbetalingerMedKunEnPeriode()
        )
        val generertBrev = TekstformatererVarselbrev.lagFritekst(varselbrevsdokument, false)
        val fasit = les("/varselbrev/BA_en_periode.txt")
        val vergeTekst = les("/varselbrev/verge.txt")
        generertBrev shouldBe "$fasit${System.lineSeparator().repeat(2)}$vergeTekst"
    }

    @Test
    fun `lagVarselbrevsvedleggHtml skal lage oversikt over varselet uten skatt på bokmål`() {

        val vedleggsdata = Vedleggsdata(
            Språkkode.NB,
            false,
            listOf(
                FeilutbetaltPeriode(
                    YearMonth.of(2022, 1),
                    BigDecimal(1572),
                    BigDecimal(1573),
                    BigDecimal(1574)
                )
            )
        )

        val html = TekstformatererVarselbrev.lagVarselbrevsvedleggHtml(vedleggsdata)

        val fasit = les("/varselbrev/vedlegg/vedlegg_nb_uten_skatt.txt")
        html shouldBe fasit
    }

    @Test
    fun `lagVarselbrevsvedleggHtml skal lage oversikt over varselet uten skatt på nynorsk`() {

        val vedleggsdata = Vedleggsdata(
            Språkkode.NN,
            false,
            listOf(
                FeilutbetaltPeriode(
                    YearMonth.of(2022, 1),
                    BigDecimal(1572),
                    BigDecimal(1573),
                    BigDecimal(1574)
                )
            )
        )

        val html = TekstformatererVarselbrev.lagVarselbrevsvedleggHtml(vedleggsdata)

        val fasit = les("/varselbrev/vedlegg/vedlegg_nn_uten_skatt.txt")
        html shouldBe fasit
    }

    @Test
    fun `lagVarselbrevsvedleggHtml skal lage oversikt over varselet med skatt på bokmål`() {

        val vedleggsdata = Vedleggsdata(
            Språkkode.NB,
            true,
            listOf(
                FeilutbetaltPeriode(
                    YearMonth.of(2022, 1),
                    BigDecimal(1572),
                    BigDecimal(1573),
                    BigDecimal(1574)
                )
            )
        )

        val html = TekstformatererVarselbrev.lagVarselbrevsvedleggHtml(vedleggsdata)

        val fasit = les("/varselbrev/vedlegg/vedlegg_nb_med_skatt.txt")
        html shouldBe fasit
    }

    @Test
    fun `lagVarselbrevsvedleggHtml skal lage oversikt over varselet med skatt på nynorsk`() {

        val vedleggsdata = Vedleggsdata(
            Språkkode.NN,
            true,
            listOf(
                FeilutbetaltPeriode(
                    YearMonth.of(2022, 1),
                    BigDecimal(1572),
                    BigDecimal(1573),
                    BigDecimal(1574)
                )
            )
        )

        val html = TekstformatererVarselbrev.lagVarselbrevsvedleggHtml(vedleggsdata)

        val fasit = les("/varselbrev/vedlegg/vedlegg_nn_med_skatt.txt")
        html shouldBe fasit
    }

    private fun lagFeilutbetalingerMedFlerePerioder(): List<Handlebarsperiode> {
        val periode1 = Handlebarsperiode(
            LocalDate.of(2019, 3, 3),
            LocalDate.of(2020, 3, 3)
        )
        val periode2 = Handlebarsperiode(
            LocalDate.of(2022, 3, 3),
            LocalDate.of(2024, 3, 3)
        )
        return listOf(periode1, periode2)
    }

    private fun lagFeilutbetalingerMedKunEnPeriode(): List<Handlebarsperiode> {
        return listOf(
            Handlebarsperiode(
                LocalDate.of(2019, 3, 3),
                LocalDate.of(2020, 3, 3)
            )
        )
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
