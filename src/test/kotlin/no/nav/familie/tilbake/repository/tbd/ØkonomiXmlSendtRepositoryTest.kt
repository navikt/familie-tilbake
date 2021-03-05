package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

internal class ØkonomiXmlSendtRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var økonomiXmlSendtRepository: ØkonomiXmlSendtRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val økonomiXmlSendt = Testdata.økonomiXmlSendt

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av ØkonomiXmlSendt til basen`() {
        økonomiXmlSendtRepository.insert(økonomiXmlSendt)

        val lagretØkonomiXmlSendt = økonomiXmlSendtRepository.findByIdOrThrow(økonomiXmlSendt.id)

        assertThat(lagretØkonomiXmlSendt).isEqualToIgnoringGivenFields(økonomiXmlSendt, "sporbar", "versjon")
        assertEquals(1, lagretØkonomiXmlSendt.versjon)
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av ØkonomiXmlSendt i basen`() {
        økonomiXmlSendtRepository.insert(økonomiXmlSendt)
        var lagretØkonomiXmlSendt = økonomiXmlSendtRepository.findByIdOrThrow(økonomiXmlSendt.id)
        val oppdatertØkonomiXmlSendt = lagretØkonomiXmlSendt.copy(melding = "bob")

        økonomiXmlSendtRepository.update(oppdatertØkonomiXmlSendt)

        lagretØkonomiXmlSendt = økonomiXmlSendtRepository.findByIdOrThrow(økonomiXmlSendt.id)
        assertThat(lagretØkonomiXmlSendt).isEqualToIgnoringGivenFields(oppdatertØkonomiXmlSendt, "sporbar", "versjon")
        assertEquals(2, lagretØkonomiXmlSendt.versjon)
    }

}
