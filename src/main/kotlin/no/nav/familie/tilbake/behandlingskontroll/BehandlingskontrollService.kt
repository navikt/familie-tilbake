package no.nav.familie.tilbake.behandlingskontroll

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.AUTOUTFØRT
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.AVBRUTT
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.KLAR
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.TILBAKEFØRT
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.UTFØRT
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.VENTER
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.datavarehus.saksstatistikk.BehandlingTilstandService
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerRepository
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.kontrakter.tilbakekreving.Tilbakekrevingsvalg
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class BehandlingskontrollService(
    private val behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository,
    private val behandlingRepository: BehandlingRepository,
    private val behandlingTilstandService: BehandlingTilstandService,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val historikkService: HistorikkService,
    private val brevmottakerRepository: ManuellBrevmottakerRepository,
) {
    private val log = TracedLogger.getLogger<BehandlingskontrollService>()

    @Transactional
    fun fortsettBehandling(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.erAvsluttet) {
            return
        }
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        val aktivtStegstilstand = finnAktivStegstilstand(behandlingsstegstilstand)

        if (aktivtStegstilstand == null) {
            val nesteStegMetaData = finnNesteBehandlingsstegMedStatus(behandling, behandlingsstegstilstand)
            persisterBehandlingsstegOgStatus(behandlingId, nesteStegMetaData, logContext)
            if (nesteStegMetaData.behandlingsstegstatus == VENTER) {
                historikkService.lagHistorikkinnslag(
                    behandlingId = behandlingId,
                    historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT,
                    aktør = Aktør.Vedtaksløsning,
                    opprettetTidspunkt = LocalDateTime.now(),
                    beskrivelse = nesteStegMetaData.venteårsak?.beskrivelse,
                )
            }
        } else {
            log.medContext(logContext) {
                info(
                    "Behandling har allerede et aktivt steg=${aktivtStegstilstand.behandlingssteg} " +
                        "med status=${aktivtStegstilstand.behandlingsstegsstatus}",
                )
            }
        }
    }

    @Transactional
    fun tilbakehoppBehandlingssteg(
        behandlingId: UUID,
        behandlingsstegsinfo: Behandlingsstegsinfo,
        logContext: SecureLog.Context,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.erAvsluttet) {
            throw Feil(
                message =
                    "Behandling med id=$behandlingId er allerede ferdig behandlet, " +
                        "så kan ikke forsette til ${behandlingsstegsinfo.behandlingssteg}",
                logContext = logContext,
            )
        }
        val behandlingsstegstilstand: List<Behandlingsstegstilstand> =
            behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        val aktivtBehandlingssteg =
            finnAktivStegstilstand(behandlingsstegstilstand)
                ?: throw Feil(
                    message = "Behandling med id=$behandlingId har ikke noe aktivt steg",
                    logContext = logContext,
                )
        // steg som kan behandles, kan avbrytes
        if (aktivtBehandlingssteg.behandlingssteg.kanSaksbehandles) {
            behandlingsstegstilstandRepository.update(aktivtBehandlingssteg.copy(behandlingsstegsstatus = AVBRUTT))
            persisterBehandlingsstegOgStatus(behandlingId, behandlingsstegsinfo, logContext)
        }
    }

    @Transactional
    fun tilbakeførBehandledeSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        val alleIkkeVentendeSteg =
            behandlingsstegstilstand
                .filter { it.behandlingsstegsstatus != VENTER }
                .filter { it.behandlingssteg !in listOf(Behandlingssteg.VARSEL, Behandlingssteg.GRUNNLAG) }
        alleIkkeVentendeSteg.forEach {
            log.medContext(logContext) {
                info("Tilbakefører ${it.behandlingssteg} for behandling $behandlingId")
            }
            oppdaterBehandlingsstegStatus(behandlingId, Behandlingsstegsinfo(it.behandlingssteg, TILBAKEFØRT), logContext)
        }
    }

    @Transactional
    fun behandleStegPåNytt(
        behandlingId: UUID,
        behandledeSteg: Behandlingssteg,
        logContext: SecureLog.Context,
    ) {
        val aktivtBehandlingssteg =
            finnAktivtSteg(behandlingId)
                ?: throw Feil(
                    message = "Behandling med id=$behandlingId har ikke noe aktivt steg",
                    logContext = logContext,
                )

        if (behandledeSteg.sekvens < aktivtBehandlingssteg.sekvens) {
            for (i in aktivtBehandlingssteg.sekvens downTo behandledeSteg.sekvens + 1 step 1) {
                val behandlingssteg = Behandlingssteg.fraSekvens(i, sjekkOmBrevmottakerErstatterVergeForSekvens(i, behandlingId))
                oppdaterBehandlingsstegStatus(behandlingId, Behandlingsstegsinfo(behandlingssteg, TILBAKEFØRT), logContext)
            }
            oppdaterBehandlingsstegStatus(behandlingId, Behandlingsstegsinfo(behandledeSteg, KLAR), logContext)
        }
    }

    @Transactional
    fun behandleVergeSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        tilbakeførBehandledeSteg(behandlingId, logContext)
        log.medContext(logContext) {
            info("Oppretter verge steg for behandling med id=$behandlingId")
        }
        val eksisterendeVergeSteg =
            behandlingsstegstilstandRepository.findByBehandlingIdAndBehandlingssteg(
                behandlingId,
                Behandlingssteg.VERGE,
            )
        when {
            eksisterendeVergeSteg != null -> {
                oppdaterBehandlingsstegStatus(behandlingId, Behandlingsstegsinfo(Behandlingssteg.VERGE, KLAR), logContext)
            }
            else -> {
                opprettBehandlingsstegOgStatus(behandlingId, Behandlingsstegsinfo(Behandlingssteg.VERGE, KLAR), true, logContext)
            }
        }
    }

    @Transactional
    fun behandleBrevmottakerSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Aktiverer brevmottaker steg for behandling med id=$behandlingId")
        }
        behandlingsstegstilstandRepository
            .findByBehandlingIdAndBehandlingssteg(
                behandlingId,
                Behandlingssteg.BREVMOTTAKER,
            ) ?.apply {
                oppdaterBehandlingsstegStatus(behandlingId, Behandlingsstegsinfo(Behandlingssteg.BREVMOTTAKER, AUTOUTFØRT), logContext)
            } ?: opprettBehandlingsstegOgStatus(
            behandlingId = behandlingId,
            nesteStegMedStatus = Behandlingsstegsinfo(Behandlingssteg.BREVMOTTAKER, AUTOUTFØRT),
            // da det settes AUTOUTFØRT, forblir aktivt steg / tilstanden den samme
            opprettSendingAvBehandlingensTilstand = false,
            logContext = logContext,
        )
    }

    @Transactional
    fun settBehandlingPåVent(
        behandlingId: UUID,
        venteårsak: Venteårsak,
        tidsfrist: LocalDate,
        logContext: SecureLog.Context,
    ) {
        val behandlingsstegstilstand: List<Behandlingsstegstilstand> =
            behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        val aktivtBehandlingsstegstilstand =
            finnAktivStegstilstand(behandlingsstegstilstand)
                ?: throw Feil(
                    message =
                        "Behandling $behandlingId " +
                            "har ikke aktivt steg",
                    frontendFeilmelding =
                        "Behandling $behandlingId " +
                            "har ikke aktivt steg",
                    logContext = logContext,
                )
        behandlingsstegstilstandRepository.update(
            aktivtBehandlingsstegstilstand.copy(
                behandlingsstegsstatus = VENTER,
                venteårsak = venteårsak,
                tidsfrist = tidsfrist,
            ),
        )
        // oppdater tilsvarende behandlingsstatus
        oppdaterBehandlingsstatus(behandlingId, aktivtBehandlingsstegstilstand.behandlingssteg)

        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BEHANDLING_PÅ_VENT,
            aktør = Aktør.Saksbehandler.fraBehandling(behandlingId, behandlingRepository),
            opprettetTidspunkt = LocalDateTime.now(),
            beskrivelse = venteårsak.beskrivelse,
        )
    }

    @Transactional
    fun henleggBehandlingssteg(behandlingId: UUID) {
        val behandlingsstegstilstand: List<Behandlingsstegstilstand> =
            behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        behandlingsstegstilstand
            .filter { it.behandlingssteg != Behandlingssteg.VARSEL }
            .forEach {
                behandlingsstegstilstandRepository.update(it.copy(behandlingsstegsstatus = AVBRUTT))
            }
    }

    fun erBehandlingPåVent(behandlingId: UUID): Boolean {
        val behandlingsstegstilstand: List<Behandlingsstegstilstand> =
            behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        val aktivtBehandlingsstegstilstand: Behandlingsstegstilstand =
            finnAktivStegstilstand(behandlingsstegstilstand)
                ?: return false
        return VENTER == aktivtBehandlingsstegstilstand.behandlingsstegsstatus
    }

    fun hentBehandlingsstegstilstand(behandling: Behandling): List<Behandlingsstegsinfo> {
        val behandlingsstegstilstand: List<Behandlingsstegstilstand> =
            behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        return behandlingsstegstilstand.map {
            Behandlingsstegsinfo(
                behandlingssteg = it.behandlingssteg,
                behandlingsstegstatus = it.behandlingsstegsstatus,
                venteårsak = it.venteårsak,
                tidsfrist = it.tidsfrist,
            )
        }
    }

    fun finnAktivtSteg(behandlingId: UUID): Behandlingssteg? {
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandlingId)
        return finnAktivStegstilstand(behandlingsstegstilstand)?.behandlingssteg
    }

    fun finnAktivStegstilstand(behandlingsstegstilstand: List<Behandlingsstegstilstand>): Behandlingsstegstilstand? {
        return behandlingsstegstilstand
            .firstOrNull { Behandlingsstegstatus.erStegAktiv(it.behandlingsstegsstatus) }
        // forutsetter at behandling kan ha kun et aktiv steg om gangen
    }

    fun finnAktivStegstilstand(behandlingId: UUID): Behandlingsstegstilstand? = finnAktivStegstilstand(behandlingsstegstilstandRepository.findByBehandlingId(behandlingId))

    @Transactional
    fun oppdaterBehandlingsstegStatus(
        behandlingId: UUID,
        behandlingsstegsinfo: Behandlingsstegsinfo,
        logContext: SecureLog.Context,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.erAvsluttet &&
            (
                behandlingsstegsinfo.behandlingssteg != Behandlingssteg.AVSLUTTET &&
                    behandlingsstegsinfo.behandlingsstegstatus != UTFØRT
            )
        ) {
            throw Feil(
                "Behandling med id=$behandlingId er allerede ferdig behandlet, " +
                    "så status=${behandlingsstegsinfo.behandlingsstegstatus} kan ikke oppdateres",
                logContext = logContext,
            )
        }
        val behandlingsstegstilstand =
            behandlingsstegstilstandRepository
                .findByBehandlingIdAndBehandlingssteg(behandlingId, behandlingsstegsinfo.behandlingssteg)
                ?: throw Feil(
                    message =
                        "Behandling med id=$behandlingId og " +
                            "steg=${behandlingsstegsinfo.behandlingssteg} finnes ikke",
                    logContext = logContext,
                )

        behandlingsstegstilstandRepository
            .update(
                behandlingsstegstilstand.copy(
                    behandlingsstegsstatus = behandlingsstegsinfo.behandlingsstegstatus,
                    venteårsak = behandlingsstegsinfo.venteårsak,
                    tidsfrist = behandlingsstegsinfo.tidsfrist,
                ),
            )

        // oppdater tilsvarende behandlingsstatus
        oppdaterBehandlingsstatus(behandlingId, behandlingsstegsinfo.behandlingssteg)
        behandlingTilstandService.opprettSendingAvBehandlingensTilstand(behandlingId, behandlingsstegsinfo, logContext)
    }

    private fun opprettBehandlingsstegOgStatus(
        behandlingId: UUID,
        nesteStegMedStatus: Behandlingsstegsinfo,
        opprettSendingAvBehandlingensTilstand: Boolean,
        logContext: SecureLog.Context,
    ) {
        // startet nytt behandlingssteg
        behandlingsstegstilstandRepository
            .insert(
                Behandlingsstegstilstand(
                    behandlingId = behandlingId,
                    behandlingssteg = nesteStegMedStatus.behandlingssteg,
                    venteårsak = nesteStegMedStatus.venteårsak,
                    tidsfrist = nesteStegMedStatus.tidsfrist,
                    behandlingsstegsstatus = nesteStegMedStatus.behandlingsstegstatus,
                ),
            )
        // oppdater tilsvarende behandlingsstatus
        oppdaterBehandlingsstatus(behandlingId, nesteStegMedStatus.behandlingssteg)
        if (opprettSendingAvBehandlingensTilstand) {
            behandlingTilstandService.opprettSendingAvBehandlingensTilstand(behandlingId, nesteStegMedStatus, logContext)
        }
    }

    private fun persisterBehandlingsstegOgStatus(
        behandlingId: UUID,
        behandlingsstegsinfo: Behandlingsstegsinfo,
        logContext: SecureLog.Context,
    ) {
        val gammelBehandlingsstegstilstand =
            behandlingsstegstilstandRepository.findByBehandlingIdAndBehandlingssteg(
                behandlingId,
                behandlingsstegsinfo.behandlingssteg,
            )
        when (gammelBehandlingsstegstilstand) {
            null -> {
                opprettBehandlingsstegOgStatus(behandlingId, behandlingsstegsinfo, true, logContext)
            }
            else -> {
                oppdaterBehandlingsstegStatus(behandlingId, behandlingsstegsinfo, logContext)
            }
        }
    }

    private fun finnNesteBehandlingsstegMedStatus(
        behandling: Behandling,
        stegstilstand: List<Behandlingsstegstilstand>,
    ): Behandlingsstegsinfo {
        if (stegstilstand.isEmpty()) {
            return when {
                // setter tidsfristen fra opprettelse dato
                kanSendeVarselsbrev(behandling) ->
                    lagBehandlingsstegsinfo(
                        Behandlingssteg.VARSEL,
                        VENTER,
                        Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                        behandling.opprettetDato,
                    )
                !harAktivtGrunnlag(behandling) ->
                    lagBehandlingsstegsinfo(
                        Behandlingssteg.GRUNNLAG,
                        VENTER,
                        Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                        behandling.opprettetDato,
                    )
                else -> lagBehandlingsstegsinfo(Behandlingssteg.FAKTA, KLAR)
            }
        }

        val finnesAvbruttSteg = stegstilstand.any { AVBRUTT == it.behandlingsstegsstatus }
        if (finnesAvbruttSteg) {
            // forutsetter behandling har et AVBRUTT steg om gangen
            val avbruttSteg = stegstilstand.first { AVBRUTT == it.behandlingsstegsstatus }
            return lagBehandlingsstegsinfo(avbruttSteg.behandlingssteg, KLAR)
        }

        // Finn sisteUtførteSteg. Dersom tidspunkt for to steg er likt, bruk den med høyest sekvensnummer.
        val sisteUtførteSteg =
            stegstilstand
                .filter { Behandlingsstegstatus.erStegUtført(it.behandlingsstegsstatus) }
                .maxWithOrNull(compareBy({ it.sporbar.endret.endretTid }, { it.behandlingssteg.sekvens }))!!
                .behandlingssteg

        if (Behandlingssteg.VARSEL == sisteUtførteSteg) {
            return håndterOmSisteUtførteStegErVarsel(behandling)
        }
        return lagBehandlingsstegsinfo(
            behandlingssteg =
                Behandlingssteg.finnNesteBehandlingssteg(
                    behandlingssteg = sisteUtførteSteg,
                    harVerge = behandling.harVerge,
                    harManuelleBrevmottakere = brevmottakerRepository.findByBehandlingId(behandling.id).isNotEmpty(),
                ),
            KLAR,
        )
    }

    private fun håndterOmSisteUtførteStegErVarsel(behandling: Behandling): Behandlingsstegsinfo =
        when {
            erKravgrunnlagSperret(behandling) -> {
                val kravgrunnlag =
                    kravgrunnlagRepository
                        .findByBehandlingIdAndAktivIsTrueAndSperretTrue(behandling.id)

                // setter tidsfristen fra sperret dato
                lagBehandlingsstegsinfo(
                    behandlingssteg = Behandlingssteg.GRUNNLAG,
                    behandlingsstegstatus = VENTER,
                    venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                    tidsfrist =
                        kravgrunnlag.sporbar.endret.endretTid
                            .toLocalDate(),
                )
            }
            harAktivtGrunnlag(behandling) -> {
                if (behandling.harVerge) {
                    lagBehandlingsstegsinfo(behandlingssteg = Behandlingssteg.VERGE, behandlingsstegstatus = KLAR)
                } else {
                    lagBehandlingsstegsinfo(behandlingssteg = Behandlingssteg.FAKTA, behandlingsstegstatus = KLAR)
                }
            }
            // setter tidsfristen fra opprettelse dato
            else ->
                lagBehandlingsstegsinfo(
                    behandlingssteg = Behandlingssteg.GRUNNLAG,
                    behandlingsstegstatus = VENTER,
                    venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                    tidsfrist = behandling.opprettetDato,
                )
        }

    private fun kanSendeVarselsbrev(behandling: Behandling): Boolean =
        Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL == behandling.aktivFagsystemsbehandling.tilbakekrevingsvalg &&
            !behandling.manueltOpprettet &&
            !behandling.erRevurdering

    private fun harAktivtGrunnlag(behandling: Behandling): Boolean = kravgrunnlagRepository.existsByBehandlingIdAndAktivTrueAndSperretFalse(behandling.id)

    private fun erKravgrunnlagSperret(behandling: Behandling): Boolean = kravgrunnlagRepository.existsByBehandlingIdAndAktivTrueAndSperretTrue(behandling.id)

    private fun lagBehandlingsstegsinfo(
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
        venteårsak: Venteårsak? = null,
        tidsfrist: LocalDate? = null,
    ): Behandlingsstegsinfo =
        Behandlingsstegsinfo(
            behandlingssteg = behandlingssteg,
            behandlingsstegstatus = behandlingsstegstatus,
            venteårsak = venteårsak,
            tidsfrist = venteårsak?.defaultVenteTidIUker?.let { tidsfrist?.plusWeeks(it) },
        )

    private fun oppdaterBehandlingsstatus(
        behandlingId: UUID,
        behandlingssteg: Behandlingssteg,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        // Oppdaterer tilsvarende behandlingsstatus bortsett fra Avsluttet steg. Det håndteres separat av AvsluttBehandlingTask
        if (Behandlingssteg.AVSLUTTET != behandlingssteg) {
            behandlingRepository.update(behandling.copy(status = behandlingssteg.behandlingsstatus))
        }
    }

    private fun sjekkOmBrevmottakerErstatterVergeForSekvens(
        sekvens: Int,
        behandlingId: UUID,
    ) = sekvens == Behandlingssteg.VERGE.sekvens &&
        behandlingsstegstilstandRepository
            .findByBehandlingIdAndBehandlingssteg(behandlingId, Behandlingssteg.BREVMOTTAKER) != null
}
