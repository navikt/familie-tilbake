package no.nav.tilbakekreving

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.ApplicationProperties
import no.nav.familie.tilbake.integration.pdl.PdlClient
import no.nav.familie.tilbake.integration.pdl.internal.PdlKjønnType
import no.nav.familie.tilbake.kontrakter.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFaktaDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeslåVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.ManuellBrevmottakerRequestDto
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.RegistrertBrevmottaker
import no.nav.tilbakekreving.behov.Behov
import no.nav.tilbakekreving.behov.BrukerinfoBehov
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.behov.IverksettelseBehov
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.kontrakter.brev.MottakerType
import no.nav.tilbakekreving.kontrakter.bruker.Kjønn
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class TilbakekrevingService(
    private val applicationProperties: ApplicationProperties,
    private val pdlClient: PdlClient,
    private val iverksettService: IverksettService,
    private val tilbakekrevingRepository: TilbakekrevingRepository,
) {
    private val fnr = "20046912345"
    private val logger = TracedLogger.getLogger<TilbakekrevingService>()

    fun opprettTilbakekreving(
        opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
        håndter: (Tilbakekreving) -> Unit,
    ) {
        val observatør = Observatør()
        val tilbakekreving = Tilbakekreving.opprett(observatør, opprettTilbakekrevingHendelse)
        håndter(tilbakekreving)

        val logContext = SecureLog.Context.fra(tilbakekreving)
        logger.medContext(logContext) { info("Lagrer tilbakekreving") }
        lagre(tilbakekreving)

        logger.medContext(logContext) { info("Håndterer behov") }
        sjekkBehovOgHåndter(tilbakekreving, observatør, SecureLog.Context.fra(tilbakekreving))

        logger.medContext(logContext) { info("Tilbakekreving ferdig opprettet") }
    }

    fun hentTilbakekreving(
        fagsystem: FagsystemDTO,
        eksternFagsakId: String,
    ): Tilbakekreving? {
        if (!applicationProperties.toggles.nyModellEnabled) return null

        val tilbakekrevinger = tilbakekrevingRepository.hentAlleTilbakekrevinger()?.map { it.fraEntity(Observatør()) }
        return tilbakekrevinger?.firstOrNull { it.tilFrontendDto().fagsystem == fagsystem && it.tilFrontendDto().eksternFagsakId == eksternFagsakId }
    }

    fun hentTilbakekreving(behandlingId: UUID): Tilbakekreving? {
        if (!applicationProperties.toggles.nyModellEnabled) return null

        return tilbakekrevingRepository.hentTilbakekreving(behandlingId)?.fraEntity(Observatør())
    }

    fun <T> hentTilbakekreving(
        behandlingId: UUID,
        håndter: (Tilbakekreving) -> T,
    ): T? {
        val observatør = Observatør()
        return tilbakekrevingRepository.hentTilbakekreving(behandlingId)
            ?.fraEntity(observatør)
            ?.let { tilbakekreving ->
                val resultat = håndter(tilbakekreving)
                lagre(tilbakekreving)
                sjekkBehovOgHåndter(tilbakekreving, observatør, SecureLog.Context.fra(tilbakekreving))
                resultat
            }
    }

    fun lagre(
        tilbakekreving: Tilbakekreving,
    ) {
        tilbakekrevingRepository.lagre(
            tilbakekreving.id,
            tilbakekreving.behandlingHistorikk.nåværende().entry.internId,
            tilbakekreving.tilEntity(),
        )
    }

    private fun sjekkBehovOgHåndter(
        tilbakekreving: Tilbakekreving,
        observatør: Observatør,
        logContext: SecureLog.Context,
    ) {
        while (observatør.harUbesvarteBehov()) {
            try {
                val behov = observatør.nesteBehov()
                håndterBehov(tilbakekreving, behov, logContext)
                lagre(tilbakekreving)
            } catch (e: Exception) {
                logger.medContext(logContext) {
                    warn("Feilet under håndtering av behov", e)
                }
                break
            }
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
                tilbakekreving.håndter(
                    FagsysteminfoHendelse(
                        behandlingId = UUID.randomUUID().toString(),
                        ident = fnr,
                        revurderingsresultat = "revurderingsresultat",
                        revurderingsårsak = "revurderingsårsak",
                        begrunnelseForTilbakekreving = "begrunnelseForTilbakekreving",
                        revurderingsvedtaksdato = LocalDate.now(),
                    ),
                )
            }

            is IverksettelseBehov -> iverksettService.iverksett(behov, logContext)
        }
    }

    fun utførSteg(
        behandler: Behandler,
        tilbakekreving: Tilbakekreving,
        behandlingsstegDto: BehandlingsstegDto,
        logContext: SecureLog.Context,
    ) {
        val result = when (behandlingsstegDto) {
            is BehandlingsstegForeldelseDto -> behandleForeldelse(tilbakekreving, behandlingsstegDto, behandler)
            is BehandlingsstegVilkårsvurderingDto -> behandleVilkårsvurdering(tilbakekreving, behandlingsstegDto, behandler)
            is BehandlingsstegFaktaDto -> behandleFakta(tilbakekreving, behandlingsstegDto, behandler)
            is BehandlingsstegForeslåVedtaksstegDto -> behandleForeslåVedtak(tilbakekreving, behandlingsstegDto, behandler)
            is BehandlingsstegFatteVedtaksstegDto -> behandleFatteVedtak(tilbakekreving, behandlingsstegDto, behandler)
            else -> throw Feil("Vurdering for ${behandlingsstegDto.getSteg()} er ikke implementert i ny modell enda.", logContext = logContext)
        }
        tilbakekrevingRepository.lagreTilstand(tilbakekreving.tilEntity())
        return result
    }

    private fun behandleFakta(
        tilbakekreving: Tilbakekreving,
        fakta: BehandlingsstegFaktaDto,
        behandler: Behandler,
    ) {
        fakta.feilutbetaltePerioder.forEach {
            tilbakekreving.håndter(behandler, it)
            // TODO: Fakta steg
        }
    }

    private fun behandleVilkårsvurdering(
        tilbakekreving: Tilbakekreving,
        vurdering: BehandlingsstegVilkårsvurderingDto,
        behandler: Behandler,
    ) {
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry

        behandling.splittVilkårsvurdertePerioder(vurdering.vilkårsvurderingsperioder.map { it.periode })
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
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry
        behandling.splittForeldetPerioder(vurdering.foreldetPerioder.map { it.periode })
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
        for (stegVurdering in vurdering.totrinnsvurderinger) {
            tilbakekreving.håndter(
                beslutter = beslutter,
                behandlingssteg = stegVurdering.behandlingssteg,
                vurdering = when (stegVurdering.godkjent) {
                    true -> FatteVedtakSteg.Vurdering.Godkjent
                    else -> FatteVedtakSteg.Vurdering.Underkjent(stegVurdering.begrunnelse!!)
                },
            )
        }
    }

    fun behandleBrevmottaker(
        behandler: Behandler,
        tilbakekreving: Tilbakekreving,
        brevmottakerDto: ManuellBrevmottakerRequestDto,
        id: UUID,
    ) {
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
        tilbakekrevingRepository.lagreTilstand(tilbakekreving.tilEntity())
    }

    fun flyttBehandlingsstegTilbakeTilFakta(tilbakekreving: Tilbakekreving) {
        tilbakekreving.håndterNullstilling()
        tilbakekrevingRepository.lagreTilstand(tilbakekreving.tilEntity())
    }

    fun aktiverBrevmottakerSteg(tilbakekreving: Tilbakekreving) {
        validerBrevmottaker(tilbakekreving)
        tilbakekreving.aktiverBrevmottakerSteg()
        tilbakekrevingRepository.lagreTilstand(tilbakekreving.tilEntity())
    }

    fun deaktiverBrevmottakerSteg(tilbakekreving: Tilbakekreving) {
        tilbakekreving.deaktiverBrevmottakerSteg()
        tilbakekrevingRepository.lagreTilstand(tilbakekreving.tilEntity())
    }

    fun fjernManuelBrevmottaker(
        behandler: Behandler,
        tilbakekreving: Tilbakekreving,
        manuellBrevmottakerId: UUID,
    ) {
        tilbakekreving.fjernManuelBrevmottaker(behandler, manuellBrevmottakerId)
        tilbakekrevingRepository.lagreTilstand(tilbakekreving.tilEntity())
    }

    private fun validerBrevmottaker(tilbakekreving: Tilbakekreving) {
        val behandlingId = tilbakekreving.behandlingHistorikk.nåværende().entry.internId

        val personIdenter = listOfNotNull(tilbakekreving.bruker!!.ident)
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
