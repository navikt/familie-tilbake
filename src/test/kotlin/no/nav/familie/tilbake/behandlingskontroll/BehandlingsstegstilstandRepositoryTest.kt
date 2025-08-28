package no.nav.familie.tilbake.behandlingskontroll

import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BehandlingsstegstilstandRepositoryTest : OppslagSpringRunnerTest() {
    override val tømDBEtterHverTest = false

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private lateinit var behandlingsstegstilstand: Behandlingsstegstilstand

    @BeforeEach
    fun init() {
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        val behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsakId = fagsak.id))
        behandlingsstegstilstand = Testdata.lagBehandlingsstegstilstand(behandling.id)
    }

    @Test
    fun `insert med gyldige verdier skal persistere en forekomst av Behandlingsstegstilstand til basen`() {
        behandlingsstegstilstandRepository.insert(behandlingsstegstilstand)

        val lagretBehandlingsstegstilstand = behandlingsstegstilstandRepository.findByIdOrThrow(behandlingsstegstilstand.id)

        lagretBehandlingsstegstilstand.shouldBeEqualToIgnoringFields(
            behandlingsstegstilstand,
            Behandlingsstegstilstand::sporbar,
            Behandlingsstegstilstand::versjon,
        )
        lagretBehandlingsstegstilstand.versjon shouldBe 1
    }

    @Test
    fun `update med gyldige verdier skal oppdatere en forekomst av Behandlingsstegstilstand i basen`() {
        behandlingsstegstilstandRepository.insert(behandlingsstegstilstand)
        var lagretBehandlingsstegstilstand = behandlingsstegstilstandRepository.findByIdOrThrow(behandlingsstegstilstand.id)
        val oppdatertBehandlingsstegstilstand =
            lagretBehandlingsstegstilstand.copy(behandlingsstegsstatus = Behandlingsstegstatus.KLAR)

        behandlingsstegstilstandRepository.update(oppdatertBehandlingsstegstilstand)

        lagretBehandlingsstegstilstand = behandlingsstegstilstandRepository.findByIdOrThrow(behandlingsstegstilstand.id)
        lagretBehandlingsstegstilstand.shouldBeEqualToIgnoringFields(
            oppdatertBehandlingsstegstilstand,
            Behandlingsstegstilstand::sporbar,
            Behandlingsstegstilstand::versjon,
        )
        lagretBehandlingsstegstilstand.versjon shouldBe 2
    }
}
