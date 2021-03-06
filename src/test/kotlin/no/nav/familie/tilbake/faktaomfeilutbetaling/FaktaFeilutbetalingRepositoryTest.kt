package no.nav.familie.tilbake.faktaomfeilutbetaling

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

internal class FaktaFeilutbetalingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    private val fagsak = Testdata.fagsak
    private val behandling = Testdata.behandling
    private val faktaFeilutbetaling = Testdata.faktaFeilutbetaling

    @BeforeEach
    fun init() {
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av FaktaFeilutbetaling til basen`() {
        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling)

        val lagretFaktaFeilutbetaling = faktaFeilutbetalingRepository.findByIdOrThrow(faktaFeilutbetaling.id)

        assertThat(lagretFaktaFeilutbetaling).usingRecursiveComparison()
                .ignoringFields("sporbar", "versjon")
                .isEqualTo(faktaFeilutbetaling)
        assertEquals(1, lagretFaktaFeilutbetaling.versjon)
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av FaktaFeilutbetaling i basen`() {
        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling)
        var lagretFaktaFeilutbetaling = faktaFeilutbetalingRepository.findByIdOrThrow(faktaFeilutbetaling.id)
        val oppdatertFaktaFeilutbetaling = lagretFaktaFeilutbetaling.copy(begrunnelse = "bob")

        faktaFeilutbetalingRepository.update(oppdatertFaktaFeilutbetaling)

        lagretFaktaFeilutbetaling = faktaFeilutbetalingRepository.findByIdOrThrow(faktaFeilutbetaling.id)
        assertThat(lagretFaktaFeilutbetaling).usingRecursiveComparison()
                .ignoringFields("sporbar", "versjon")
                .isEqualTo(oppdatertFaktaFeilutbetaling)
        assertEquals(2, lagretFaktaFeilutbetaling.versjon)
    }

    @Test
    fun `findByBehandlingIdAndAktivIsTrue returnerer resultat når det finnes en forekomst`() {
        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling)

        val findByBehandlingId = faktaFeilutbetalingRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)

        assertThat(findByBehandlingId).usingRecursiveComparison()
                .ignoringFields("sporbar", "versjon")
                .isEqualTo(faktaFeilutbetaling)
    }

}
