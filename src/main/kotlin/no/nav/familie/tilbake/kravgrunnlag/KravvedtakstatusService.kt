package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.tilbake.api.dto.HenleggelsesbrevFritekstDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.FagsystemUtil
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.exceptionhandler.UgyldigStatusmeldingFeil
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kontrakter.oppgave.Oppgavetype
import no.nav.familie.tilbake.kontrakter.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.micrometer.TellerService
import no.nav.familie.tilbake.oppgave.OppgaveService
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.tilbakekreving.status.v1.KravOgVedtakstatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
class KravvedtakstatusService(
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val behandlingRepository: BehandlingRepository,
    private val mottattXmlService: ØkonomiXmlMottattService,
    private val stegService: StegService,
    private val tellerService: TellerService,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val behandlingService: BehandlingService,
    private val historikkService: HistorikkService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val oppgaveService: OppgaveService,
) {
    private val log = LoggerFactory.getLogger(KravgrunnlagService::class.java)

    @Transactional
    fun håndterMottattStatusmelding(
        statusmeldingXml: String,
        taskId: Long,
        taskMetadata: Properties,
    ) {
        val kravOgVedtakstatus: KravOgVedtakstatus = KravgrunnlagUtil.unmarshalStatusmelding(statusmeldingXml)

        validerStatusmelding(kravOgVedtakstatus)

        val fagsystemId = kravOgVedtakstatus.fagsystemId
        val vedtakId = kravOgVedtakstatus.vedtakId
        val ytelsestype: Ytelsestype = KravgrunnlagUtil.tilYtelsestype(kravOgVedtakstatus.kodeFagomraade)

        val behandling: Behandling? = finnÅpenBehandling(ytelsestype, fagsystemId)

        log.info("BehandleStatusmeldingTask prosesserer med id={} og metadata {}", taskId, taskMetadata)
        SecureLog
            .medBehandling(fagsystemId, behandling?.id?.toString()) {
                info("BehandleStatusmeldingTask prosesserer med id={} og metadata {}", taskId, taskMetadata)
            }

        if (behandling == null) {
            val kravgrunnlagXmlListe =
                mottattXmlService
                    .hentMottattKravgrunnlag(
                        eksternFagsakId = fagsystemId,
                        ytelsestype = ytelsestype,
                        vedtakId = vedtakId,
                    )
            håndterStatusmeldingerUtenBehandling(kravgrunnlagXmlListe, kravOgVedtakstatus)
            mottattXmlService.arkiverMottattXml(kravgrunnlagXmlListe.maxByOrNull { it.kontrollfelt!! }?.id, statusmeldingXml, fagsystemId, ytelsestype)
            tellerService.tellUkobletStatusmelding(FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype))
            return
        }
        val kravgrunnlag431: Kravgrunnlag431 = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        val logContext = SecureLog.Context.medBehandling(kravgrunnlag431.fagsystemId, behandling.id.toString())
        håndterStatusmeldingerMedBehandling(kravgrunnlag431, kravOgVedtakstatus, behandling, logContext)
        mottattXmlService.arkiverMottattXml(mottattXmlId = null, statusmeldingXml, fagsystemId, ytelsestype)
        tellerService.tellKobletStatusmelding(FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype))
    }

    private fun validerStatusmelding(kravOgVedtakstatus: KravOgVedtakstatus) {
        kravOgVedtakstatus.referanse
            ?: throw UgyldigStatusmeldingFeil(
                melding =
                    "Ugyldig statusmelding for vedtakId=${kravOgVedtakstatus.vedtakId}, " +
                        "Mangler referanse.",
                SecureLog.Context.utenBehandling(kravOgVedtakstatus.fagsystemId),
            )
    }

    private fun finnÅpenBehandling(
        ytelsestype: Ytelsestype,
        fagsystemId: String,
    ): Behandling? =
        behandlingRepository.finnÅpenTilbakekrevingsbehandling(
            ytelsestype = ytelsestype,
            eksternFagsakId = fagsystemId,
        )

    private fun håndterStatusmeldingerUtenBehandling(
        kravgrunnlagXmlListe: List<ØkonomiXmlMottatt>,
        kravOgVedtakstatus: KravOgVedtakstatus,
    ) {
        when (val kravstatuskode = Kravstatuskode.fraKode(kravOgVedtakstatus.kodeStatusKrav)) {
            Kravstatuskode.SPERRET, Kravstatuskode.MANUELL ->
                kravgrunnlagXmlListe.forEach { mottattXmlService.oppdaterMottattXml(it.copy(sperret = true)) }
            Kravstatuskode.ENDRET ->
                kravgrunnlagXmlListe.forEach {
                    mottattXmlService
                        .oppdaterMottattXml(it.copy(sperret = false))
                }
            Kravstatuskode.AVSLUTTET ->
                kravgrunnlagXmlListe.forEach {
                    mottattXmlService.arkiverMottattXml(
                        mottattXmlId = it.id,
                        it.melding,
                        it.eksternFagsakId,
                        it.ytelsestype,
                    )
                    mottattXmlService.slettMottattXml(it.id)
                }
            else -> throw IllegalArgumentException("Ukjent statuskode $kravstatuskode i statusmelding")
        }
    }

    private fun håndterStatusmeldingerMedBehandling(
        kravgrunnlag431: Kravgrunnlag431,
        kravOgVedtakstatus: KravOgVedtakstatus,
        behandling: Behandling,
        logContext: SecureLog.Context,
    ) {
        when (val kravstatuskode = Kravstatuskode.fraKode(kravOgVedtakstatus.kodeStatusKrav)) {
            Kravstatuskode.SPERRET, Kravstatuskode.MANUELL -> {
                håndterSperMeldingMedBehandling(behandling.id, kravgrunnlag431)
            }
            Kravstatuskode.ENDRET -> {
                kravgrunnlagRepository.update(kravgrunnlag431.copy(sperret = false))
                stegService.håndterSteg(behandling.id, SecureLog.Context.medBehandling(kravgrunnlag431.fagsystemId, behandling.id.toString()))
                oppgaveTaskService.oppdaterOppgaveTask(
                    behandlingId = behandling.id,
                    beskrivelse = "Behandling er tatt av vent, pga mottatt ENDR melding",
                    frist = LocalDate.now(),
                    logContext = logContext,
                )
            }
            Kravstatuskode.AVSLUTTET -> {
                kravgrunnlagRepository.update(kravgrunnlag431.copy(avsluttet = true))
                behandlingService
                    .henleggBehandling(
                        behandlingId = behandling.id,
                        HenleggelsesbrevFritekstDto(
                            behandlingsresultatstype =
                                Behandlingsresultatstype
                                    .HENLAGT_KRAVGRUNNLAG_NULLSTILT,
                            begrunnelse = "",
                        ),
                    )
            }
            else -> throw IllegalArgumentException("Ukjent statuskode $kravstatuskode i statusmelding")
        }
    }

    @Transactional
    fun håndterSperMeldingMedBehandling(
        behandlingId: UUID,
        kravgrunnlag431: Kravgrunnlag431,
    ) {
        val logContext = SecureLog.Context.medBehandling(kravgrunnlag431.fagsystemId, behandlingId.toString())
        kravgrunnlagRepository.update(kravgrunnlag431.copy(sperret = true))
        val venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG
        val tidsfrist = LocalDate.now().plusWeeks(venteårsak.defaultVenteTidIUker)
        behandlingskontrollService
            .tilbakehoppBehandlingssteg(
                behandlingId,
                Behandlingsstegsinfo(
                    behandlingssteg = Behandlingssteg.GRUNNLAG,
                    behandlingsstegstatus = Behandlingsstegstatus.VENTER,
                    venteårsak = venteårsak,
                    tidsfrist = tidsfrist,
                ),
                logContext = logContext,
            )
        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT,
            aktør = Aktør.Vedtaksløsning,
            opprettetTidspunkt = LocalDateTime.now(),
            beskrivelse = venteårsak.beskrivelse,
        )

        // oppgave oppdateres ikke dersom behandling venter på varsel
        val aktivtBehandlingssteg = behandlingskontrollService.finnAktivtSteg(behandlingId)
        if (aktivtBehandlingssteg?.let { it != Behandlingssteg.VARSEL } == true) {
            val oppgave = oppgaveService.finnOppgaveForBehandlingUtenOppgaveType(behandlingId)
            val behandleSakOppgavetyper = listOf(Oppgavetype.BehandleSak.value, Oppgavetype.BehandleUnderkjentVedtak.value)
            if (behandleSakOppgavetyper.contains(oppgave.oppgavetype)) {
                oppgaveTaskService.oppdaterOppgaveTask(
                    behandlingId = behandlingId,
                    beskrivelse = venteårsak.beskrivelse,
                    frist = tidsfrist,
                    logContext = logContext,
                )
            } else {
                oppgaveTaskService.ferdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgave(behandlingId = behandlingId, beskrivelse = venteårsak.beskrivelse, frist = tidsfrist, logContext = logContext)
            }
        }
    }
}
