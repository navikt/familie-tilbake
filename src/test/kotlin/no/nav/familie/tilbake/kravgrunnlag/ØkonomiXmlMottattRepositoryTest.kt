package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kontrakter.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigInteger
import java.time.LocalDate

internal class ØkonomiXmlMottattRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var økonomiXmlMottattRepository: ØkonomiXmlMottattRepository

    private val økonomiXmlMottatt = Testdata.økonomiXmlMottatt

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av ØkonomiXmlMottatt til basen`() {
        // Act
        økonomiXmlMottattRepository.insert(økonomiXmlMottatt)

        // Assert
        val lagretØkonomiXmlMottatt = økonomiXmlMottattRepository.findByIdOrThrow(økonomiXmlMottatt.id)
        assertThat(lagretØkonomiXmlMottatt.versjon).isEqualTo(1)
        assertThat(lagretØkonomiXmlMottatt.melding).isEqualTo("testverdi")
        assertThat(lagretØkonomiXmlMottatt.kravstatuskode).isEqualTo(Kravstatuskode.NYTT)
        assertThat(lagretØkonomiXmlMottatt.eksternFagsakId).isEqualTo("testverdi")
        assertThat(lagretØkonomiXmlMottatt.ytelsestype).isEqualTo(Ytelsestype.BARNETRYGD)
        assertThat(lagretØkonomiXmlMottatt.referanse).isEqualTo("testverdi")
        assertThat(lagretØkonomiXmlMottatt.eksternKravgrunnlagId).isEqualTo(BigInteger.ZERO)
        assertThat(lagretØkonomiXmlMottatt.vedtakId).isEqualTo(BigInteger.ZERO)
        assertThat(lagretØkonomiXmlMottatt.kontrollfelt).isEqualTo("2023-07-12-22.53.47.806186")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av ØkonomiXmlMottatt i basen`() {
        // Arrange
        økonomiXmlMottattRepository.insert(økonomiXmlMottatt)

        var lagretØkonomiXmlMottatt = økonomiXmlMottattRepository.findByIdOrThrow(økonomiXmlMottatt.id)
        val oppdatertØkonomiXmlMottatt = lagretØkonomiXmlMottatt.copy(eksternFagsakId = "bob")

        // Act
        økonomiXmlMottattRepository.update(oppdatertØkonomiXmlMottatt)

        // Assert
        lagretØkonomiXmlMottatt = økonomiXmlMottattRepository.findByIdOrThrow(økonomiXmlMottatt.id)
        assertThat(lagretØkonomiXmlMottatt.versjon).isEqualTo(2)
        assertThat(lagretØkonomiXmlMottatt.melding).isEqualTo("testverdi")
        assertThat(lagretØkonomiXmlMottatt.kravstatuskode).isEqualTo(Kravstatuskode.NYTT)
        assertThat(lagretØkonomiXmlMottatt.eksternFagsakId).isEqualTo("bob")
        assertThat(lagretØkonomiXmlMottatt.ytelsestype).isEqualTo(Ytelsestype.BARNETRYGD)
        assertThat(lagretØkonomiXmlMottatt.referanse).isEqualTo("testverdi")
        assertThat(lagretØkonomiXmlMottatt.eksternKravgrunnlagId).isEqualTo(BigInteger.ZERO)
        assertThat(lagretØkonomiXmlMottatt.vedtakId).isEqualTo(BigInteger.ZERO)
        assertThat(lagretØkonomiXmlMottatt.kontrollfelt).isEqualTo("2023-07-12-22.53.47.806186")
    }

    @Test
    fun `hentFrakobletKravgrunnlag skal hente de riktige verdiene fra repoet`() {
        // Arrange
        økonomiXmlMottattRepository.insert(økonomiXmlMottatt)

        val bestemtDato = LocalDate.now().plusWeeks(2)

        // Act
        val frakobletKravgrunnlag =
            økonomiXmlMottattRepository.hentFrakobletKravgrunnlag(
                barnetrygdBestemtDato = bestemtDato,
                barnetilsynBestemtDato = bestemtDato,
                overgangsstonadbestemtdato = bestemtDato,
                skolePengerBestemtDato = bestemtDato,
                kontantstottebestemtdato = bestemtDato,
            )

        // Assert
        assertThat(frakobletKravgrunnlag).hasSize(1)
        assertThat(frakobletKravgrunnlag).anySatisfy {
            assertThat(it.versjon).isEqualTo(1)
            assertThat(it.melding).isEqualTo("testverdi")
            assertThat(it.kravstatuskode).isEqualTo(Kravstatuskode.NYTT)
            assertThat(it.eksternFagsakId).isEqualTo("testverdi")
            assertThat(it.ytelsestype).isEqualTo(Ytelsestype.BARNETRYGD)
            assertThat(it.referanse).isEqualTo("testverdi")
            assertThat(it.eksternKravgrunnlagId).isEqualTo(BigInteger.ZERO)
            assertThat(it.vedtakId).isEqualTo(BigInteger.ZERO)
            assertThat(it.kontrollfelt).isEqualTo("2023-07-12-22.53.47.806186")
        }
    }
}
