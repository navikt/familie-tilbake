package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class FaktaFeilutbetalingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var grupperingFaktaFeilutbetalingRepository: GrupperingFaktaFeilutbetalingRepository

    private val fagsak = Testdata.fagsak
    private val behandling = Testdata.behandling
    private val grupperingFaktaFeilutbetaling = Testdata.grupperingFaktaFeilutbetaling
    private val faktaFeilutbetaling = Testdata.faktaFeilutbetaling

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av FaktaFeilutbetaling til basen`() {
        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling)

        val lagretFaktaFeilutbetaling = faktaFeilutbetalingRepository.findByIdOrThrow(faktaFeilutbetaling.id)

        assertThat(lagretFaktaFeilutbetaling).isEqualToIgnoringGivenFields(faktaFeilutbetaling, "sporbar")
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av FaktaFeilutbetaling i basen`() {
        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling)
        val oppdatertFaktaFeilutbetaling = faktaFeilutbetaling.copy(begrunnelse = "bob")

        faktaFeilutbetalingRepository.update(oppdatertFaktaFeilutbetaling)

        val lagretFaktaFeilutbetaling = faktaFeilutbetalingRepository.findByIdOrThrow(faktaFeilutbetaling.id)
        assertThat(lagretFaktaFeilutbetaling).isEqualToIgnoringGivenFields(oppdatertFaktaFeilutbetaling, "sporbar")
    }

    @Test
    fun `findByBehandlingId returnerer resultat når det finnes en forekomst`() {
        faktaFeilutbetalingRepository.insert(faktaFeilutbetaling)
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)
        grupperingFaktaFeilutbetalingRepository.insert(grupperingFaktaFeilutbetaling)

        val findByBehandlingId = faktaFeilutbetalingRepository.findByBehandlingId(behandling.id)

        assertThat(findByBehandlingId).isEqualToIgnoringGivenFields(faktaFeilutbetaling, "sporbar")
    }

}
