package no.nav.tilbakekreving

import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.integration.pdl.PdlClient
import no.nav.familie.tilbake.integration.pdl.internal.PdlKjønnType
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstidspunkt
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstype
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.familie.tilbake.sikkerhet.ValideringContext
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeslåVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.api.v2.fagsystem.behov.FagsysteminfoBehovHendelse
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.SærligGrunn
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonSærligeGrunner
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Skyldgrad
import no.nav.tilbakekreving.behandlingslogg.Behandlingslogg
import no.nav.tilbakekreving.behov.Behov
import no.nav.tilbakekreving.behov.BrukerinfoBehov
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.behov.IverksettelseBehov
import no.nav.tilbakekreving.behov.VarselbrevDistribusjonBehov
import no.nav.tilbakekreving.behov.VarselbrevJournalføringBehov
import no.nav.tilbakekreving.behov.VedtaksbrevDistribusjonBehov
import no.nav.tilbakekreving.behov.VedtaksbrevJournalføringBehov
import no.nav.tilbakekreving.bigquery.BigQueryService
import no.nav.tilbakekreving.brev.varselbrev.ForhåndsvarselService
import no.nav.tilbakekreving.brev.vedtaksbrev.NyVedtaksbrevService
import no.nav.tilbakekreving.config.FeatureService
import no.nav.tilbakekreving.endring.EndringObservatørService
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.hendelse.DistribusjonHendelse
import no.nav.tilbakekreving.hendelse.IverksettelseHendelse
import no.nav.tilbakekreving.hendelse.JournalføringHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevDistribueringHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevJournalføringHendelse
import no.nav.tilbakekreving.integrasjoner.dokdistfordeling.DokdistClient
import no.nav.tilbakekreving.kontrakter.bruker.Kjønn
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.frontend.models.BurdeForstaattDto
import no.nav.tilbakekreving.kontrakter.frontend.models.DelerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForaarsaketAvMottakerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForsettligDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForstoDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForstoEllerBurdeForstaattDto
import no.nav.tilbakekreving.kontrakter.frontend.models.GodTroDto
import no.nav.tilbakekreving.kontrakter.frontend.models.GrovtUaktsomtDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HeleDto
import no.nav.tilbakekreving.kontrakter.frontend.models.IkkeAktueltDto
import no.nav.tilbakekreving.kontrakter.frontend.models.IngentingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.JaSaerligeGrunnerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.LogginnslagDto
import no.nav.tilbakekreving.kontrakter.frontend.models.MomentDto
import no.nav.tilbakekreving.kontrakter.frontend.models.NeiSaerligeGrunnerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SkalIkkeUnnlatesDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SkalUnnlatesDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UaktsomtDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UnnlatelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VilkaarsvurderingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VilkaarsvurderingIkkeVurdertDto
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagBufferRepository
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
import no.nav.tilbakekreving.repository.TilbakekrevingRepository
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class TilbakekrevingService(
    private val pdlClient: PdlClient,
    private val iverksettService: IverksettService,
    private val tilbakekrevingRepository: TilbakekrevingRepository,
    private val bigQueryService: BigQueryService,
    private val endringObservatørService: EndringObservatørService,
    private val kafkaProducer: KafkaProducer,
    private val kravgrunnlagBufferRepository: KravgrunnlagBufferRepository,
    private val dokdistService: DokdistClient,
    private val featureService: FeatureService,
    private val forhåndsvarselService: ForhåndsvarselService,
    private val vedtaksbrevService: NyVedtaksbrevService,
    private val tilgangskontrollService: TilgangskontrollService,
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

        val logContext = SecureLog.Context.fra(tilbakekreving)
        if (validerScope) {
            kravgrunnlagBufferRepository.validerKravgrunnlagInnenforScope(tilbakekreving.eksternFagsak.eksternId, logContext.behandlingId)
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
        callback: (Tilbakekreving, (Behandler) -> SideeffektContext) -> T,
    ): T? {
        var result: T? = null
        val observatør = Observatør()
        lateinit var logContext: SecureLog.Context
        tilbakekrevingRepository.hentOgLagreResultat(filter) { it, behandlingslogg ->
            kravgrunnlagBufferRepository.validerKravgrunnlagInnenforScope(it.eksternFagsak.eksternId, it.behandlingHistorikkEntities.lastOrNull()?.id?.toString())
            val tilbakekreving = it.fraEntity()
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
                    håndterBehov(tilbakekreving, systemContext, observatør.nesteBehov(), SecureLog.Context.fra(tilbakekreving))
                } catch (e: Exception) {
                    logger.medContext(logContext) {
                        warn("Feilet under håndtering av behov", e)
                    }
                    tilbakekreving.oppdaterPåminnelsestidspunkt(systemContext.klokke)
                    break
                }
            }
            tilbakekreving.tilEntity()
        }
    }

    private fun håndterBehov(
        tilbakekreving: Tilbakekreving,
        sideeffektContext: SideeffektContext,
        behov: Behov,
        logContext: SecureLog.Context,
    ) {
        when (behov) {
            is BrukerinfoBehov -> {
                val personinfo = pdlClient.hentPersoninfo(
                    ident = behov.ident,
                    fagsystem = behov.ytelse.tilFagsystemDTO(),
                    logContext = SecureLog.Context.fra(tilbakekreving),
                )
                tilbakekreving.håndter(
                    BrukerinfoHendelse(
                        ident = personinfo.ident,
                        fødselsdato = personinfo.fødselsdato,
                        navn = personinfo.navn,
                        kjønn = when (personinfo.kjønn) {
                            PdlKjønnType.MANN -> Kjønn.MANN
                            PdlKjønnType.KVINNE -> Kjønn.KVINNE
                            PdlKjønnType.UKJENT -> Kjønn.UKJENT
                        },
                        dødsdato = personinfo.dødsdato,
                    ),
                    sideeffektContext,
                )
            }

            is VarselbrevJournalføringBehov -> {
                val journalpostResponse = forhåndsvarselService.journalførVarselbrev(
                    varselbrevBehov = behov,
                    logContext = logContext,
                    features = featureService.modellFeatures,
                )
                if (journalpostResponse.journalpostId == null) {
                    throw Feil(
                        message = "journalførin av varselbrev til behandlingId ${behov.behandlingId} misslykket med denne meldingen: ${journalpostResponse.melding}",
                        frontendFeilmelding = "journalførin av varselbrev til behandlingId ${behov.behandlingId} misslykket med denne meldingen: ${journalpostResponse.melding}",
                        logContext = SecureLog.Context.fra(tilbakekreving),
                    )
                }

                if (journalpostResponse.dokumenter.isNullOrEmpty()) {
                    throw Feil(
                        message = "Response fra journalføring av varselbrev til behandlingId ${behov.behandlingId} mangler dokumenter. Dokumenter er enten null eller tom. ${journalpostResponse.melding}",
                        frontendFeilmelding = "Response fra journalføring av varselbrev til behandlingId ${behov.behandlingId} mangler dokumenter. Dokumenter er enten null eller tom. ${journalpostResponse.melding}",
                        logContext = SecureLog.Context.fra(tilbakekreving),
                    )
                }

                tilbakekreving.håndter(
                    VarselbrevJournalføringHendelse(
                        varselbrevId = behov.info.id,
                        journalpostId = journalpostResponse.journalpostId,
                        dokumentInfoId = journalpostResponse.dokumenter[0].dokumentInfoId!!,
                    ),
                    sideeffektContext,
                )
            }

            is VarselbrevDistribusjonBehov -> {
                dokdistService.brevTilUtsending(
                    behandlingId = behov.behandlingId,
                    journalpostId = behov.journalpostId,
                    fagsystem = behov.ytelse.tilFagsystemDTO(),
                    distribusjonstype = Distribusjonstype.VIKTIG,
                    distribusjonstidspunkt = Distribusjonstidspunkt.KJERNETID,
                    adresse = null,
                    logContext = logContext,
                )
                tilbakekreving.håndter(
                    VarselbrevDistribueringHendelse(
                        brevId = behov.brevId,
                        journalpostId = behov.journalpostId,
                        dokumentInfoId = behov.dokumentInfoId,
                    ),
                    sideeffektContext,
                )
            }

            is FagsysteminfoBehov -> {
                val logContext = SecureLog.Context.utenBehandling(behov.eksternFagsakId)
                kafkaProducer.sendKafkaEvent(
                    kafkamelding = FagsysteminfoBehovHendelse(
                        eksternFagsakId = behov.eksternFagsakId,
                        kravgrunnlagReferanse = behov.eksternBehandlingId,
                        hendelseOpprettet = LocalDateTime.now(),
                    ),
                    metadata = FagsysteminfoBehovHendelse.METADATA,
                    vedtakGjelderId = behov.vedtakGjelderId,
                    ytelse = behov.ytelse,
                    logContext = logContext,
                )
            }

            is IverksettelseBehov -> {
                val iverksattVedtak = iverksettService.iverksett(behov, logContext)

                tilbakekreving.håndter(
                    IverksettelseHendelse(
                        iverksattVedtakId = iverksattVedtak.id,
                        behandlingId = iverksattVedtak.behandlingId,
                        vedtakId = iverksattVedtak.vedtakId,
                    ),
                    sideeffektContext,
                )
            }

            is VedtaksbrevJournalføringBehov -> {
                val journalpost = vedtaksbrevService.journalførVedtaksbrev(behov)
                if (journalpost.journalpostId == null) {
                    throw Feil(
                        message = "journalføring av vedtaksbrev til behandlingId ${behov.behandlingId} misslykket med denne meldingen: ${journalpost.melding}",
                        frontendFeilmelding = "journalføring av vedtaksbrev til behandlingId ${behov.behandlingId} misslykket med denne meldingen: ${journalpost.melding}",
                        logContext = SecureLog.Context.fra(tilbakekreving),
                    )
                }
                if (journalpost.dokumenter.isNullOrEmpty()) {
                    throw Feil(
                        message = "Response fra journalføring av vedtaksbrev til behandlingId ${behov.behandlingId} mangler dokumenter. Dokumenter er enten null eller tom. ${journalpost.melding}",
                        frontendFeilmelding = "Response fra journalføring av vedtaksbrev til behandlingId ${behov.behandlingId} mangler dokumenter. Dokumenter er enten null eller tom. ${journalpost.melding}",
                        logContext = SecureLog.Context.fra(tilbakekreving),
                    )
                }

                tilbakekreving.håndter(
                    JournalføringHendelse(
                        brevId = behov.brevId,
                        behandlingId = behov.behandlingId,
                        journalpostId = journalpost.journalpostId,
                        fagsakId = behov.fagsakId,
                        dokumentInfoId = journalpost.dokumenter[0].dokumentInfoId!!,
                    ),
                    sideeffektContext,
                )
            }

            is VedtaksbrevDistribusjonBehov -> {
                vedtaksbrevService.distribuereVedtaksbrev(behov, logContext)
                tilbakekreving.håndter(
                    DistribusjonHendelse(
                        behandlingId = behov.behandlingId,
                        brevId = behov.brevId,
                        fagsakId = behov.fagsakId,
                        journalpostId = behov.journalpostId,
                        dokumentInfoId = behov.dokumentInfoId,
                    ),
                    sideeffektContext,
                )
            }
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
                            Foreldelsesvurderingstype.FORELDET -> Foreldelsesteg.Vurdering.Foreldet(periode.begrunnelse)
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

    fun mapTilForårsaketAvBruker(vilkaarsvurderingDto: VilkaarsvurderingDto, tilbakekreving: Tilbakekreving): ForårsaketAvBruker {
        val logContext = SecureLog.Context.fra(tilbakekreving)
        val valg = vilkaarsvurderingDto.valg
        val vurdering: ForårsaketAvBruker = when (valg) {
            is ForstoEllerBurdeForstaattDto -> {
                val forståelse = valg.forståelse
                when (forståelse) {
                    is ForstoDto -> NivåAvForståelse.Forstod(
                        begrunnelseMottakersForståelse = forståelse.begrunnelse,
                        begrunnelse = vilkaarsvurderingDto.begrunnelse,
                        kanUnnlates4XRettsgebyr = when (forståelse.unnlatelse) {
                            is IkkeAktueltDto, is SkalIkkeUnnlatesDto -> mapTilSkalIkkeUnnlatesEllerOver4xxRettsgebyr(forståelse.unnlatelse, logContext = logContext)
                            is SkalUnnlatesDto -> KanUnnlates4xRettsgebyr.Unnlates
                        },
                    )

                    is BurdeForstaattDto -> NivåAvForståelse.BurdeForstått(
                        grad = NivåAvForståelse.Grad.BURDE_FORSTÅTT,
                        begrunnelseMottakersForståelse = forståelse.begrunnelse,
                        kanUnnlates4XRettsgebyr = when (forståelse.unnlatelse) {
                            is IkkeAktueltDto, is SkalIkkeUnnlatesDto -> mapTilSkalIkkeUnnlatesEllerOver4xxRettsgebyr(forståelse.unnlatelse, logContext = logContext)
                            is SkalUnnlatesDto -> KanUnnlates4xRettsgebyr.Unnlates
                        },
                        begrunnelse = vilkaarsvurderingDto.begrunnelse,
                    )
                }
            }

            is ForaarsaketAvMottakerDto -> {
                val aktsomhet = valg.aktsomhet
                when (aktsomhet) {
                    is ForsettligDto -> Skyldgrad.Forsett(
                        begrunnelse = vilkaarsvurderingDto.begrunnelse,
                        begrunnelseAktsomhet = aktsomhet.begrunnelse,
                        feilaktigeEllerMangelfulleOpplysninger = TODO(),
                    )

                    is GrovtUaktsomtDto -> Skyldgrad.GrovUaktsomhet(
                        begrunnelse = vilkaarsvurderingDto.begrunnelse,
                        begrunnelseAktsomhet = aktsomhet.begrunnelse,
                        reduksjonSærligeGrunner = when (aktsomhet.erDetSærligeGrunner) {
                            is JaSaerligeGrunnerDto -> mapTilSkalReduseres(
                                begrunnelse = (aktsomhet.erDetSærligeGrunner as JaSaerligeGrunnerDto).begrunnelse,
                                momenter = (aktsomhet.erDetSærligeGrunner as JaSaerligeGrunnerDto).særligeGrunnerFor,
                                annetBegrunnelse = (aktsomhet.erDetSærligeGrunner as JaSaerligeGrunnerDto).annetBegrunnelse,
                                skalReduseres = ReduksjonSærligeGrunner.SkalReduseres.Ja((aktsomhet.erDetSærligeGrunner as JaSaerligeGrunnerDto).prosentReduksjon),
                                logContext = logContext,
                            )

                            is NeiSaerligeGrunnerDto -> mapTilSkalReduseres(
                                begrunnelse = (aktsomhet.erDetSærligeGrunner as NeiSaerligeGrunnerDto).begrunnelse,
                                momenter = (aktsomhet.erDetSærligeGrunner as NeiSaerligeGrunnerDto).særligeGrunnerMot,
                                annetBegrunnelse = (aktsomhet.erDetSærligeGrunner as NeiSaerligeGrunnerDto).annetBegrunnelse,
                                skalReduseres = ReduksjonSærligeGrunner.SkalReduseres.Nei,
                                logContext = logContext,
                            )
                        },
                        feilaktigeEllerMangelfulleOpplysninger = TODO(),
                    )

                    is UaktsomtDto -> Skyldgrad.Uaktsomt(
                        begrunnelse = vilkaarsvurderingDto.begrunnelse,
                        begrunnelseAktsomhet = aktsomhet.begrunnelse,
                        kanUnnlates4XRettsgebyr = when (aktsomhet.unnlatelse) {
                            is IkkeAktueltDto, is SkalIkkeUnnlatesDto -> mapTilSkalIkkeUnnlatesEllerOver4xxRettsgebyr(aktsomhet.unnlatelse, logContext = logContext)
                            is SkalUnnlatesDto -> KanUnnlates4xRettsgebyr.Unnlates
                        },
                        feilaktigeEllerMangelfulleOpplysninger = TODO(),
                    )
                }
            }

            is GodTroDto -> NivåAvForståelse.GodTro(
                beløpIBehold = when (valg.beløpIBehold) {
                    is DelerDto -> TODO()
                    is HeleDto -> TODO()
                    is IngentingDto -> NivåAvForståelse.GodTro.BeløpIBehold.Nei
                },
                begrunnelseForGodTro = valg.begrunnelse,
                begrunnelse = vilkaarsvurderingDto.begrunnelse,
            )

            is VilkaarsvurderingIkkeVurdertDto -> {
                throw Feil("Feil vilkaarsvurderingDto: $vilkaarsvurderingDto!", logContext = logContext)
            }
        }
        return vurdering
    }

    private fun mapTilSkalIkkeUnnlatesEllerOver4xxRettsgebyr(unnlatelseDto: UnnlatelseDto, logContext: SecureLog.Context): KanUnnlates4xRettsgebyr {
        val særligeGrunnerDto = when (unnlatelseDto) {
            is IkkeAktueltDto -> unnlatelseDto.erDetSærligeGrunner
            is SkalIkkeUnnlatesDto -> unnlatelseDto.erDetSærligeGrunner
            is SkalUnnlatesDto -> throw Feil("Feil unnlatelseDto: $unnlatelseDto!", logContext = logContext)
        }

        val reduksjon = when (særligeGrunnerDto) {
            is JaSaerligeGrunnerDto -> mapTilSkalReduseres(
                begrunnelse = særligeGrunnerDto.begrunnelse,
                momenter = særligeGrunnerDto.særligeGrunnerFor,
                annetBegrunnelse = særligeGrunnerDto.annetBegrunnelse,
                skalReduseres = ReduksjonSærligeGrunner.SkalReduseres.Ja(særligeGrunnerDto.prosentReduksjon),
                logContext = logContext,
            )

            is NeiSaerligeGrunnerDto -> mapTilSkalReduseres(
                begrunnelse = særligeGrunnerDto.begrunnelse,
                momenter = særligeGrunnerDto.særligeGrunnerMot,
                annetBegrunnelse = særligeGrunnerDto.annetBegrunnelse,
                skalReduseres = ReduksjonSærligeGrunner.SkalReduseres.Nei,
                logContext = logContext,
            )
        }

        return when (unnlatelseDto) {
            is IkkeAktueltDto -> KanUnnlates4xRettsgebyr.ErOver4xRettsgebyr(reduksjon)
            is SkalIkkeUnnlatesDto -> KanUnnlates4xRettsgebyr.SkalIkkeUnnlates(reduksjon)
            is SkalUnnlatesDto -> throw Feil("Feil unnlatelseDto: $unnlatelseDto!", logContext = logContext)
        }
    }

    private fun mapTilSkalReduseres(
        begrunnelse: String,
        momenter: List<MomentDto>,
        annetBegrunnelse: String?,
        skalReduseres: ReduksjonSærligeGrunner.SkalReduseres,
        logContext: SecureLog.Context,
    ): ReduksjonSærligeGrunner =
        ReduksjonSærligeGrunner(
            begrunnelse = begrunnelse,
            grunner = momenter.map { mapSærligGrunn(it.moment, annetBegrunnelse, logContext) }.toSet(),
            skalReduseres = skalReduseres,
        )

    private fun mapSærligGrunn(moment: String, annetBegrunnelse: String?, logContext: SecureLog.Context): SærligGrunn =
        when (moment) {
            SærligGrunnType.GRAD_AV_UAKTSOMHET.name -> SærligGrunn.GradAvUaktsomhet
            SærligGrunnType.HELT_ELLER_DELVIS_NAVS_FEIL.name -> SærligGrunn.HeltEllerDelvisNavsFeil
            SærligGrunnType.STØRRELSE_BELØP.name -> SærligGrunn.StørrelseBeløp
            SærligGrunnType.TID_FRA_UTBETALING.name -> SærligGrunn.TidFraUtbetaling
            SærligGrunnType.ANNET.name -> SærligGrunn.Annet(annetBegrunnelse!!)
            else -> throw Feil("Ukjent særlig grunn: $moment", logContext = logContext)
        }
}
