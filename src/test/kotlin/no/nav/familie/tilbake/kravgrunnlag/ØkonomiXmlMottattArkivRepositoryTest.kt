package no.nav.familie.tilbake.kravgrunnlag

import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottattArkiv
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class ØkonomiXmlMottattArkivRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var økonomiXmlMottattArkivRepository: ØkonomiXmlMottattArkivRepository

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av ØkonomiXmlMottattArkiv til basen`() {
        val økonomiXmlMottattArkiv = økonomiXmlMottattArkivRepository.insert(Testdata.lagØkonomiXmlMottattArkiv())

        val lagretØkonomiXmlMottattArkiv = økonomiXmlMottattArkivRepository.findByIdOrThrow(økonomiXmlMottattArkiv.id)

        lagretØkonomiXmlMottattArkiv.shouldBeEqualToIgnoringFields(
            økonomiXmlMottattArkiv,
            ØkonomiXmlMottattArkiv::sporbar,
            ØkonomiXmlMottattArkiv::versjon,
        )
        lagretØkonomiXmlMottattArkiv.versjon shouldBe 1
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av ØkonomiXmlMottattArkiv i basen`() {
        val økonomiXmlMottattArkiv = økonomiXmlMottattArkivRepository.insert(Testdata.lagØkonomiXmlMottattArkiv())
        var lagretØkonomiXmlMottattArkiv = økonomiXmlMottattArkivRepository.findByIdOrThrow(økonomiXmlMottattArkiv.id)
        val oppdatertØkonomiXmlMottattArkiv = lagretØkonomiXmlMottattArkiv.copy(melding = "bob")

        økonomiXmlMottattArkivRepository.update(oppdatertØkonomiXmlMottattArkiv)

        lagretØkonomiXmlMottattArkiv = økonomiXmlMottattArkivRepository.findByIdOrThrow(økonomiXmlMottattArkiv.id)
        lagretØkonomiXmlMottattArkiv.shouldBeEqualToIgnoringFields(
            oppdatertØkonomiXmlMottattArkiv,
            ØkonomiXmlMottattArkiv::sporbar,
            ØkonomiXmlMottattArkiv::versjon,
        )
        lagretØkonomiXmlMottattArkiv.versjon shouldBe 2
    }
}
