package no.nav.familie.tilbake.behandlingskontroll

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

internal class BehandlingsstegstilstandRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val behandlingsstegstilstand = Testdata.behandlingsstegstilstand

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Behandlingsstegstilstand til basen`() {
        behandlingsstegstilstandRepository.insert(behandlingsstegstilstand)

        val lagretBehandlingsstegstilstand = behandlingsstegstilstandRepository.findByIdOrThrow(behandlingsstegstilstand.id)

        assertThat(lagretBehandlingsstegstilstand).usingRecursiveComparison()
                .ignoringFields("sporbar", "versjon")
                .isEqualTo(behandlingsstegstilstand)
        assertEquals(1, lagretBehandlingsstegstilstand.versjon)
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Behandlingsstegstilstand i basen`() {
        behandlingsstegstilstandRepository.insert(behandlingsstegstilstand)
        var lagretBehandlingsstegstilstand = behandlingsstegstilstandRepository.findByIdOrThrow(behandlingsstegstilstand.id)
        val oppdatertBehandlingsstegstilstand =
                lagretBehandlingsstegstilstand.copy(behandlingsstegsstatus = Behandlingsstegstatus.KLAR)

        behandlingsstegstilstandRepository.update(oppdatertBehandlingsstegstilstand)

        lagretBehandlingsstegstilstand = behandlingsstegstilstandRepository.findByIdOrThrow(behandlingsstegstilstand.id)
        assertThat(lagretBehandlingsstegstilstand)
                .usingRecursiveComparison().ignoringFields("sporbar", "versjon").isEqualTo(oppdatertBehandlingsstegstilstand)
        assertEquals(2, lagretBehandlingsstegstilstand.versjon)
    }

}
