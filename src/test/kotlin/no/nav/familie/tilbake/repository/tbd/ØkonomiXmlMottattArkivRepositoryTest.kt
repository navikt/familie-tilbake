package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class ØkonomiXmlMottattArkivRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var økonomiXmlMottattArkivRepository: ØkonomiXmlMottattArkivRepository

    private val økonomiXmlMottattArkiv = Testdata.økonomiXmlMottattArkiv

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av ØkonomiXmlMottattArkiv til basen`() {
        økonomiXmlMottattArkivRepository.insert(økonomiXmlMottattArkiv)

        val lagretØkonomiXmlMottattArkiv = økonomiXmlMottattArkivRepository.findByIdOrThrow(økonomiXmlMottattArkiv.id)

        Assertions.assertThat(lagretØkonomiXmlMottattArkiv).isEqualToIgnoringGivenFields(økonomiXmlMottattArkiv, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av ØkonomiXmlMottattArkiv i basen`() {
        økonomiXmlMottattArkivRepository.insert(økonomiXmlMottattArkiv)
        val oppdatertØkonomiXmlMottattArkiv = økonomiXmlMottattArkiv.copy(melding = "bob")

        økonomiXmlMottattArkivRepository.update(oppdatertØkonomiXmlMottattArkiv)

        val lagretØkonomiXmlMottattArkiv = økonomiXmlMottattArkivRepository.findByIdOrThrow(økonomiXmlMottattArkiv.id)
        Assertions.assertThat(lagretØkonomiXmlMottattArkiv)
                .isEqualToIgnoringGivenFields(oppdatertØkonomiXmlMottattArkiv, "sporbar")
    }

}
