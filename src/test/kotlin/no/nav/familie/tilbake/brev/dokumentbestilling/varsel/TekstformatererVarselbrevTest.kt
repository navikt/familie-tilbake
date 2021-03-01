package no.nav.familie.tilbake.brev.dokumentbestilling.varsel

import no.nav.familie.kontrakter.felles.tilbakekreving.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.brev.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.brev.dokumentbestilling.felles.BrevMetadata
import no.nav.familie.tilbake.brev.dokumentbestilling.handlebars.dto.periode.HbPeriode
import no.nav.familie.tilbake.brev.dokumentbestilling.varsel.handlebars.dto.VarselbrevDokument
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.Scanner

class TekstformatererVarselbrevTest {

    private val metadata = BrevMetadata(sakspartId = "123456",
                                        sakspartNavn = "Test",
                                        mottakerAdresse = lagAdresseInfo(),
                                        språkkode = Språkkode.NB,
                                        ytelsestype = Ytelsestype.OVERGANGSSTØNAD,
                                        behandlendeEnhetNavn = "Skien",
                                        ansvarligSaksbehandler = "Bob")

    private val varselbrevSamletInfo =
            VarselbrevSamletInfo(fritekstFraSaksbehandler = "Dette er fritekst skrevet av saksbehandler.",
                                 sumFeilutbetaling = 595959L,
                                 feilutbetaltePerioder = mockFeilutbetalingerMedKunEnPeriode(),
                                 fristdato = LocalDate.of(2020, 4, 4),
                                 revurderingVedtakDato = LocalDate.of(2019, 12, 18),
                                 brevMetadata = metadata)


    @Test
    fun `lagVarselbrevFritekst skal generere varseltekst for flere perioder overgangsstønad`() {
        val metadata = metadata.copy(språkkode = Språkkode.NN)
        val varselbrevSamletInfo = varselbrevSamletInfo.copy(brevMetadata = metadata,
                                                             feilutbetaltePerioder = mockFeilutbetalingerMedFlerePerioder())
        val generertBrev = TekstformatererVarselbrev.lagVarselbrevFritekst(varselbrevSamletInfo)
        val fasit = les("/varselbrev/OS_flere_perioder.txt")
        assertThat(generertBrev).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagVarselbrevFritekst skal generere varseltekst for enkelt periode overgangsstønad`() {
        val varselbrevSamletInfo = varselbrevSamletInfo.copy(feilutbetaltePerioder = mockFeilutbetalingerMedKunEnPeriode())
        val generertBrev = TekstformatererVarselbrev.lagVarselbrevFritekst(varselbrevSamletInfo)
        val fasit = les("/varselbrev/OS_en_periode.txt")
        assertThat(generertBrev).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagVarselbrevFritekst skal generere varseltekst for enkelt periode barnetrygd`() {
        val metadata = metadata.copy(ytelsestype = Ytelsestype.BARNETRYGD)
        val varselbrevSamletInfo = varselbrevSamletInfo.copy(brevMetadata = metadata,
                                                             feilutbetaltePerioder = mockFeilutbetalingerMedKunEnPeriode())
        val generertBrev = TekstformatererVarselbrev.lagVarselbrevFritekst(varselbrevSamletInfo)
        val fasit = les("/varselbrev/BA_en_periode.txt")
        assertThat(generertBrev).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `mapTilVarselbrevDokument skal mappe verdier fra dtoer til komplett tilbakekrevingsvarsel`() {
        val varselbrevSamletInfo =
                varselbrevSamletInfo.copy(fristdato = LocalDate.of(2018, 5, 27),
                                          revurderingVedtakDato = LocalDate.of(2018, 5, 6))
        val varselbrev: VarselbrevDokument = TekstformatererVarselbrev.mapTilVarselbrevDokument(varselbrevSamletInfo)
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
    fun `mapTilVarselbrevDokument skal ikke sette tidligste og seneste dato når det foreligger flere perioder`() {
        val varselbrevSamletInfo = varselbrevSamletInfo.copy(feilutbetaltePerioder = mockFeilutbetalingerMedFlerePerioder())
        val varselbrev: VarselbrevDokument = TekstformatererVarselbrev.mapTilVarselbrevDokument(varselbrevSamletInfo)
        assertThat(varselbrev.datoerHvisSammenhengendePeriode).isNull()
    }

    @Test
    fun `lagVarselbrevOverskrift skal generere varselbrev overskrift`() {
        val overskrift = TekstformatererVarselbrev.lagVarselbrevOverskrift(metadata)
        val fasit = "NAV vurderer om du må betale tilbake overgangsstønad"
        assertThat(overskrift).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagVarselbrevOverskrift skal generere varselbrev overskrift nynorsk`() {
        val brevMetadata = metadata.copy(språkkode = Språkkode.NN)
        val overskrift = TekstformatererVarselbrev.lagVarselbrevOverskrift(brevMetadata)
        val fasit = "NAV vurderer om du må betale tilbake overgangsstønad"
        assertThat(overskrift).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagVarselbrevOverskrift skal generere korrigert varselbrev overskrift`() {
        val overskrift = TekstformatererVarselbrev.lagKorrigertVarselbrevOverskrift(metadata)
        val fasit = "Korrigert varsel om feilutbetalt overgangsstønad"
        assertThat(overskrift).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagVarselbrevOverskrift skal generere korrigert varselbrev overskrift nynorsk`() {
        val brevMetadata = metadata.copy(språkkode = Språkkode.NN)
        val overskrift = TekstformatererVarselbrev.lagKorrigertVarselbrevOverskrift(brevMetadata)
        val fasit = "Korrigert varsel om feilutbetalt overgangsstønad"
        assertThat(overskrift).isEqualToNormalizingNewlines(fasit)
    }

    @Test
    fun `lagVarselbrevFritekst skal generere varselbrev for verge`() {
        val metadata = metadata.copy(vergeNavn = "John Doe",
                                     finnesVerge = true,
                                     språkkode = Språkkode.NB)
        val varselbrevSamletInfo = varselbrevSamletInfo.copy(brevMetadata = metadata)
        val generertBrev = TekstformatererVarselbrev.lagVarselbrevFritekst(varselbrevSamletInfo)
        val fasit = les("/varselbrev/OS_en_periode.txt")
        val vergeTekst = les("/varselbrev/verge.txt")
        assertThat(generertBrev).isEqualToNormalizingNewlines("$fasit\n\n$vergeTekst")
    }

    private fun mockFeilutbetalingerMedFlerePerioder(): List<HbPeriode> {
        val periode1 = HbPeriode(LocalDate.of(2019, 3, 3),
                                 LocalDate.of(2020, 3, 3))
        val periode2 = HbPeriode(LocalDate.of(2022, 3, 3),
                                 LocalDate.of(2024, 3, 3))
        return listOf(periode1, periode2)
    }


    private fun mockFeilutbetalingerMedKunEnPeriode(): List<HbPeriode> {
        return listOf(HbPeriode(LocalDate.of(2019, 3, 3),
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

    private fun lagAdresseInfo(): Adresseinfo {
        return Adresseinfo("Test", "123456")
    }
}
