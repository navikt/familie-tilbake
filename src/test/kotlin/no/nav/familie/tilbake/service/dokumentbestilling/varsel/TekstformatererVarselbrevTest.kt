package no.nav.familie.tilbake.service.dokumentbestilling.varsel

import no.nav.familie.kontrakter.felles.tilbakekreving.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
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

    private val varselbrevSamletInfo =
            VarselbrevSamletInfo(fritekstFraSaksbehandler = "Dette er fritekst skrevet av saksbehandler.",
                                 sumFeilutbetaling = 595959L,
                                 feilutbetaltePerioder = lagFeilutbetalingerMedKunEnPeriode(),
                                 fristdato = LocalDate.of(2020, 4, 4),
                                 revurderingsvedtaksdato = LocalDate.of(2019, 12, 18),
                                 brevmetadata = metadata)


    @Test
    fun `lagVarselbrevsfritekst skal generere varseltekst for flere perioder overgangsstønad`() {
        val metadata = metadata.copy(språkkode = Språkkode.NN)
        val varselbrevSamletInfo = varselbrevSamletInfo.copy(brevmetadata = metadata,
                                                             feilutbetaltePerioder = lagFeilutbetalingerMedFlerePerioder())
        val generertBrev = TekstformatererVarselbrev.lagVarselbrevsfritekst(varselbrevSamletInfo)
        val fasit = les("/varselbrev/OS_flere_perioder.txt")
        assertThat(generertBrev).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagVarselbrevsfritekst skal generere varseltekst for enkelt periode overgangsstønad`() {
        val varselbrevSamletInfo = varselbrevSamletInfo.copy(feilutbetaltePerioder = lagFeilutbetalingerMedKunEnPeriode())
        val generertBrev = TekstformatererVarselbrev.lagVarselbrevsfritekst(varselbrevSamletInfo)
        val fasit = les("/varselbrev/OS_en_periode.txt")
        assertThat(generertBrev).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagVarselbrevsfritekst skal generere varseltekst for enkelt periode barnetrygd`() {
        val metadata = metadata.copy(ytelsestype = Ytelsestype.BARNETRYGD)
        val varselbrevSamletInfo = varselbrevSamletInfo.copy(brevmetadata = metadata,
                                                             feilutbetaltePerioder = lagFeilutbetalingerMedKunEnPeriode())
        val generertBrev = TekstformatererVarselbrev.lagVarselbrevsfritekst(varselbrevSamletInfo)
        val fasit = les("/varselbrev/BA_en_periode.txt")
        assertThat(generertBrev).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `mapTilVarselbrevsdokument skal mappe verdier fra dtoer til komplett tilbakekrevingsvarsel`() {
        val varselbrevSamletInfo =
                varselbrevSamletInfo.copy(fristdato = LocalDate.of(2018, 5, 27),
                                          revurderingsvedtaksdato = LocalDate.of(2018, 5, 6))
        val varselbrev: Varselbrevsdokument = TekstformatererVarselbrev.mapTilVarselbrevsdokument(varselbrevSamletInfo)
        assertThat(varselbrev.endringsdato).isEqualTo(LocalDate.of(2018, 5, 6))
        assertThat(varselbrev.fristdatoForTilbakemelding).isEqualTo(LocalDate.of(2018, 5, 27))
        assertThat(varselbrev.varseltekstFraSaksbehandler).isEqualTo("Dette er fritekst skrevet av saksbehandler.")
        assertThat(varselbrev.datoerHvisSammenhengendePeriode?.fom).isEqualTo(LocalDate.of(2019, 3, 3))
        assertThat(varselbrev.datoerHvisSammenhengendePeriode?.tom).isEqualTo(LocalDate.of(2020, 3, 3))
        assertThat(varselbrev.ytelsesnavnUbestemt).isEqualTo("overgangsstønad")
        assertThat(varselbrev.beløp).isEqualTo(595959L)
        assertThat(varselbrev.feilutbetaltePerioder).isNotNull()
    }

    @Test
    fun `mapTilVarselbrevsdokument skal ikke sette tidligste og seneste dato når det foreligger flere perioder`() {
        val varselbrevSamletInfo = varselbrevSamletInfo.copy(feilutbetaltePerioder = lagFeilutbetalingerMedFlerePerioder())
        val varselbrev: Varselbrevsdokument = TekstformatererVarselbrev.mapTilVarselbrevsdokument(varselbrevSamletInfo)
        assertThat(varselbrev.datoerHvisSammenhengendePeriode).isNull()
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
        val varselbrevSamletInfo = varselbrevSamletInfo.copy(brevmetadata = metadata)
        val generertBrev = TekstformatererVarselbrev.lagVarselbrevsfritekst(varselbrevSamletInfo)
        val fasit = les("/varselbrev/OS_en_periode.txt")
        val vergeTekst = les("/varselbrev/verge.txt")
        assertThat(generertBrev).isEqualToNormalizingNewlines("$fasit\n\n$vergeTekst")
    }

    private fun lagFeilutbetalingerMedFlerePerioder(): List<Periode> {
        val periode1 = Periode(LocalDate.of(2019, 3, 3),
                               LocalDate.of(2020, 3, 3))
        val periode2 = Periode(LocalDate.of(2022, 3, 3),
                               LocalDate.of(2024, 3, 3))
        return listOf(periode1, periode2)
    }


    private fun lagFeilutbetalingerMedKunEnPeriode(): List<Periode> {
        return listOf(Periode(LocalDate.of(2019, 3, 3),
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
        return Adresseinfo("Test", "123456")
    }
}
