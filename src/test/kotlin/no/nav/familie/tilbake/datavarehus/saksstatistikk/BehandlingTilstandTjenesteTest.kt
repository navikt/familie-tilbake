package no.nav.familie.tilbake.datavarehus.saksstatistikk

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.BehandlingPåVentDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.OffsetDateTime


class BehandlingTilstandServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var behandlingService: BehandlingService

    private lateinit var service: BehandlingTilstandService

    private lateinit var behandling: Behandling

    @BeforeEach
    fun setup() {
        service = BehandlingTilstandService(behandlingRepository,
                                            behandlingsstegstilstandRepository,
                                            fagsakRepository,
                                            taskService)

        fagsakRepository.insert(Testdata.fagsak)
        behandling = behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun `hentBehandlingensTilstand skal utlede behandlingtilstand for nyopprettet behandling`() {

        val tilstand = service.hentBehandlingensTilstand(behandling.id)

        assertThat(tilstand.ytelsestype).isEqualTo(Ytelsestype.BARNETRYGD)
        assertThat(tilstand.saksnummer).isEqualTo(Testdata.fagsak.eksternFagsakId)
        assertThat(tilstand.behandlingUuid).isEqualTo(behandling.eksternBrukId)
        assertThat(tilstand.referertFagsaksbehandling).isEqualTo(behandling.aktivFagsystemsbehandling.eksternId)
        assertThat(tilstand.behandlingstype).isEqualTo(Behandlingstype.TILBAKEKREVING)
        assertThat(tilstand.behandlingsstatus).isEqualTo(Behandlingsstatus.OPPRETTET)
        assertThat(tilstand.behandlingsresultat).isEqualTo(Behandlingsresultatstype.IKKE_FASTSATT)
        assertThat(tilstand.venterPåBruker).isFalse()
        assertThat(tilstand.venterPåØkonomi).isFalse()
        assertThat(tilstand.erBehandlingManueltOpprettet).isFalse()
        assertThat(tilstand.funksjonellTid).isBetween(OffsetDateTime.now().minusMinutes(1), OffsetDateTime.now())
        assertThat(tilstand.tekniskTid).isNull()
        assertThat(tilstand.ansvarligBeslutter).isEqualTo(behandling.ansvarligBeslutter)
        assertThat(tilstand.ansvarligSaksbehandler).isEqualTo(behandling.ansvarligSaksbehandler)
        assertThat(tilstand.behandlendeEnhetsKode).isEqualTo(behandling.behandlendeEnhet)
    }

    @Test
    fun `hentBehandlingensTilstand skal utlede behandlingtilstand for fattet behandling`() {
        val behandlingsresultat = Behandlingsresultat(type = Behandlingsresultatstype.FULL_TILBAKEBETALING)
        val fattetBehandling = behandling.copy(behandlendeEnhet = "1234", behandlendeEnhetsNavn = "foo bar",
                                               ansvarligSaksbehandler = "Z111111",
                                               ansvarligBeslutter = "Z111112",
                                               resultater = setOf(behandlingsresultat))
        behandlingRepository.update(fattetBehandling)

        val tilstand = service.hentBehandlingensTilstand(behandling.id)

        assertThat(tilstand.ytelsestype).isEqualTo(Ytelsestype.BARNETRYGD)
        assertThat(tilstand.saksnummer).isEqualTo(Testdata.fagsak.eksternFagsakId)
        assertThat(tilstand.behandlingUuid).isEqualTo(behandling.eksternBrukId)
        assertThat(tilstand.referertFagsaksbehandling).isEqualTo(behandling.aktivFagsystemsbehandling.eksternId)
        assertThat(tilstand.behandlingstype).isEqualTo(behandling.type)
        assertThat(tilstand.behandlingsstatus).isEqualTo(behandling.status)
        assertThat(tilstand.behandlingsresultat).isEqualTo(behandlingsresultat.type)
        assertThat(tilstand.venterPåBruker).isFalse()
        assertThat(tilstand.venterPåØkonomi).isFalse()
        assertThat(tilstand.erBehandlingManueltOpprettet).isFalse()
        assertThat(tilstand.funksjonellTid).isBetween(OffsetDateTime.now().minusMinutes(1), OffsetDateTime.now())
        assertThat(tilstand.tekniskTid).isNull()
        assertThat(tilstand.ansvarligBeslutter).isEqualTo("Z111112")
        assertThat(tilstand.ansvarligSaksbehandler).isEqualTo("Z111111")
        assertThat(tilstand.behandlendeEnhetsKode).isEqualTo("1234")
    }

    @Test
    fun `hentBehandlingensTilstand skal utlede behandlingstilstand for behandling på vent`() {
        behandlingsstegstilstandRepository.insert(Testdata.behandlingsstegstilstand)
        behandlingService.settBehandlingPåVent(behandling.id,
                                               BehandlingPåVentDto(Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                                                                   LocalDate.now().plusDays(1)))
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)

        val tilstand = service.hentBehandlingensTilstand(behandling.id)

        assertThat(tilstand.ytelsestype).isEqualTo(Ytelsestype.BARNETRYGD)
        assertThat(tilstand.saksnummer).isEqualTo(Testdata.fagsak.eksternFagsakId)
        assertThat(tilstand.behandlingUuid).isEqualTo(behandling.eksternBrukId)
        assertThat(tilstand.referertFagsaksbehandling).isEqualTo(behandling.aktivFagsystemsbehandling.eksternId)
        assertThat(tilstand.behandlingstype).isEqualTo(behandling.type)
        assertThat(tilstand.behandlingsstatus).isEqualTo(behandling.status)
        assertThat(tilstand.behandlingsresultat).isEqualTo(Testdata.behandlingsresultat.type)
        assertThat(tilstand.venterPåBruker).isTrue()
        assertThat(tilstand.venterPåØkonomi).isFalse()
        assertThat(tilstand.funksjonellTid).isBetween(OffsetDateTime.now().minusMinutes(1), OffsetDateTime.now())
    }

}