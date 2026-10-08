package no.nav.tilbakekreving

import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.familie.tilbake.sikkerhet.ValideringContext
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeslåVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandlingslogg.Behandlingslogg
import no.nav.tilbakekreving.bigquery.BigQueryService
import no.nav.tilbakekreving.config.FeatureService
import no.nav.tilbakekreving.endring.EndringObservatørService
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.integrasjoner.feil.UnexpectedResponseException
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.frontend.models.LogginnslagDto
import no.nav.tilbakekreving.kravgrunnlag.StatusmeldingBufferRepository
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
import no.nav.tilbakekreving.repository.TilbakekrevingRepository
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TilbakekrevingService(
    private val tilbakekrevingRepository: TilbakekrevingRepository,
    private val bigQueryService: BigQueryService,
    private val endringObservatørService: EndringObservatørService,
    private val statusmeldingBufferRepository: StatusmeldingBufferRepository,
    private val featureService: FeatureService,
    private val tilgangskontrollService: TilgangskontrollService,
    private val behovMediator: BehovMediator,
) {
    private val logger = TracedLogger.getLogger<TilbakekrevingService>()

    private fun sideeffektContext(behandler: Behandler, observatør: Observatør, behandlingslogg: Behandlingslogg) =
        SideeffektContext(
            behandler = behandler,
            endringObservatør = endringObservatørService,
            behovObservatør = observatør,
            bigQueryService = bigQueryService,
            features = featureService.modellFeatures,
            klokke = SystemKlokke,
            behandlingslogg = behandlingslogg,
        )

    fun lesecontext(behandler: Behandler = ContextService.hentBehandler(SecureLog.Context.tom())) = LesContext(
        behandler = behandler,
        features = featureService.modellFeatures,
        klokke = SystemKlokke,
    )

    fun opprettTilbakekreving(
        opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
        håndter: (Tilbakekreving, SideeffektContext) -> Unit,
    ) {
        val observatør = Observatør()
        val behandlingslogg = Behandlingslogg(mutableListOf())
        val systemContext = sideeffektContext(Behandler.Vedtaksløsning, observatør, behandlingslogg)

        val tilbakekreving = Tilbakekreving.opprett(
            id = tilbakekrevingRepository.nesteId(),
            opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse,
            sideeffektContext = systemContext,
        )

        håndter(tilbakekreving, systemContext)
        val tilbakekrevingId = tilbakekrevingRepository.opprett(tilbakekreving.tilEntity(), behandlingslogg)

        val logContext = SecureLog.Context.fra(tilbakekreving)

        logger.medContext(logContext) { info("Lagrer tilbakekreving") }

        utførSideeffekter(TilbakekrevingFilter.tilbakekreving(tilbakekrevingId), observatør, logContext)

        logger.medContext(logContext) { info("Tilbakekreving ferdig opprettet") }
    }

    fun lesTilbakekreving(
        filter: TilbakekrevingFilter,
        valideringContext: ValideringContext,
        validerScope: Boolean = true,
    ): Tilbakekreving? {
        val tilbakekreving = hentTilbakekreving(filter, validerScope) ?: return null
        val behandler = ContextService.hentBehandler(filter.logContext())
        tilgangskontrollService.validerTilgangTilbakekreving(tilbakekreving, valideringContext, behandler)
        return tilbakekreving
    }

    fun hentTilbakekreving(filter: TilbakekrevingFilter, validerScope: Boolean = true): Tilbakekreving? {
        val tilbakekreving = tilbakekrevingRepository.hentTilbakekreving(filter)?.fraEntity() ?: return null

        if (validerScope) {
            if (statusmeldingBufferRepository.erAnnullert(tilbakekreving.eksternFagsak.eksternId)) {
                throw ModellFeil.UtenforScopeException(UtenforScope.KravgrunnlagBortfalt, tilbakekreving.sporingsinformasjon())
            }
            tilbakekreving.validerInnenforScope(featureService.modellFeatures)
        }
        return tilbakekreving
    }

    fun <T> endreTilbakekreving(
        filter: TilbakekrevingFilter,
        valideringContext: ValideringContext,
        callback: (Tilbakekreving, SideeffektContext) -> T,
    ): T? {
        return hentOgLagreTilbakekreving(filter) { tilbakekreving, sideeffektContext ->
            val behandler = ContextService.hentBehandler(filter.logContext())
            tilgangskontrollService.validerTilgangTilbakekreving(tilbakekreving, valideringContext, behandler)
            callback(tilbakekreving, sideeffektContext(behandler))
        }
    }

    fun <T> hentOgLagreTilbakekreving(
        filter: TilbakekrevingFilter,
        validerScope: Boolean = true,
        callback: (Tilbakekreving, (Behandler) -> SideeffektContext) -> T,
    ): T? {
        var result: T? = null
        val observatør = Observatør()
        lateinit var logContext: SecureLog.Context
        tilbakekrevingRepository.hentOgLagreResultat(filter) { it, behandlingslogg ->
            val tilbakekreving = it.fraEntity()
            if (validerScope) {
                tilbakekreving.validerInnenforScope(featureService.modellFeatures)
            }
            logContext = SecureLog.Context.fra(tilbakekreving)
            result = callback(tilbakekreving) { behandler ->
                sideeffektContext(behandler, observatør, behandlingslogg)
            }

            tilbakekreving.tilEntity()
        }

        if (result == null) {
            return null
        }

        utførSideeffekter(filter, observatør, logContext)

        return result
    }

    private fun utførSideeffekter(
        strategy: TilbakekrevingFilter,
        observatør: Observatør,
        logContext: SecureLog.Context,
    ) {
        tilbakekrevingRepository.hentOgLagreResultat(strategy) { it, behandlingslogg ->
            val systemContext = sideeffektContext(Behandler.Vedtaksløsning, observatør, behandlingslogg)
            val tilbakekreving = it.fraEntity()
            while (observatør.harUbesvarteBehov()) {
                try {
                    behovMediator.håndterBehov(tilbakekreving, systemContext, observatør.nesteBehov(), SecureLog.Context.fra(tilbakekreving))
                } catch (e: Exception) {
                    logger.medContext(logContext) {
                        warn("Feilet under håndtering av behov", e)
                    }
                    if (e is UnexpectedResponseException) {
                        SecureLog.medContext(logContext) {
                            warn("Feilet under håndtering av behov, status: {}, response: {}", e.statusCode, e.response, e)
                        }
                    }
                    tilbakekreving.oppdaterPåminnelsestidspunkt(systemContext.klokke)
                    break
                }
            }
            tilbakekreving.tilEntity()
        }
    }

    fun utførSteg(
        tilbakekreving: Tilbakekreving,
        context: SideeffektContext,
        behandlingId: UUID,
        dto: BehandlingsstegDto,
    ) {
        val logContext = SecureLog.Context.fra(tilbakekreving)
        tilbakekreving.gjørSaksbehandling(behandlingId, context) {
            when (dto) {
                is BehandlingsstegForeldelseDto -> dto.foreldetPerioder.forEach { periode ->
                    vurderForeldelse(
                        periode.periode,
                        when (periode.foreldelsesvurderingstype) {
                            Foreldelsesvurderingstype.IKKE_VURDERT -> Foreldelsesteg.Vurdering.IkkeVurdert
                            Foreldelsesvurderingstype.FORELDET -> Foreldelsesteg.Vurdering.Foreldet(periode.begrunnelse, periode.foreldelsesfrist)
                            Foreldelsesvurderingstype.IKKE_FORELDET -> Foreldelsesteg.Vurdering.IkkeForeldet(periode.begrunnelse)
                            Foreldelsesvurderingstype.AUTOMATISK_VURDERT_IKKE_FORELDET -> Foreldelsesteg.Vurdering.AutomatiskIkkeForeldet(periode.begrunnelse)
                            Foreldelsesvurderingstype.TILLEGGSFRIST -> Foreldelsesteg.Vurdering.Tilleggsfrist(periode.foreldelsesfrist!!, periode.oppdagelsesdato!!)
                        },
                    )
                }

                is BehandlingsstegVilkårsvurderingDto -> dto.vilkårsvurderingsperioder.forEach { periode ->
                    vurderVilkår(periode.periode, VilkårsvurderingMapperV2.tilVurdering(periode))
                }

                is BehandlingsstegForeslåVedtaksstegDto -> foreslåVedtak()

                is BehandlingsstegFatteVedtaksstegDto -> fatteVedtak(
                    vurderinger = dto.totrinnsvurderinger.map { stegVurdering ->
                        stegVurdering.behandlingssteg to when (stegVurdering.godkjent) {
                            true -> FatteVedtakSteg.Vurdering.Godkjent
                            else -> FatteVedtakSteg.Vurdering.Underkjent(stegVurdering.begrunnelse!!)
                        }
                    },
                )

                else -> throw Feil("Vurdering for ${dto.getSteg()} er ikke implementert i ny modell enda.", logContext = logContext)
            }
        }
    }

    fun hentHistorikk(tilbakekrevingId: String): List<LogginnslagDto> {
        return tilbakekrevingRepository.hentBehandlingslogg(tilbakekrevingId).tilFrontend()
    }
}
