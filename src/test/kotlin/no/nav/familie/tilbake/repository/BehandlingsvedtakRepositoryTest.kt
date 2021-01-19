package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class BehandlingsvedtakRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingsvedtakRepository: BehandlingsvedtakRepository

    @Autowired
    private lateinit var behandlingsresultatRepository: BehandlingsresultatRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val behandlingsvedtak = Testdata.behandlingsvedtak

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandlingRepository.insert(Testdata.behandling)
        behandlingsresultatRepository.insert(Testdata.behandlingsresultat)
    }

    @Test
    fun insertPersistererEnForekomstAvBehandlingsvedtakTilBasen() {
        behandlingsvedtakRepository.insert(behandlingsvedtak)

        val lagretBehandlingsvedtak = behandlingsvedtakRepository.findByIdOrThrow(behandlingsvedtak.id)

        Assertions.assertThat(lagretBehandlingsvedtak).isEqualToIgnoringGivenFields(behandlingsvedtak, "sporbar")
    }

    @Test
    fun updateOppdatererEnForekomstAvBehandlingsvedtakIBasen() {
        behandlingsvedtakRepository.insert(behandlingsvedtak)
        val oppdatertBehandlingsvedtak = behandlingsvedtak.copy(ansvarligSaksbehandler = "bob")

        behandlingsvedtakRepository.update(oppdatertBehandlingsvedtak)

        val lagretBehandlingsvedtak = behandlingsvedtakRepository.findByIdOrThrow(behandlingsvedtak.id)
        Assertions.assertThat(lagretBehandlingsvedtak).isEqualToIgnoringGivenFields(oppdatertBehandlingsvedtak, "sporbar")
    }

}