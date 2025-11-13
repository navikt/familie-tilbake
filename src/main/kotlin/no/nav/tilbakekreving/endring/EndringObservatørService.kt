package no.nav.tilbakekreving.endring

import no.nav.familie.tilbake.datavarehus.saksstatistikk.sakshendelse.Behandlingstilstand
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.SærligeGrunner
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.UtvidetVilkårsresultat
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.VedtakPeriode
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.Vedtaksoppsummering
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.api.v2.PeriodeDto
import no.nav.tilbakekreving.api.v2.fagsystem.BehandlingEndretHendelse
import no.nav.tilbakekreving.api.v2.fagsystem.ForenkletBehandlingsstatus
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.config.Toggles
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.Periode
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsresultatstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

@Service
class EndringObservatørService(
    private val kafkaProducer: KafkaProducer,
    private val applicationProperties: ApplicationProperties,
) : EndringObservatør {
    override fun behandlingsstatusOppdatert(
        behandlingId: UUID,
        forrigeBehandlingId: UUID?,
        eksternFagsystemId: String,
        eksternBehandlingId: String,
        ytelse: Ytelse,
        tilstand: TilbakekrevingTilstand,
        behandlingstatus: Behandlingsstatus,
        vedtaksresultat: Vedtaksresultat?,
        venterPåBruker: Boolean,
        ansvarligEnhet: String?,
        ansvarligSaksbehandler: String,
        ansvarligBeslutter: String?,
        totaltFeilutbetaltBeløp: BigDecimal?,
        totalFeilutbetaltPeriode: Datoperiode?,
    ) {
        kafkaProducer.sendSaksdata(
            behandlingId,
            Behandlingstilstand(
                funksjoneltTidspunkt = OffsetDateTime.now(),
                tekniskTidspunkt = OffsetDateTime.now(),
                saksnummer = eksternFagsystemId,
                fagsystem = ytelse.tilFagsystemDTO(),
                ytelsestype = ytelse.tilYtelseDTO(),
                behandlingUuid = behandlingId,
                referertFagsaksbehandling = eksternBehandlingId,
                behandlingstype = applicationProperties.toggles.defaultWhenDisabled(Toggles::revurdering) { Behandlingstype.TILBAKEKREVING },
                behandlingsstatus = behandlingstatus,
                behandlingsresultat = when (vedtaksresultat) {
                    Vedtaksresultat.FULL_TILBAKEBETALING -> Behandlingsresultatstype.FULL_TILBAKEBETALING
                    Vedtaksresultat.DELVIS_TILBAKEBETALING -> Behandlingsresultatstype.DELVIS_TILBAKEBETALING
                    Vedtaksresultat.INGEN_TILBAKEBETALING -> Behandlingsresultatstype.INGEN_TILBAKEBETALING
                    null -> Behandlingsresultatstype.IKKE_FASTSATT
                },
                behandlingErManueltOpprettet = applicationProperties.toggles.defaultWhenDisabled(Toggles::manuellOpprettelse) { false },
                venterPåBruker = venterPåBruker,
                venterPåØkonomi = tilstand == TilbakekrevingTilstand.AVVENTER_KRAVGRUNNLAG,
                ansvarligEnhet = ansvarligEnhet ?: "Ukjent",
                ansvarligSaksbehandler = ansvarligSaksbehandler,
                ansvarligBeslutter = ansvarligBeslutter,
                forrigeBehandling = forrigeBehandlingId,
                revurderingOpprettetÅrsak = applicationProperties.toggles.defaultWhenDisabled(Toggles::revurdering) { null },
                totalFeilutbetaltBeløp = totaltFeilutbetaltBeløp,
                totalFeilutbetaltPeriode = totalFeilutbetaltPeriode?.let { Periode(it.fom, it.tom) },
            ),
            logContext = SecureLog.Context.medBehandling(eksternFagsystemId, behandlingId.toString()),
        )
    }

    override fun behandlingEndret(
        behandlingId: UUID,
        vedtakGjelderId: String,
        eksternFagsakId: String,
        ytelse: Ytelse,
        eksternBehandlingId: String?,
        sakOpprettet: LocalDateTime,
        varselSendt: LocalDateTime?,
        behandlingsstatus: ForenkletBehandlingsstatus,
        totaltFeilutbetaltBeløp: BigDecimal,
        hentSaksbehandlingURL: (String) -> String,
        fullstendigPeriode: Datoperiode,
    ) {
        val logContext = SecureLog.Context.utenBehandling(eksternFagsakId)
        kafkaProducer.sendKafkaEvent(
            BehandlingEndretHendelse(
                eksternFagsakId = eksternFagsakId,
                hendelseOpprettet = LocalDateTime.now(),
                eksternBehandlingId = eksternBehandlingId,
                tilbakekreving = BehandlingEndretHendelse.Tilbakekreving(
                    behandlingId = behandlingId,
                    sakOpprettet = sakOpprettet,
                    varselSendt = varselSendt,
                    behandlingsstatus = behandlingsstatus,
                    totaltFeilutbetaltBeløp = totaltFeilutbetaltBeløp,
                    saksbehandlingURL = hentSaksbehandlingURL(applicationProperties.frontendUrl),
                    fullstendigPeriode = PeriodeDto(fullstendigPeriode.fom, fullstendigPeriode.tom),
                ),
            ),
            BehandlingEndretHendelse.METADATA,
            vedtakGjelderId,
            ytelse,
            logContext,
        )
    }

    override fun vedtakFattet(
        behandlingId: UUID,
        forrigeBehandlingId: UUID?,
        behandlingOpprettet: OffsetDateTime,
        eksternFagsystemId: String,
        eksternBehandlingId: String,
        ytelse: Ytelse,
        vedtakFattetTidspunkt: OffsetDateTime,
        ansvarligEnhet: String?,
        ansvarligSaksbehandler: String,
        ansvarligBeslutter: String,
        vurderteUtbetalinger: List<VurdertUtbetaling>,
    ) {
        val logContext = SecureLog.Context.medBehandling(eksternFagsystemId, behandlingId.toString())
        kafkaProducer.sendVedtaksdata(
            behandlingId = behandlingId,
            request = Vedtaksoppsummering(
                saksnummer = eksternFagsystemId,
                ytelsestype = ytelse.tilYtelseDTO(),
                fagsystem = ytelse.tilFagsystemDTO(),
                behandlingUuid = behandlingId,
                behandlingstype = applicationProperties.toggles.defaultWhenDisabled(Toggles::revurdering) { Behandlingstype.TILBAKEKREVING },
                erBehandlingManueltOpprettet = applicationProperties.toggles.defaultWhenDisabled(Toggles::manuellOpprettelse) { false },
                behandlingOpprettetTidspunkt = behandlingOpprettet,
                vedtakFattetTidspunkt = vedtakFattetTidspunkt,
                referertFagsaksbehandling = eksternBehandlingId,
                forrigeBehandling = forrigeBehandlingId,
                ansvarligSaksbehandler = ansvarligSaksbehandler,
                ansvarligBeslutter = ansvarligBeslutter,
                behandlendeEnhet = ansvarligEnhet ?: "Ukjent",
                perioder = vurderteUtbetalinger.map {
                    VedtakPeriode(
                        fom = it.periode.fom,
                        tom = it.periode.tom,
                        hendelsestype = Hendelsestype.ANNET.name,
                        hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST.name,
                        vilkårsresultat = when (it.vilkårsvurdering.forårsaketAvBruker) {
                            VurdertUtbetaling.ForårsaketAvBruker.FEILAKTIGE_OPPLYSNINGER -> UtvidetVilkårsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER
                            VurdertUtbetaling.ForårsaketAvBruker.MANGELFULLE_OPPLYSNINGER -> UtvidetVilkårsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER
                            VurdertUtbetaling.ForårsaketAvBruker.IKKE_FORÅRSAKET_AV_BRUKER -> UtvidetVilkårsresultat.FORSTO_BURDE_FORSTÅTT
                            VurdertUtbetaling.ForårsaketAvBruker.GOD_TRO -> UtvidetVilkårsresultat.GOD_TRO
                        },
                        feilutbetaltBeløp = it.beregning.feilutbetaltBeløp,
                        bruttoTilbakekrevingsbeløp = it.beregning.tilbakekrevesBeløp,
                        rentebeløp = it.beregning.rentebeløp,
                        harBruktSjetteLedd = it.vilkårsvurdering.beløpUnnlatesUnder4Rettsgebyr == VurdertUtbetaling.JaNeiVurdering.Ja,
                        aktsomhet = it.vilkårsvurdering.aktsomhetEtterUtbetaling ?: it.vilkårsvurdering.aktsomhetFørUtbetaling,
                        særligeGrunner = it.vilkårsvurdering.særligeGrunner?.let { særligeGrunner ->
                            SærligeGrunner(
                                erSærligeGrunnerTilReduksjon = særligeGrunner.beløpReduseres == VurdertUtbetaling.JaNeiVurdering.Ja,
                                særligeGrunner = særligeGrunner.grunner.map { it.type }.toList(),
                            )
                        },
                    )
                },
            ),
            logContext = logContext,
        )
    }
}
