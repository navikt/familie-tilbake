package no.nav.familie.tilbake.forvaltning

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.api.forvaltning.Behandlingsinfo
import no.nav.familie.tilbake.api.forvaltning.Kravgrunnlagsinfo
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingsvedtakService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Iverksettingsstatus
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.datavarehus.saksstatistikk.BehandlingTilstandService
import no.nav.familie.tilbake.dokumentbestilling.vedtak.SendVedtaksbrevTask
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.AnnulerKravgrunnlagService
import no.nav.familie.tilbake.kravgrunnlag.HentKravgrunnlagService
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.familie.tilbake.kravgrunnlag.event.EndretKravgrunnlagEventPublisher
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattService
import no.nav.familie.tilbake.micrometer.TellerService
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.time.LocalDate
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
    private val historikkTaskService: HistorikkTaskService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val tellerService: TellerService,
    private val taskService: TaskService,
    private val endretKravgrunnlagEventPublisher: EndretKravgrunnlagEventPublisher,
) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    fun korrigerKravgrunnlag(
        behandlingId: UUID,
        kravgrunnlagId: BigInteger,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        sjekkOmBehandlingErAvsluttet(behandling)
        val hentetKravgrunnlag =
            hentKravgrunnlagService.hentKravgrunnlagFraØkonomi(
                kravgrunnlagId,
                KodeAksjon.HENT_KORRIGERT_KRAVGRUNNLAG,
            )

        val kravgrunnlag = kravgrunnlagRepository.findByEksternKravgrunnlagIdAndAktivIsTrue(kravgrunnlagId)
        if (kravgrunnlag != null) {
            kravgrunnlagRepository.update(kravgrunnlag.copy(aktiv = false))
        }
        hentKravgrunnlagService.lagreHentetKravgrunnlag(behandlingId, hentetKravgrunnlag)

        stegService.håndterSteg(behandlingId)
    }

    @Transactional
    fun korrigerKravgrunnlag(
        behandlingId: UUID,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        sjekkOmBehandlingErAvsluttet(behandling)

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
            )

        val kravgrunnlag = kravgrunnlagRepository.findByEksternKravgrunnlagIdAndAktivIsTrue(kravgrunnlagId)
        if (kravgrunnlag != null) {
            kravgrunnlagRepository.update(kravgrunnlag.copy(aktiv = false))
        }
        hentKravgrunnlagService.lagreHentetKravgrunnlag(behandlingId, hentetKravgrunnlag)

        stegService.håndterSteg(behandlingId)
    }

    @Transactional
    fun hoppOverIverksettingMotOppdrag(
        behandlingId: UUID,
        taskId: Long,
    ) {
        behandlingVedtakService.oppdaterBehandlingsvedtak(behandlingId, Iverksettingsstatus.IVERKSATT)

        behandlingskontrollService
            .oppdaterBehandlingsstegStatus(
                behandlingId,
                Behandlingsstegsinfo(
                    behandlingssteg = Behandlingssteg.IVERKSETT_VEDTAK,
                    behandlingsstegstatus = Behandlingsstegstatus.UTFØRT,
                ),
            )
        behandlingskontrollService.fortsettBehandling(behandlingId)

        val task = taskService.findById(taskId)
        taskService.save(
            Task(
                type = SendVedtaksbrevTask.TYPE,
                payload = task.payload,
                properties = task.metadata,
            ),
        )
        // Setter feilet task til ferdig.
        taskService.save(task.copy(status = Status.FERDIG))
    }

    @Transactional
    fun arkiverMottattKravgrunnlag(mottattXmlId: UUID) {
        logger.info("Arkiverer mottattXml for Id=$mottattXmlId")
        val mottattKravgrunnlag = økonomiXmlMottattService.hentMottattKravgrunnlag(mottattXmlId)
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
        sjekkOmBehandlingErAvsluttet(behandling)

        // oppdaterer behandlingsstegstilstand
        behandlingskontrollService.henleggBehandlingssteg(behandlingId)

        // oppdaterer behandlingsresultat og behandling
        val behandlingsresultat = Behandlingsresultat(type = Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD)
        behandlingRepository.update(
            behandling.copy(
                resultater = setOf(behandlingsresultat),
                status = Behandlingsstatus.AVSLUTTET,
                ansvarligSaksbehandler = ContextService.hentSaksbehandler(),
                avsluttetDato = LocalDate.now(),
            ),
        )
        behandlingTilstandService.opprettSendingAvBehandlingenHenlagt(behandlingId)

        historikkTaskService.lagHistorikkTask(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT,
            aktør = Aktør.SAKSBEHANDLER,
            beskrivelse = "",
        )
        oppgaveTaskService.ferdigstilleOppgaveTask(behandlingId)
        tellerService.tellVedtak(Behandlingsresultatstype.HENLAGT, behandling)
    }

    @Transactional
    fun flyttBehandlingsstegTilbakeTilFakta(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        sjekkOmBehandlingErAvsluttet(behandling)

        // fjerner eksisterende saksbehandlet data
        endretKravgrunnlagEventPublisher.fireEvent(behandlingId)
        behandlingskontrollService.behandleStegPåNytt(behandlingId, Behandlingssteg.FAKTA)

        historikkTaskService.lagHistorikkTask(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.BEHANDLING_FLYTTET_MED_FORVALTNING,
            Aktør.VEDTAKSLØSNING,
        )
    }

    fun annulerKravgrunnlag(eksternKravgrunnlagId: BigInteger) {
        val økonomiXmlMottatt = økonomiXmlMottattRepository.findByEksternKravgrunnlagId(eksternKravgrunnlagId)
        val kravgrunnlag431 = kravgrunnlagRepository.findByEksternKravgrunnlagIdAndAktivIsTrue(eksternKravgrunnlagId)
        if (økonomiXmlMottatt == null && kravgrunnlag431 == null) {
            throw Feil(message = "Finnes ikke eksternKravgrunnlagId=$eksternKravgrunnlagId")
        }
        val vedtakId = økonomiXmlMottatt?.vedtakId ?: kravgrunnlag431!!.vedtakId
        annulerKravgrunnlagService.annulerKravgrunnlagRequest(eksternKravgrunnlagId, vedtakId)
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
                "Finnes ikke kravgrunnlag som ikke er arkivert for ytelsestype=$ytelsestype og eksternFagsakId=$eksternFagsakId",
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

    private fun sjekkOmBehandlingErAvsluttet(behandling: Behandling) {
        if (behandling.erAvsluttet) {
            throw Feil(
                "Behandling med id=${behandling.id} er allerede ferdig behandlet.",
                frontendFeilmelding = "Behandling med id=${behandling.id} er allerede ferdig behandlet.",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }
}
