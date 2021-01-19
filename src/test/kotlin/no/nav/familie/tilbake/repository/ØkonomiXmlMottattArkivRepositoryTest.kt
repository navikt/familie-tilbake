package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class ØkonomiXmlMottattArkivRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var økonomiXmlMottattArkivRepository: ØkonomiXmlMottattArkivRepository

    private val økonomiXmlMottattArkiv = Testdata.økonomiXmlMottattArkiv

    @Test
    fun insertPersistererEnForekomstAvØkonomiXmlMottattArkivTilBasen() {
        økonomiXmlMottattArkivRepository.insert(økonomiXmlMottattArkiv)

        val lagretØkonomiXmlMottattArkiv = økonomiXmlMottattArkivRepository.findByIdOrThrow(økonomiXmlMottattArkiv.id)

        Assertions.assertThat(lagretØkonomiXmlMottattArkiv).isEqualToIgnoringGivenFields(økonomiXmlMottattArkiv, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvØkonomiXmlMottattArkivIBasen() {
        økonomiXmlMottattArkivRepository.insert(økonomiXmlMottattArkiv)
        val oppdatertØkonomiXmlMottattArkiv = økonomiXmlMottattArkiv.copy(melding = "bob")

        økonomiXmlMottattArkivRepository.update(oppdatertØkonomiXmlMottattArkiv)

        val lagretØkonomiXmlMottattArkiv = økonomiXmlMottattArkivRepository.findByIdOrThrow(økonomiXmlMottattArkiv.id)
        Assertions.assertThat(lagretØkonomiXmlMottattArkiv)
                .isEqualToIgnoringGivenFields(oppdatertØkonomiXmlMottattArkiv, "sporbar")
    }

}