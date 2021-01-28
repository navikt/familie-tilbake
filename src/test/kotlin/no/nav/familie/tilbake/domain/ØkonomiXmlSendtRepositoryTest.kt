package no.nav.familie.tilbake.domain

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.repository.tbd.ØkonomiXmlSendtRepository
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow

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

        Assertions.assertThat(lagretØkonomiXmlSendt).isEqualToIgnoringGivenFields(økonomiXmlSendt, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av ØkonomiXmlSendt i basen`() {
        økonomiXmlSendtRepository.insert(økonomiXmlSendt)
        val oppdatertØkonomiXmlSendt = økonomiXmlSendt.copy(melding = "bob")

        økonomiXmlSendtRepository.update(oppdatertØkonomiXmlSendt)

        val lagretØkonomiXmlSendt = økonomiXmlSendtRepository.findByIdOrThrow(økonomiXmlSendt.id)
        Assertions.assertThat(lagretØkonomiXmlSendt).isEqualToIgnoringGivenFields(oppdatertØkonomiXmlSendt, "sporbar")
    }

}