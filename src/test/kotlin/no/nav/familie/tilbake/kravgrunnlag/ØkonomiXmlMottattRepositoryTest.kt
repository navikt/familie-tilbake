package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigInteger
import java.time.LocalDate

internal class ØkonomiXmlMottattRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var økonomiXmlMottattRepository: ØkonomiXmlMottattRepository

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av ØkonomiXmlMottatt til basen`() {
        // Act
        val økonomiXmlMottatt = økonomiXmlMottattRepository.insert(Testdata.getøkonomiXmlMottatt())

        // Assert
        val lagretØkonomiXmlMottatt = økonomiXmlMottattRepository.findByIdOrThrow(økonomiXmlMottatt.id)
        assertThat(lagretØkonomiXmlMottatt.versjon).isEqualTo(1)
        assertThat(lagretØkonomiXmlMottatt.melding).isEqualTo("testverdi")
        assertThat(lagretØkonomiXmlMottatt.kravstatuskode).isEqualTo(Kravstatuskode.NYTT)
        assertThat(lagretØkonomiXmlMottatt.eksternFagsakId).isEqualTo(økonomiXmlMottatt.eksternFagsakId)
        assertThat(lagretØkonomiXmlMottatt.ytelsestype).isEqualTo(Ytelsestype.BARNETRYGD)
        assertThat(lagretØkonomiXmlMottatt.referanse).isEqualTo("2026-04-20-11.22.33.123456")
        assertThat(lagretØkonomiXmlMottatt.eksternKravgrunnlagId).isEqualTo(økonomiXmlMottatt.eksternKravgrunnlagId)
        assertThat(lagretØkonomiXmlMottatt.vedtakId).isEqualTo(BigInteger.ZERO)
        assertThat(lagretØkonomiXmlMottatt.kontrollfelt).isEqualTo("2023-07-12-22.53.47.806186")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av ØkonomiXmlMottatt i basen`() {
        // Arrange
        val økonomiXmlMottatt = økonomiXmlMottattRepository.insert(Testdata.getøkonomiXmlMottatt())

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
        assertThat(lagretØkonomiXmlMottatt.referanse).isEqualTo("2026-04-20-11.22.33.123456")
        assertThat(lagretØkonomiXmlMottatt.eksternKravgrunnlagId).isEqualTo(økonomiXmlMottatt.eksternKravgrunnlagId)
        assertThat(lagretØkonomiXmlMottatt.vedtakId).isEqualTo(BigInteger.ZERO)
        assertThat(lagretØkonomiXmlMottatt.kontrollfelt).isEqualTo("2023-07-12-22.53.47.806186")
    }

    @Test
    fun `hentFrakobletKravgrunnlag skal hente de riktige verdiene fra repoet`() {
        // Arrange
        val økonomiXmlMottatt = økonomiXmlMottattRepository.insert(Testdata.getøkonomiXmlMottatt())

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
        assertThat(frakobletKravgrunnlag).anySatisfy {
            assertThat(it.versjon).isEqualTo(1)
            assertThat(it.melding).isEqualTo("testverdi")
            assertThat(it.kravstatuskode).isEqualTo(Kravstatuskode.NYTT)
            assertThat(it.eksternFagsakId).isEqualTo(økonomiXmlMottatt.eksternFagsakId)
            assertThat(it.ytelsestype).isEqualTo(Ytelsestype.BARNETRYGD)
            assertThat(it.referanse).isEqualTo("2026-04-20-11.22.33.123456")
            assertThat(it.eksternKravgrunnlagId).isEqualTo(økonomiXmlMottatt.eksternKravgrunnlagId)
            assertThat(it.vedtakId).isEqualTo(BigInteger.ZERO)
            assertThat(it.kontrollfelt).isEqualTo("2023-07-12-22.53.47.806186")
        }
    }
}
