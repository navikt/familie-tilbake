package no.nav.familie.tilbake.datavarehus.saksstatistikk

import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.datavarehus.saksstatistikk.sakshendelse.Behandlingstilstand
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.FagsystemUtil
import no.nav.tilbakekreving.kontrakter.Periode
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsresultatstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Properties
import java.util.UUID

@Service
@Transactional
class BehandlingTilstandService(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository,
    private val fagsakRepository: FagsakRepository,
    private val taskService: TracableTaskService,
    private val faktaFeilutbetalingService: FaktaFeilutbetalingService,
    private val logService: LogService,
) {
    fun opprettSendingAvBehandlingensTilstand(
        behandlingId: UUID,
        info: Behandlingsstegsinfo,
        logContext: SecureLog.Context,
    ) {
        val hendelsesbeskrivelse =
            "Ny behandlingsstegstilstand " +
                "${info.behandlingssteg}:${info.behandlingsstegstatus} " +
                "for behandling $behandlingId"

        val tilstand = hentBehandlingensTilstand(behandlingId)
        opprettProsessTask(behandlingId, tilstand, hendelsesbeskrivelse, logContext)
    }

    fun opprettSendingAvBehandlingenHenlagt(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        val hendelsesbeskrivelse = "Henlegger behandling $behandlingId"

        val tilstand = hentBehandlingensTilstand(behandlingId)
        opprettProsessTask(behandlingId, tilstand, hendelsesbeskrivelse, logContext)
    }

    fun opprettSendingAvBehandlingenManuelt(behandlingId: UUID) {
        val logContext = logService.contextFraBehandling(behandlingId)
        val hendelsesbeskrivelse = "Sender siste tilstand manuellt på behandling $behandlingId"

        val tilstand = hentBehandlingensTilstand(behandlingId)
        opprettProsessTask(behandlingId, tilstand, hendelsesbeskrivelse, logContext)
    }

    private fun opprettProsessTask(
        behandlingId: UUID,
        behandlingstilstand: Behandlingstilstand,
        hendelsesbeskrivelse: String,
        logContext: SecureLog.Context,
    ) {
        val task =
            Task(
                SendSakshendelseTilDvhTask.TASK_TYPE,
                behandlingId.toString(),
                Properties().apply {
                    setProperty("behandlingstilstand", objectMapper.writeValueAsString(behandlingstilstand))
                    setProperty("beskrivelse", hendelsesbeskrivelse)
                    setProperty(
                        PropertyName.FAGSYSTEM,
                        FagsystemUtil.hentFagsystemFraYtelsestype(behandlingstilstand.ytelsestype).name,
                    )
                },
            )
        taskService.save(task, logContext)
    }

    fun hentBehandlingensTilstand(behandlingId: UUID): Behandlingstilstand {
        val behandling: Behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, behandling.id.toString())
        val eksternBehandling = behandling.aktivFagsystemsbehandling.eksternId
        val behandlingsresultat = behandling.sisteResultat?.type ?: Behandlingsresultatstype.IKKE_FASTSATT
        val behandlingsstegstilstand =
            behandlingsstegstilstandRepository
                .findByBehandlingIdAndBehandlingsstegsstatusIn(behandlingId, Behandlingsstegstatus.aktiveStegStatuser)
        val venterPåBruker: Boolean = Venteårsak.venterPåBruker(behandlingsstegstilstand?.venteårsak)
        val venterPåØkonomi: Boolean = Venteårsak.venterPåØkonomi(behandlingsstegstilstand?.venteårsak)
        val behandlingsårsak = behandling.årsaker.firstOrNull()
        val forrigeBehandling = behandlingsårsak?.originalBehandlingId?.let { behandlingRepository.findByIdOrNull(it) }

        var totalFeilutbetaltPeriode: Periode? = null
        var totalFeilutbetaltBeløp: BigDecimal? = null
        val erBehandlingsstegEtterGrunnlagSteg =
            behandlingsstegstilstand?.behandlingssteg?.sekvens?.let { it > Behandlingssteg.GRUNNLAG.sekvens } ?: false
        val erBehandlingHenlagt = behandling.sisteResultat?.erBehandlingHenlagt() ?: false
        if (erBehandlingsstegEtterGrunnlagSteg) {
            val fakta = faktaFeilutbetalingService.hentFaktaomfeilutbetaling(behandlingId)
            totalFeilutbetaltPeriode = Periode(fakta.totalFeilutbetaltPeriode.fom, fakta.totalFeilutbetaltPeriode.tom)
            totalFeilutbetaltBeløp = fakta.totaltFeilutbetaltBeløp
        } else if (behandlingsstegstilstand?.behandlingssteg == Behandlingssteg.VARSEL && !erBehandlingHenlagt) {
            val varsel =
                behandling.aktivtVarsel ?: throw Feil(
                    message = "Behandling $behandlingId venter på varselssteg uten varsel data",
                    logContext = logContext,
                )
            val førsteDagIVarselsperiode = varsel.perioder.minOf { it.fom }
            val sisteDagIVarselsperiode = varsel.perioder.maxOf { it.tom }

            totalFeilutbetaltBeløp = varsel.varselbeløp.toBigDecimal()
            totalFeilutbetaltPeriode = Periode(førsteDagIVarselsperiode, sisteDagIVarselsperiode)
        }

        return Behandlingstilstand(
            ytelsestype = fagsak.ytelsestype.tilDTO(),
            fagsystem = fagsak.fagsystem.tilDTO(),
            saksnummer = fagsak.eksternFagsakId,
            behandlingUuid = behandling.eksternBrukId,
            referertFagsaksbehandling = eksternBehandling,
            behandlingstype = behandling.type,
            behandlingsstatus = behandling.status,
            behandlingsresultat = behandlingsresultat,
            ansvarligEnhet = behandling.behandlendeEnhet,
            ansvarligBeslutter = behandling.ansvarligBeslutter,
            ansvarligSaksbehandler = behandling.ansvarligSaksbehandler,
            behandlingErManueltOpprettet = behandling.manueltOpprettet,
            funksjoneltTidspunkt = OffsetDateTime.now(ZoneOffset.UTC),
            venterPåBruker = venterPåBruker,
            venterPåØkonomi = venterPåØkonomi,
            forrigeBehandling = forrigeBehandling?.let(Behandling::eksternBrukId),
            revurderingOpprettetÅrsak = behandlingsårsak?.type,
            totalFeilutbetaltBeløp = totalFeilutbetaltBeløp,
            totalFeilutbetaltPeriode = totalFeilutbetaltPeriode,
        )
    }
}
