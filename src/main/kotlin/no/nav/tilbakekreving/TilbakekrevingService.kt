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
import no.nav.tilbakekreving.kontrakter.frontend.models.LogginnslagDto
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
    ): Tilbakekreving? {
        val tilbakekreving = hentTilbakekreving(filter) ?: return null
        val behandler = ContextService.hentBehandler(filter.logContext())
        tilgangskontrollService.validerTilgangTilbakekreving(tilbakekreving, valideringContext, behandler)
        return tilbakekreving
    }

    fun hentTilbakekreving(filter: TilbakekrevingFilter): Tilbakekreving? {
        val tilbakekreving = tilbakekrevingRepository.hentTilbakekreving(filter)?.fraEntity() ?: return null

        val logContext = SecureLog.Context.fra(tilbakekreving)
        kravgrunnlagBufferRepository.validerKravgrunnlagInnenforScope(tilbakekreving.eksternFagsak.eksternId, logContext.behandlingId)
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
                    håndter(
                        context,
                        periode.periode,
                        when (periode.foreldelsesvurderingstype) {
                            Foreldelsesvurderingstype.IKKE_VURDERT -> Foreldelsesteg.Vurdering.IkkeVurdert
                            Foreldelsesvurderingstype.FORELDET -> Foreldelsesteg.Vurdering.Foreldet(periode.begrunnelse)
                            Foreldelsesvurderingstype.IKKE_FORELDET -> Foreldelsesteg.Vurdering.IkkeForeldet(periode.begrunnelse)
                            Foreldelsesvurderingstype.TILLEGGSFRIST -> Foreldelsesteg.Vurdering.Tilleggsfrist(periode.foreldelsesfrist!!, periode.oppdagelsesdato!!)
                        },
                    )
                }

                is BehandlingsstegVilkårsvurderingDto -> dto.vilkårsvurderingsperioder.forEach { periode ->
                    håndter(context, periode.periode, VilkårsvurderingMapperV2.tilVurdering(periode))
                }

                is BehandlingsstegForeslåVedtaksstegDto -> håndterForeslåVedtak(context)
                is BehandlingsstegFatteVedtaksstegDto -> håndter(
                    sideeffektContext = context,
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
