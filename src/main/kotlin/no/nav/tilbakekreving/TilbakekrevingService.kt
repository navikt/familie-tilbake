package no.nav.tilbakekreving

import no.nav.familie.tilbake.api.forvaltning.Behandlingsinfo
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.integration.pdl.PdlClient
import no.nav.familie.tilbake.integration.pdl.internal.PdlKjønnType
import no.nav.familie.tilbake.kontrakter.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFaktaDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeslåVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.ManuellBrevmottakerRequestDto
import no.nav.tilbakekreving.api.v2.fagsystem.behov.FagsysteminfoBehovHendelse
import no.nav.tilbakekreving.behandling.saksbehandling.Faktasteg
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.RegistrertBrevmottaker
import no.nav.tilbakekreving.behov.Behov
import no.nav.tilbakekreving.behov.BrukerinfoBehov
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.behov.IverksettelseBehov
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.bigquery.BigQueryService
import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.endring.EndringObservatørService
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.hendelse.IverksettelseHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.kontrakter.brev.MottakerType
import no.nav.tilbakekreving.kontrakter.bruker.Kjønn
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class TilbakekrevingService(
    private val applicationProperties: ApplicationProperties,
    private val pdlClient: PdlClient,
    private val iverksettService: IverksettService,
    private val tilbakekrevingRepository: TilbakekrevingRepository,
    private val bigQueryService: BigQueryService,
    private val endringObservatørService: EndringObservatørService,
    private val kafkaProducer: KafkaProducer,
) {
    private val aktør = Aktør.Person(ident = "20046912345")
    private val logger = TracedLogger.getLogger<TilbakekrevingService>()

    fun opprettTilbakekreving(
        opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
        håndter: (Tilbakekreving) -> Unit,
    ) {
        val observatør = Observatør()
        lateinit var logContext: SecureLog.Context

        val tilbakekrevingId = tilbakekrevingRepository.opprett(Tilbakekreving.opprett(tilbakekrevingRepository.nesteId(), observatør, opprettTilbakekrevingHendelse, bigQueryService, endringObservatørService).tilEntity())

        hentOgLagreTilbakekreving(tilbakekrevingId) { tilbakekreving ->
            håndter(tilbakekreving)

            logContext = SecureLog.Context.fra(tilbakekreving)
            logger.medContext(logContext) { info("Lagrer tilbakekreving") }
            logger.medContext(logContext) {
                info("URL til behandling er: {}", tilbakekreving.hentTilbakekrevingUrl(applicationProperties.frontendUrl))
            }

            tilbakekreving.tilEntity()
        }

        logger.medContext(logContext) { info("Tilbakekreving ferdig opprettet") }
    }

    fun hentTilbakekreving(
        fagsystem: FagsystemDTO,
        eksternFagsakId: String,
    ): Tilbakekreving? {
        if (!applicationProperties.toggles.nyModellEnabled) return null

        val tilbakekrevinger = tilbakekrevingRepository.hentAlleTilbakekrevinger()?.map { it.fraEntity(Observatør(), bigQueryService, endringObservatørService) }
        return tilbakekrevinger?.firstOrNull { it.tilFrontendDto().fagsystem == fagsystem && it.tilFrontendDto().eksternFagsakId == eksternFagsakId }
    }

    fun hentTilbakekreving(behandlingId: UUID): Tilbakekreving? {
        if (!applicationProperties.toggles.nyModellEnabled) return null

        return tilbakekrevingRepository.hentTilbakekreving(behandlingId)?.fraEntity(Observatør(), bigQueryService, endringObservatørService)
    }

    fun <T : Any> hentOgLagreTilbakekreving(
        tilbakekrevingId: String,
        callback: (Tilbakekreving) -> T,
    ): T {
        lateinit var result: T
        val observatør = Observatør()
        tilbakekrevingRepository.hentOgLagreResultat(tilbakekrevingId) {
            val tilbakekreving = it.fraEntity(observatør, bigQueryService, endringObservatørService)
            val logContext = SecureLog.Context.fra(tilbakekreving)
            result = callback(tilbakekreving)

            while (observatør.harUbesvarteBehov()) {
                try {
                    håndterBehov(tilbakekreving, observatør.nesteBehov(), SecureLog.Context.fra(tilbakekreving))
                } catch (e: Exception) {
                    logger.medContext(logContext) {
                        warn("Feilet under håndtering av behov", e)
                    }
                    break
                }
            }

            tilbakekreving.tilEntity()
        }

        return result
    }

    fun <T : Any> hentTilbakekreving(
        fagsystem: FagsystemDTO,
        eksternFagsakId: String,
        håndter: (Tilbakekreving) -> T,
    ): T? {
        return tilbakekrevingRepository.hentAlleTilbakekrevinger()
            ?.asSequence()
            ?.map {
                it.fraEntity(Observatør(), bigQueryService, endringObservatørService)
            }
            ?.firstOrNull { tilbakekreving -> tilbakekreving.tilFrontendDto().fagsystem == fagsystem && tilbakekreving.tilFrontendDto().eksternFagsakId == eksternFagsakId }
            ?.let { tilbakekreving ->
                hentOgLagreTilbakekreving(tilbakekreving.id, håndter)
            }
    }

    fun <T : Any> hentTilbakekreving(
        behandlingId: UUID,
        håndter: (Tilbakekreving) -> T,
    ): T? {
        return tilbakekrevingRepository.hentTilbakekreving(behandlingId)
            ?.fraEntity(Observatør(), bigQueryService, endringObservatørService)
            ?.let {
                hentOgLagreTilbakekreving(it.id, håndter)
            }
    }

    private fun håndterBehov(
        tilbakekreving: Tilbakekreving,
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
                )
            }

            is VarselbrevBehov -> {
                tilbakekreving.håndter(
                    VarselbrevSendtHendelse(
                        Varselbrev.opprett(varsletBeløp = 2000L),
                    ),
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
                        vedtakId = iverksattVedtak.vedtakId,
                    ),
                )
            }
        }
    }

    fun utførSteg(
        behandler: Behandler,
        tilbakekrevingId: String,
        behandlingsstegDto: BehandlingsstegDto,
        logContext: SecureLog.Context,
    ) {
        return hentOgLagreTilbakekreving(tilbakekrevingId) { tilbakekreving ->
            when (behandlingsstegDto) {
                is BehandlingsstegForeldelseDto -> behandleForeldelse(tilbakekreving, behandlingsstegDto, behandler)
                is BehandlingsstegVilkårsvurderingDto -> behandleVilkårsvurdering(tilbakekreving, behandlingsstegDto, behandler)
                is BehandlingsstegFaktaDto -> behandleFakta(tilbakekreving, behandlingsstegDto, behandler)
                is BehandlingsstegForeslåVedtaksstegDto -> behandleForeslåVedtak(tilbakekreving, behandlingsstegDto, behandler)
                is BehandlingsstegFatteVedtaksstegDto -> behandleFatteVedtak(tilbakekreving, behandlingsstegDto, behandler)
                else -> throw Feil("Vurdering for ${behandlingsstegDto.getSteg()} er ikke implementert i ny modell enda.", logContext = logContext)
            }
        }
    }

    private fun behandleFakta(
        tilbakekreving: Tilbakekreving,
        fakta: BehandlingsstegFaktaDto,
        behandler: Behandler,
    ) {
        tilbakekreving.håndter(
            behandler,
            vurdering = Faktasteg.Vurdering(
                perioder = fakta.feilutbetaltePerioder.map {
                    Faktasteg.FaktaPeriode(
                        periode = it.periode,
                        rettsligGrunnlag = it.hendelsestype,
                        rettsligGrunnlagUnderkategori = it.hendelsesundertype,
                    )
                },
                årsakTilFeilutbetaling = fakta.begrunnelse,
                uttalelse = when (fakta.vurderingAvBrukersUttalelse?.harBrukerUttaltSeg) {
                    HarBrukerUttaltSeg.JA -> Faktasteg.Uttalelse.Ja(fakta.vurderingAvBrukersUttalelse!!.beskrivelse!!)
                    HarBrukerUttaltSeg.NEI -> Faktasteg.Uttalelse.Nei
                    HarBrukerUttaltSeg.IKKE_AKTUELT -> Faktasteg.Uttalelse.IkkeAktuelt
                    HarBrukerUttaltSeg.IKKE_VURDERT, null -> Faktasteg.Uttalelse.IkkeVurdert
                },
            ),
        )
    }

    private fun behandleVilkårsvurdering(
        tilbakekreving: Tilbakekreving,
        vurdering: BehandlingsstegVilkårsvurderingDto,
        behandler: Behandler,
    ) {
        vurdering.vilkårsvurderingsperioder.forEach { periode ->
            tilbakekreving.håndter(
                behandler,
                periode.periode,
                VilkårsvurderingMapperV2.tilVurdering(periode),
            )
        }
    }

    private fun behandleForeldelse(
        tilbakekreving: Tilbakekreving,
        vurdering: BehandlingsstegForeldelseDto,
        behandler: Behandler,
    ) {
        vurdering.foreldetPerioder.forEach { periode ->
            tilbakekreving.håndter(
                behandler,
                periode.periode,
                when (periode.foreldelsesvurderingstype) {
                    Foreldelsesvurderingstype.IKKE_VURDERT -> Foreldelsesteg.Vurdering.IkkeVurdert
                    Foreldelsesvurderingstype.FORELDET -> Foreldelsesteg.Vurdering.Foreldet(periode.begrunnelse)
                    Foreldelsesvurderingstype.IKKE_FORELDET -> Foreldelsesteg.Vurdering.IkkeForeldet(periode.begrunnelse)
                    Foreldelsesvurderingstype.TILLEGGSFRIST -> Foreldelsesteg.Vurdering.Tilleggsfrist(periode.foreldelsesfrist!!, periode.oppdagelsesdato!!)
                },
            )
        }
    }

    private fun behandleForeslåVedtak(
        tilbakekreving: Tilbakekreving,
        vurdering: BehandlingsstegForeslåVedtaksstegDto,
        behandler: Behandler,
    ) {
        tilbakekreving.håndter(
            behandler,
            ForeslåVedtakSteg.Vurdering.ForeslåVedtak(
                vurdering.fritekstavsnitt.oppsummeringstekst,
                vurdering.fritekstavsnitt.perioderMedTekst.map {
                    ForeslåVedtakSteg.Vurdering.ForeslåVedtak.PeriodeMedTekst(
                        periode = it.periode,
                        faktaAvsnitt = it.faktaAvsnitt,
                        foreldelseAvsnitt = it.foreldelseAvsnitt,
                        vilkårAvsnitt = it.vilkårAvsnitt,
                        særligeGrunnerAvsnitt = it.særligeGrunnerAvsnitt,
                        særligeGrunnerAnnetAvsnitt = it.særligeGrunnerAnnetAvsnitt,
                    )
                },
            ),
        )
    }

    private fun behandleFatteVedtak(
        tilbakekreving: Tilbakekreving,
        vurdering: BehandlingsstegFatteVedtaksstegDto,
        beslutter: Behandler,
    ) {
        tilbakekreving.håndter(
            beslutter = beslutter,
            vurderinger = vurdering.totrinnsvurderinger.map { stegVurdering ->
                stegVurdering.behandlingssteg to when (stegVurdering.godkjent) {
                    true -> FatteVedtakSteg.Vurdering.Godkjent
                    else -> FatteVedtakSteg.Vurdering.Underkjent(stegVurdering.begrunnelse!!)
                }
            },
        )
    }

    fun behandleBrevmottaker(
        behandler: Behandler,
        tilbakekrevingId: String,
        brevmottakerDto: ManuellBrevmottakerRequestDto,
        id: UUID,
    ) {
        hentOgLagreTilbakekreving(tilbakekrevingId) { tilbakekreving ->
            when (brevmottakerDto.type) {
                MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE -> {
                    tilbakekreving.håndter(
                        behandler,
                        RegistrertBrevmottaker.UtenlandskAdresseMottaker(
                            id = id,
                            navn = brevmottakerDto.navn,
                            manuellAdresseInfo = brevmottakerDto.manuellAdresseInfo,
                        ),
                    )
                }

                MottakerType.FULLMEKTIG -> {
                    tilbakekreving.håndter(
                        behandler,
                        RegistrertBrevmottaker.FullmektigMottaker(
                            id = id,
                            navn = brevmottakerDto.navn,
                            organisasjonsnummer = brevmottakerDto.organisasjonsnummer,
                            personIdent = brevmottakerDto.personIdent,
                            manuellAdresseInfo = brevmottakerDto.manuellAdresseInfo,
                        ),
                    )
                }

                MottakerType.VERGE -> {
                    tilbakekreving.håndter(
                        behandler,
                        RegistrertBrevmottaker.VergeMottaker(
                            id = id,
                            navn = brevmottakerDto.navn,
                            personIdent = brevmottakerDto.personIdent,
                            manuellAdresseInfo = brevmottakerDto.manuellAdresseInfo,
                        ),
                    )
                }

                MottakerType.DØDSBO -> {
                    tilbakekreving.håndter(
                        behandler,
                        RegistrertBrevmottaker.DødsboMottaker(
                            id = id,
                            navn = brevmottakerDto.navn,
                            manuellAdresseInfo = brevmottakerDto.manuellAdresseInfo,
                        ),
                    )
                }

                else -> throw IllegalArgumentException("Default eller ugydlig mottaker type ${brevmottakerDto.type}")
            }
        }
    }

    fun settPåVent(tilbakekrevingId: String, venteårsak: Venteårsak, tidsfrist: LocalDate, begrunnelse: String?) {
        hentOgLagreTilbakekreving(tilbakekrevingId) { tilbakekreving ->
            tilbakekreving.behandlingHistorikk.nåværende().entry.settPåVent(
                årsak = venteårsak,
                utløpsdato = tidsfrist,
                begrunnelse = begrunnelse,
            )
        }
    }

    fun taAvVent(tilbakekrevingId: String) {
        hentOgLagreTilbakekreving(tilbakekrevingId) { tilbakekreving ->
            tilbakekreving.behandlingHistorikk.nåværende().entry.taAvVent()
        }
    }

    fun flyttBehandlingTilFakta(tilbakekrevingId: String) {
        hentOgLagreTilbakekreving(tilbakekrevingId) { tilbakekreving ->
            tilbakekreving.håndterNullstilling()
        }
    }

    fun aktiverBrevmottakerSteg(tilbakekrevingId: String) {
        hentOgLagreTilbakekreving(tilbakekrevingId) { tilbakekreving ->
            validerBrevmottaker(tilbakekreving)
            tilbakekreving.aktiverBrevmottakerSteg()
        }
    }

    fun fjernBrevmottakerSteg(tilbakekrevingId: String) {
        hentOgLagreTilbakekreving(tilbakekrevingId) { tilbakekreving ->
            tilbakekreving.deaktiverBrevmottakerSteg()
        }
    }

    fun fjernManuelBrevmottaker(
        behandler: Behandler,
        tilbakekrevingId: String,
        manuellBrevmottakerId: UUID,
    ) {
        hentOgLagreTilbakekreving(tilbakekrevingId) { tilbakekreving ->
            tilbakekreving.fjernManuelBrevmottaker(behandler, manuellBrevmottakerId)
        }
    }

    fun hentBehandlingsinfo(
        tilbakekreving: Tilbakekreving,
    ): List<Behandlingsinfo> {
        val behandlingsinformasjon = tilbakekreving.behandlingHistorikk.nåværende().entry.hentBehandlingsinformasjon()
        return listOf(
            Behandlingsinfo(
                eksternKravgrunnlagId = null,
                kravgrunnlagId = null,
                kravgrunnlagKravstatuskode = null,
                eksternId = behandlingsinformasjon.kravgrunnlagReferanse,
                opprettetTid = behandlingsinformasjon.opprettetTid,
                behandlingId = behandlingsinformasjon.behandlingId,
                behandlingstatus = null,
            ),
        )
    }

    private fun validerBrevmottaker(tilbakekreving: Tilbakekreving) {
        val behandlingId = tilbakekreving.behandlingHistorikk.nåværende().entry.internId

        val personIdenter = listOfNotNull(tilbakekreving.bruker!!.aktør.ident)
        if (personIdenter.isEmpty()) return
        val strengtFortroligePersonIdenter =
            pdlClient.hentAdressebeskyttelseBolk(personIdenter, tilbakekreving.hentFagsysteminfo().tilFagsystemDTO(), SecureLog.Context.fra(tilbakekreving))
                .filter { (_, person) ->
                    person.adressebeskyttelse.any { adressebeskyttelse ->
                        adressebeskyttelse.gradering == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG ||
                            adressebeskyttelse.gradering == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND
                    }
                }.map { it.key }

        if (strengtFortroligePersonIdenter.isNotEmpty()) {
            val melding =
                "Behandlingen (id: $behandlingId) inneholder person med strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere."
            val frontendFeilmelding =
                "Behandlingen inneholder person med strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere."
            throw Feil(
                message = melding,
                frontendFeilmelding = frontendFeilmelding,
                logContext = SecureLog.Context.fra(tilbakekreving),
            )
        }
    }
}
