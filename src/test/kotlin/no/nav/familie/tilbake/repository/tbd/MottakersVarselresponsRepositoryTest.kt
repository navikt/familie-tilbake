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

        assertThat(lagretMottakersVarselrespons).usingRecursiveComparison()
                .ignoringFields("sporbar", "versjon")
                .isEqualTo(mottakersVarselrespons)
        assertEquals(1, lagretMottakersVarselrespons.versjon)
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av MottakersVarselrespons i basen`() {
        mottakersVarselresponsRepository.insert(mottakersVarselrespons)
        var lagretMottakersVarselrespons = mottakersVarselresponsRepository.findByIdOrThrow(mottakersVarselrespons.id)
        val oppdatertMottakersVarselrespons = lagretMottakersVarselrespons.copy(kilde = "bob")

        mottakersVarselresponsRepository.update(oppdatertMottakersVarselrespons)

        lagretMottakersVarselrespons = mottakersVarselresponsRepository.findByIdOrThrow(mottakersVarselrespons.id)
        assertThat(lagretMottakersVarselrespons)
                .usingRecursiveComparison().ignoringFields("sporbar", "versjon").isEqualTo(oppdatertMottakersVarselrespons)
        assertEquals(2, lagretMottakersVarselrespons.versjon)
    }

}
