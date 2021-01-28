package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class ØkonomiXmlMottattRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var økonomiXmlMottattRepository: ØkonomiXmlMottattRepository

    private val økonomiXmlMottatt = Testdata.økonomiXmlMottatt

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av ØkonomiXmlMottatt til basen`() {
        økonomiXmlMottattRepository.insert(økonomiXmlMottatt)

        val lagretØkonomiXmlMottatt = økonomiXmlMottattRepository.findByIdOrThrow(økonomiXmlMottatt.id)

        Assertions.assertThat(lagretØkonomiXmlMottatt).isEqualToIgnoringGivenFields(økonomiXmlMottatt, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av ØkonomiXmlMottatt i basen`() {
        økonomiXmlMottattRepository.insert(økonomiXmlMottatt)
        val oppdatertØkonomiXmlMottatt = økonomiXmlMottatt.copy(eksternFagsakId = "bob")

        økonomiXmlMottattRepository.update(oppdatertØkonomiXmlMottatt)

        val lagretØkonomiXmlMottatt = økonomiXmlMottattRepository.findByIdOrThrow(økonomiXmlMottatt.id)
        Assertions.assertThat(lagretØkonomiXmlMottatt).isEqualToIgnoringGivenFields(oppdatertØkonomiXmlMottatt, "sporbar")
    }

}
