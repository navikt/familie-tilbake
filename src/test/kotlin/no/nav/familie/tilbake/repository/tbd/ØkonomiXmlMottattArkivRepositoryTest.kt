package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattArkivRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

internal class ØkonomiXmlMottattArkivRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var økonomiXmlMottattArkivRepository: ØkonomiXmlMottattArkivRepository

    private val økonomiXmlMottattArkiv = Testdata.økonomiXmlMottattArkiv

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av ØkonomiXmlMottattArkiv til basen`() {
        økonomiXmlMottattArkivRepository.insert(økonomiXmlMottattArkiv)

        val lagretØkonomiXmlMottattArkiv = økonomiXmlMottattArkivRepository.findByIdOrThrow(økonomiXmlMottattArkiv.id)

        assertThat(lagretØkonomiXmlMottattArkiv).isEqualToIgnoringGivenFields(økonomiXmlMottattArkiv, "sporbar", "versjon")
        assertEquals(1, lagretØkonomiXmlMottattArkiv.versjon)
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av ØkonomiXmlMottattArkiv i basen`() {
        økonomiXmlMottattArkivRepository.insert(økonomiXmlMottattArkiv)
        var lagretØkonomiXmlMottattArkiv = økonomiXmlMottattArkivRepository.findByIdOrThrow(økonomiXmlMottattArkiv.id)
        val oppdatertØkonomiXmlMottattArkiv = lagretØkonomiXmlMottattArkiv.copy(melding = "bob")

        økonomiXmlMottattArkivRepository.update(oppdatertØkonomiXmlMottattArkiv)

        lagretØkonomiXmlMottattArkiv = økonomiXmlMottattArkivRepository.findByIdOrThrow(økonomiXmlMottattArkiv.id)
        assertThat(lagretØkonomiXmlMottattArkiv)
                .isEqualToIgnoringGivenFields(oppdatertØkonomiXmlMottattArkiv, "sporbar", "versjon")
        assertEquals(2, lagretØkonomiXmlMottattArkiv.versjon)
    }

}
