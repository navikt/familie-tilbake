package no.nav.familie.tilbake.forvaltning

import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.api.forvaltning.Behandlingsinfo
import no.nav.familie.tilbake.api.forvaltning.Kravgrunnlagsinfo
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingsvedtakService
import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Iverksettingsstatus
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.exceptionhandler.feilHvis
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.datavarehus.saksstatistikk.BehandlingTilstandService
import no.nav.familie.tilbake.dokumentbestilling.vedtak.SendVedtaksbrevTask
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.AnnulerKravgrunnlagService
import no.nav.familie.tilbake.kravgrunnlag.HentKravgrunnlagService
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.familie.tilbake.kravgrunnlag.event.EndretKravgrunnlagEventPublisher
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattService
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.micrometer.TellerService
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsresultatstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class ForvaltningService(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingVedtakService: BehandlingsvedtakService,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val økonomiXmlMottattRepository: ØkonomiXmlMottattRepository,
    private val hentKravgrunnlagService: HentKravgrunnlagService,
    private val annulerKravgrunnlagService: AnnulerKravgrunnlagService,
    private val økonomiXmlMottattService: ØkonomiXmlMottattService,
    private val stegService: StegService,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val behandlingTilstandService: BehandlingTilstandService,
    private val historikkService: HistorikkService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val tellerService: TellerService,
    private val taskService: TracableTaskService,
    private val endretKravgrunnlagEventPublisher: EndretKravgrunnlagEventPublisher,
    private val logService: LogService,
) {
    private val log = TracedLogger.getLogger<ForvaltningService>()

    @Transactional
    fun korrigerKravgrunnlag(
        behandlingId: UUID,
        kravgrunnlagId: BigInteger,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val logContext = logService.contextFraBehandling(behandlingId)
        sjekkOmBehandlingErAvsluttet(behandling, logContext)
        val hentetKravgrunnlag =
            hentKravgrunnlagService.hentKravgrunnlagFraØkonomi(
                kravgrunnlagId,
                KodeAksjon.HENT_KORRIGERT_KRAVGRUNNLAG,
                logContext,
            )

        log.medContext(logContext) {
            info("Nytt kravgrunnlag: ${hentetKravgrunnlag.tilbakekrevingsPeriode}")
        }

        val kravgrunnlag = kravgrunnlagRepository.findByEksternKravgrunnlagIdAndAktivIsTrue(kravgrunnlagId)
        if (kravgrunnlag != null) {
            kravgrunnlagRepository.update(kravgrunnlag.copy(aktiv = false))
        }
        hentKravgrunnlagService.lagreHentetKravgrunnlag(behandlingId, hentetKravgrunnlag, logContext)

        stegService.håndterSteg(behandlingId, logContext)
    }

    @Transactional
    fun korrigerKravgrunnlag(
        behandlingId: UUID,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val logContext = logService.contextFraBehandling(behandling.id)
        sjekkOmBehandlingErAvsluttet(behandling, logContext)

        val kravgrunnlagId =
            kravgrunnlagRepository
                .findByBehandlingId(behandling.id)
                .filter { it.aktiv }
                .first()
                .eksternKravgrunnlagId
        val hentetKravgrunnlag =
            hentKravgrunnlagService.hentKravgrunnlagFraØkonomi(
                kravgrunnlagId,
                KodeAksjon.HENT_KORRIGERT_KRAVGRUNNLAG,
                logContext,
            )

        log.medContext(logContext) {
            info("Nytt kravgrunnlag: ${hentetKravgrunnlag.tilbakekrevingsPeriode}")
        }

        val kravgrunnlag = kravgrunnlagRepository.findByEksternKravgrunnlagIdAndAktivIsTrue(kravgrunnlagId)
        if (kravgrunnlag != null) {
            kravgrunnlagRepository.update(kravgrunnlag.copy(aktiv = false))
        }
        hentKravgrunnlagService.lagreHentetKravgrunnlag(behandlingId, hentetKravgrunnlag, logContext)

        stegService.håndterSteg(behandlingId, logContext)
    }

    @Transactional
    fun hoppOverIverksettingMotOppdrag(
        behandlingId: UUID,
        taskId: Long,
    ) {
        val logContext = logService.contextFraBehandling(behandlingId)
        behandlingVedtakService.oppdaterBehandlingsvedtak(behandlingId, Iverksettingsstatus.IVERKSATT)

        behandlingskontrollService
            .oppdaterBehandlingsstegStatus(
                behandlingId,
                Behandlingsstegsinfo(
                    behandlingssteg = Behandlingssteg.IVERKSETT_VEDTAK,
                    behandlingsstegstatus = Behandlingsstegstatus.UTFØRT,
                ),
                logContext,
            )
        behandlingskontrollService.fortsettBehandling(behandlingId, logContext)

        val task = taskService.findById(taskId)
        taskService.save(
            Task(
                type = SendVedtaksbrevTask.TYPE,
                payload = task.payload,
                properties = task.metadata,
            ),
            logContext,
        )
        // Setter feilet task til ferdig.
        taskService.save(task.copy(status = Status.FERDIG), logContext)
    }

    @Transactional
    fun arkiverMottattKravgrunnlag(mottattXmlId: UUID) {
        val mottattKravgrunnlag = økonomiXmlMottattService.hentMottattKravgrunnlag(mottattXmlId)
        val logContext = SecureLog.Context.utenBehandling(mottattKravgrunnlag.eksternFagsakId)
        log.medContext(logContext) {
            info("Arkiverer mottattXml for Id=$mottattXmlId")
        }
        økonomiXmlMottattService.arkiverMottattXml(
            mottattKravgrunnlag.id,
            mottattKravgrunnlag.melding,
            mottattKravgrunnlag.eksternFagsakId,
            mottattKravgrunnlag.ytelsestype,
        )
        økonomiXmlMottattService.slettMottattXml(mottattXmlId)
    }

    @Transactional
    fun tvingHenleggBehandling(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val logContext = logService.contextFraBehandling(behandling.id)
        sjekkOmBehandlingErAvsluttet(behandling, logContext)

        // oppdaterer behandlingsstegstilstand
        behandlingskontrollService.henleggBehandlingssteg(behandlingId)

        // oppdaterer behandlingsresultat og behandling
        val behandlingsresultat = Behandlingsresultat(type = Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD)
        behandlingRepository.update(
            behandling.copy(
                resultater = setOf(behandlingsresultat),
                status = Behandlingsstatus.AVSLUTTET,
                ansvarligSaksbehandler = ContextService.hentSaksbehandler(logContext),
                avsluttetDato = LocalDate.now(),
            ),
        )
        behandlingTilstandService.opprettSendingAvBehandlingenHenlagt(behandlingId, logContext)

        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT,
            aktør = Aktør.Saksbehandler.fraBehandling(behandlingId, behandlingRepository),
            opprettetTidspunkt = LocalDateTime.now(),
            beskrivelse = "",
        )
        oppgaveTaskService.ferdigstilleOppgaveTask(behandlingId)
        tellerService.tellVedtak(Behandlingsresultatstype.HENLAGT, behandling)
    }

    @Transactional
    fun flyttBehandlingsstegTilbakeTilFakta(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val logContext = logService.contextFraBehandling(behandling.id)
        sjekkOmBehandlingErAvsluttet(behandling, logContext)

        // fjerner eksisterende saksbehandlet data
        endretKravgrunnlagEventPublisher.fireEvent(behandlingId)
        behandlingskontrollService.behandleStegPåNytt(behandlingId, Behandlingssteg.FAKTA, logContext)

        historikkService.lagHistorikkinnslag(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.BEHANDLING_FLYTTET_MED_FORVALTNING,
            Aktør.Vedtaksløsning,
            LocalDateTime.now(),
        )
    }

    fun annulerKravgrunnlag(eksternKravgrunnlagId: BigInteger) {
        val økonomiXmlMottatt = økonomiXmlMottattRepository.findByEksternKravgrunnlagId(eksternKravgrunnlagId)
        val kravgrunnlag431 = kravgrunnlagRepository.findByEksternKravgrunnlagIdAndAktivIsTrue(eksternKravgrunnlagId)
        if (økonomiXmlMottatt == null && kravgrunnlag431 == null) {
            throw Feil(
                message = "Finnes ikke eksternKravgrunnlagId=$eksternKravgrunnlagId",
                logContext = SecureLog.Context.tom(),
            )
        }
        val vedtakId = økonomiXmlMottatt?.vedtakId ?: kravgrunnlag431!!.vedtakId
        val fagsakId = økonomiXmlMottatt?.eksternFagsakId ?: kravgrunnlag431?.fagsystemId!!
        val logContext = SecureLog.Context.medBehandling(fagsakId, kravgrunnlag431?.behandlingId?.toString())
        annulerKravgrunnlagService.annulerKravgrunnlagRequest(eksternKravgrunnlagId, vedtakId, logContext)
    }

    fun hentForvaltningsinfo(
        ytelsestype: Ytelsestype,
        eksternFagsakId: String,
    ): List<Behandlingsinfo> {
        val behandling = behandlingRepository.finnNyesteTilbakekrevingsbehandlingForYtelsestypeAndEksternFagsakId(ytelsestype, eksternFagsakId)
        if (behandling != null) {
            val kravgrunnlag431 = kravgrunnlagRepository.findByBehandlingId(behandling.id).filter { it.aktiv }
            return kravgrunnlag431.map { kravgrunnlag ->
                Behandlingsinfo(
                    eksternKravgrunnlagId = kravgrunnlag.eksternKravgrunnlagId,
                    kravgrunnlagId = kravgrunnlag.id,
                    kravgrunnlagKravstatuskode = kravgrunnlag.kravstatuskode.kode,
                    eksternId = kravgrunnlag.referanse,
                    opprettetTid = kravgrunnlag.sporbar.opprettetTid,
                    behandlingId = behandling.id,
                    behandlingstatus = behandling.status,
                )
            }
        }
        return listOf()
    }

    fun hentIkkeArkiverteKravgrunnlag(
        ytelsestype: Ytelsestype,
        eksternFagsakId: String,
    ): List<Kravgrunnlagsinfo> {
        val økonomiXmlMottatt =
            økonomiXmlMottattRepository.findByEksternFagsakIdAndYtelsestype(eksternFagsakId, ytelsestype)
        if (økonomiXmlMottatt.isEmpty()) {
            throw Feil(
                message = "Finnes ikke kravgrunnlag som ikke er arkivert for ytelsestype=$ytelsestype og eksternFagsakId=$eksternFagsakId",
                logContext = SecureLog.Context.utenBehandling(eksternFagsakId),
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        return økonomiXmlMottatt.map { xml ->
            Kravgrunnlagsinfo(
                eksternKravgrunnlagId = xml.eksternKravgrunnlagId!!,
                kravgrunnlagKravstatuskode = xml.kravstatuskode.navn,
                mottattXmlId = xml.id,
                eksternId = xml.referanse,
                opprettetTid = xml.sporbar.opprettetTid,
            )
        }
    }

    private fun sjekkOmBehandlingErAvsluttet(
        behandling: Behandling,
        logContext: SecureLog.Context,
    ) {
        feilHvis(
            behandling.erAvsluttet,
            logContext = logContext,
            httpStatus = HttpStatus.BAD_REQUEST,
        ) {
            "Behandling med id=${behandling.id} er allerede ferdig behandlet."
        }
    }
}
