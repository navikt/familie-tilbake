package no.nav.familie.tilbake.datavarehus.saksstatistikk

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.datavarehus.saksstatistikk.sakshendelse.Behandlingstilstand
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Properties
import java.util.UUID

@Service
class BehandlingTilstandService(private val behandlingRepository: BehandlingRepository,
                                private val behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository,
                                private val fagsakRepository: FagsakRepository,
                                private val taskService: TaskService) {


    @Transactional
    fun opprettSendingAvBehandlingensTilstand(behandlingId: UUID,
                                              tilstandFør: Behandlingsstegstilstand,
                                              tilstandEtter: Behandlingsstegsinfo) {
        val hendelsesbeskrivelse = "Endring fra ${tilstandFør.behandlingssteg}:${tilstandFør.behandlingsstegsstatus} " +
                                   "til ${tilstandEtter.behandlingssteg} : ${tilstandEtter.behandlingsstegstatus} " +
                                   "for behandling $behandlingId"

        val tilstand = hentBehandlingensTilstand(behandlingId)
        opprettProsessTask(behandlingId, tilstand, hendelsesbeskrivelse)
    }

    fun opprettSendingAvBehandlingensTilstand(behandlingId: UUID, tilstandFør: Behandlingsstegstilstand) {
        val hendelsesbeskrivelse = "Oppretter ${tilstandFør.behandlingssteg}:${tilstandFør.behandlingsstegsstatus} " +
                                   "for behandling $behandlingId"

        val tilstand = hentBehandlingensTilstand(behandlingId)
        opprettProsessTask(behandlingId, tilstand, hendelsesbeskrivelse)
    }

    fun opprettSendingAvBehandlingenHenlagt(behandlingId: UUID) {
        val hendelsesbeskrivelse = "Henlegger behandling $behandlingId"

        val tilstand = hentBehandlingensTilstand(behandlingId)
        opprettProsessTask(behandlingId, tilstand, hendelsesbeskrivelse)
    }

    fun opprettSendingAvNyttSteg(behandlingId: UUID, tilstandEtter: Behandlingsstegsinfo) {
        val hendelsesbeskrivelse = "Nytt steg ${tilstandEtter.behandlingssteg} : ${tilstandEtter.behandlingsstegstatus} " +
                                   "for behandling $behandlingId"

        val tilstand = hentBehandlingensTilstand(behandlingId)
        opprettProsessTask(behandlingId, tilstand, hendelsesbeskrivelse)
    }


    private fun opprettProsessTask(behandlingId: UUID, behandlingstilstand: Behandlingstilstand, hendelsesbeskrivelse: String) {
        val task = Task(SendSakshendelseTilDvhTask.TASK_TYPE,
                        behandlingId.toString(),
                        Properties().apply {
                            put("behandlingstilstand", objectMapper.writeValueAsString(behandlingstilstand))
                            put("beskrivelse", hendelsesbeskrivelse)
                        })
        taskService.save(task)
    }

    fun hentBehandlingensTilstand(behandlingId: UUID): Behandlingstilstand {
        val behandling: Behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val eksternBehandling = behandling.aktivFagsystemsbehandling.eksternId
        val behandlingsresultat = behandling.sisteResultat?.type ?: Behandlingsresultatstype.IKKE_FASTSATT
        val behandlingsstegstilstand = behandlingsstegstilstandRepository
                .findByBehandlingIdAndBehandlingsstegsstatusIn(behandlingId, Behandlingsstegstatus.aktiveStegStatuser)
        val venterPåBruker: Boolean = Venteårsak.venterPåBruker(behandlingsstegstilstand?.venteårsak)
        val venterPåØkonomi: Boolean = Venteårsak.venterPåØkonomi(behandlingsstegstilstand?.venteårsak)
        val behandlingsårsak = behandling.årsaker.firstOrNull()
        val forrigeBehandling = behandlingsårsak?.originalBehandlingId?.let { behandlingRepository.findByIdOrNull(it) }
        return Behandlingstilstand(ytelsestype = fagsak.ytelsestype,
                                   saksnummer = fagsak.eksternFagsakId,
                                   behandlingUuid = behandling.eksternBrukId,
                                   referertFagsaksbehandling = eksternBehandling,
                                   behandlingstype = behandling.type,
                                   behandlingsstatus = behandling.status,
                                   behandlingsresultat = behandlingsresultat,
                                   behandlendeEnhetsKode = behandling.behandlendeEnhet,
                                   ansvarligBeslutter = behandling.ansvarligBeslutter,
                                   ansvarligSaksbehandler = behandling.ansvarligSaksbehandler,
                                   erBehandlingManueltOpprettet = behandling.manueltOpprettet,
                                   funksjonellTid = OffsetDateTime.now(ZoneOffset.UTC),
                                   venterPåBruker = venterPåBruker,
                                   venterPåØkonomi = venterPåØkonomi,
                                   forrigeBehandling = forrigeBehandling?.let(Behandling::eksternBrukId),
                                   revurderingOpprettetÅrsak = behandlingsårsak?.type)
    }

}