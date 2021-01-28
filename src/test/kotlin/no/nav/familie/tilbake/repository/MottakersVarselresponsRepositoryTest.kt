package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired


internal class MottakersVarselresponsRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var mottakersVarselresponsRepository: MottakersVarselresponsRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val mottakersVarselrespons = Testdata.mottakersVarselrespons

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av MottakersVarselrespons til basen`() {
        mottakersVarselresponsRepository.insert(mottakersVarselrespons)

        val lagretMottakersVarselrespons = mottakersVarselresponsRepository.findByIdOrThrow(mottakersVarselrespons.id)

        Assertions.assertThat(lagretMottakersVarselrespons).isEqualToIgnoringGivenFields(mottakersVarselrespons, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av MottakersVarselrespons i basen`() {
        mottakersVarselresponsRepository.insert(mottakersVarselrespons)
        val oppdatertMottakersVarselrespons = mottakersVarselrespons.copy(kilde = "bob")

        mottakersVarselresponsRepository.update(oppdatertMottakersVarselrespons)

        val lagretMottakersVarselrespons = mottakersVarselresponsRepository.findByIdOrThrow(mottakersVarselrespons.id)
        Assertions.assertThat(lagretMottakersVarselrespons)
                .isEqualToIgnoringGivenFields(oppdatertMottakersVarselrespons, "sporbar")
    }

}