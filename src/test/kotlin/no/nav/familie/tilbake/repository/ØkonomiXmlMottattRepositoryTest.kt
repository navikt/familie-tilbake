package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class ØkonomiXmlMottattRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var økonomiXmlMottattRepository: ØkonomiXmlMottattRepository

    private val økonomiXmlMottatt = Testdata.økonomiXmlMottatt

    @Test
    fun insertPersistererEnForekomstAvØkonomiXmlMottattTilBasen() {
        økonomiXmlMottattRepository.insert(økonomiXmlMottatt)

        val lagretØkonomiXmlMottatt = økonomiXmlMottattRepository.findByIdOrThrow(økonomiXmlMottatt.id)

        Assertions.assertThat(lagretØkonomiXmlMottatt).isEqualToIgnoringGivenFields(økonomiXmlMottatt, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvØkonomiXmlMottattIBasen() {
        økonomiXmlMottattRepository.insert(økonomiXmlMottatt)
        val oppdatertØkonomiXmlMottatt = økonomiXmlMottatt.copy(eksternFagsakId = "bob")

        økonomiXmlMottattRepository.update(oppdatertØkonomiXmlMottatt)

        val lagretØkonomiXmlMottatt = økonomiXmlMottattRepository.findByIdOrThrow(økonomiXmlMottatt.id)
        Assertions.assertThat(lagretØkonomiXmlMottatt).isEqualToIgnoringGivenFields(oppdatertØkonomiXmlMottatt, "sporbar")
    }

}