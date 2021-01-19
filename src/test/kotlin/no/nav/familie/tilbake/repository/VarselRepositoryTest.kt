package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class VarselRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var varselRepository: VarselRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository


    private val varsel = Testdata.varsel

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun insertPersistererEnForekomstAvVarselTilBasen() {
        varselRepository.insert(varsel)

        val lagretVarsel = varselRepository.findByIdOrThrow(varsel.id)

        Assertions.assertThat(lagretVarsel).isEqualToIgnoringGivenFields(varsel, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvVarselIBasen() {
        varselRepository.insert(varsel)
        val oppdatertVarsel = varsel.copy(varseltekst = "bob")

        varselRepository.update(oppdatertVarsel)

        val lagretVarsel = varselRepository.findByIdOrThrow(varsel.id)
        Assertions.assertThat(lagretVarsel).isEqualToIgnoringGivenFields(oppdatertVarsel, "sporbar")
    }

}