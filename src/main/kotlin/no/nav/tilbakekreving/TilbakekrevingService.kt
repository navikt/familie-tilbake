package no.nav.tilbakekreving

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.ApplicationProperties
import no.nav.familie.tilbake.integration.pdl.PdlClient
import no.nav.familie.tilbake.integration.pdl.internal.PdlKjønnType
import no.nav.familie.tilbake.kontrakter.personopplysning.ADRESSEBESKYTTELSEGRADERING
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFaktaDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeslåVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.ManuellBrevmottakerRequestDto
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.RegistrertBrevmottaker
import no.nav.tilbakekreving.behov.BrukerinfoBehov
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.kontrakter.brev.MottakerType
import no.nav.tilbakekreving.kontrakter.bruker.Kjønn
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagBufferRepository
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagMapper
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.util.Random
import java.util.UUID

@Service
class TilbakekrevingService(
    private val applicationProperties: ApplicationProperties,
    private val pdlClient: PdlClient,
    private val kravgrunnlagBufferRepository: KravgrunnlagBufferRepository,
) {
    private val fnr = "20046912345"

    private data class InMemorySak(
        val observatør: Observatør,
        val tilbakekreving: Tilbakekreving,
    )

    private val eksempelsaker = mutableListOf(
        testsak(),
    )

    private fun testsak(): InMemorySak {
        val behovObservatør = Observatør()
        val tilbakekreving = Tilbakekreving.opprett(
            behovObservatør,
            OpprettTilbakekrevingHendelse(
                opprettelsesvalg = Opprettelsesvalg.OPPRETT_BEHANDLING_MED_VARSEL,
                eksternFagsak = OpprettTilbakekrevingHendelse.EksternFagsak(
                    eksternId = "TEST-101010",
                    ytelse = Ytelse.Barnetrygd,
                ),
            ),
        ).apply {
            håndter(
                KravgrunnlagHendelse(
                    internId = UUID.randomUUID(),
                    vedtakId = BigInteger(128, Random()),
                    kravstatuskode = KravgrunnlagHendelse.Kravstatuskode.NY,
                    fagsystemVedtaksdato = LocalDate.now(),
                    vedtakGjelder = KravgrunnlagHendelse.Aktør.Person(fnr),
                    utbetalesTil = KravgrunnlagHendelse.Aktør.Person(fnr),
                    skalBeregneRenter = false,
                    ansvarligEnhet = "0425",
                    kontrollfelt = UUID.randomUUID().toString(),
                    referanse = UUID.randomUUID().toString(),
                    kravgrunnlagId = UUID.randomUUID().toString(),
                    perioder = listOf(
                        KravgrunnlagHendelse.Periode(
                            periode =
                                Datoperiode(
                                    fom = LocalDate.of(2018, 1, 1),
                                    tom = LocalDate.of(2018, 2, 28),
                                ),
                            månedligSkattebeløp = BigDecimal("0.0"),
                            feilutbetaltBeløp = listOf(
                                KravgrunnlagHendelse.Periode.Beløp(
                                    klassekode = "",
                                    klassetype = "FEIL",
                                    opprinneligUtbetalingsbeløp = BigDecimal("12000.0"),
                                    nyttBeløp = BigDecimal("10000.0"),
                                    tilbakekrevesBeløp = BigDecimal("2000.0"),
                                    skatteprosent = BigDecimal("0.0"),
                                ),
                            ),
                            ytelsesbeløp = listOf(
                                KravgrunnlagHendelse.Periode.Beløp(
                                    klassekode = "",
                                    klassetype = "YTEL",
                                    opprinneligUtbetalingsbeløp = BigDecimal("12000.0"),
                                    nyttBeløp = BigDecimal("10000.0"),
                                    tilbakekrevesBeløp = BigDecimal("2000.0"),
                                    skatteprosent = BigDecimal("0.0"),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        }
        return InMemorySak(behovObservatør, tilbakekreving)
    }

    fun opprettTilbakekreving(
        opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
        håndter: (Tilbakekreving) -> Unit,
    ) {
        val observatør = Observatør()
        val tilbakekreving = Tilbakekreving.opprett(observatør, opprettTilbakekrevingHendelse)
        håndter(tilbakekreving)
        eksempelsaker.add(InMemorySak(observatør, tilbakekreving))
        lagre(observatør, tilbakekreving)

        sjekkBehovOgHåndter(tilbakekreving, observatør)
    }

    fun hentTilbakekreving(
        fagsystem: FagsystemDTO,
        eksternFagsakId: String,
    ): Tilbakekreving? {
        if (!applicationProperties.toggles.nyModellEnabled) return null

        return eksempelsaker.map { it.tilbakekreving }.firstOrNull { it.tilFrontendDto().fagsystem == fagsystem && it.tilFrontendDto().eksternFagsakId == eksternFagsakId }
    }

    fun hentTilbakekreving(behandlingId: UUID): Tilbakekreving? {
        if (!applicationProperties.toggles.nyModellEnabled) return null

        return eksempelsaker.map { it.tilbakekreving }.firstOrNull { sak -> sak.tilFrontendDto().behandlinger.any { it.eksternBrukId == behandlingId } }
    }

    fun <T> hentTilbakekreving(
        behandlingId: UUID,
        håndter: (Tilbakekreving) -> T,
    ): T? {
        val sak = eksempelsaker.firstOrNull { sak -> sak.tilbakekreving.tilFrontendDto().behandlinger.any { it.eksternBrukId == behandlingId } } ?: return null

        val result = håndter(sak.tilbakekreving)
        lagre(sak.observatør, sak.tilbakekreving)
        return result
    }

    fun lagre(
        observatør: Observatør,
        tilbakekreving: Tilbakekreving,
    ) {
        val eksisterendeSak = eksempelsaker.find { it.tilbakekreving.fagsystemId == tilbakekreving.fagsystemId }
        if (eksisterendeSak == null) {
            eksempelsaker.add(InMemorySak(observatør, tilbakekreving))
        } else {
            eksempelsaker.remove(eksisterendeSak)
            eksempelsaker.add(InMemorySak(eksisterendeSak.observatør, tilbakekreving))
        }
    }

    fun sjekkBehovOgHåndter(
        tilbakekreving: Tilbakekreving,
        observatør: Observatør,
    ) {
        while (observatør.harUbesvarteBehov()) {
            val behov = observatør.nesteBehov()
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
            }
            lagre(observatør, tilbakekreving)
        }
    }

    fun utførSteg(
        behandler: Behandler,
        tilbakekreving: Tilbakekreving,
        behandlingsstegDto: BehandlingsstegDto,
        logContext: SecureLog.Context,
    ) {
        return when (behandlingsstegDto) {
            is BehandlingsstegForeldelseDto -> behandleForeldelse(tilbakekreving, behandlingsstegDto, behandler)
            is BehandlingsstegVilkårsvurderingDto -> behandleVilkårsvurdering(tilbakekreving, behandlingsstegDto, behandler)
            is BehandlingsstegFaktaDto -> behandleFakta(tilbakekreving, behandlingsstegDto, behandler)
            is BehandlingsstegForeslåVedtaksstegDto -> behandleForeslåVedtak(tilbakekreving, behandlingsstegDto, behandler)
            is BehandlingsstegFatteVedtaksstegDto -> behandleFatteVedtak(tilbakekreving, behandlingsstegDto, behandler)
            else -> throw Feil("Vurdering for ${behandlingsstegDto.getSteg()} er ikke implementert i ny modell enda.", logContext = logContext)
        }
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
                    Foreldelsesvurderingstype.FORELDET -> Foreldelsesteg.Vurdering.Foreldet(periode.begrunnelse, periode.foreldelsesfrist!!)
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
    }

    fun flyttBehandlingsstegTilbakeTilFakta(tilbakekreving: Tilbakekreving) {
        tilbakekreving.håndterNullstilling()
    }

    fun aktiverBrevmottakerSteg(tilbakekreving: Tilbakekreving) {
        validerBrevmottaker(tilbakekreving)
        tilbakekreving.aktiverBrevmottakerSteg()
    }

    fun deaktiverBrevmottakerSteg(tilbakekreving: Tilbakekreving) {
        tilbakekreving.deaktiverBrevmottakerSteg()
    }

    fun fjernManuelBrevmottaker(
        behandler: Behandler,
        tilbakekreving: Tilbakekreving,
        manuellBrevmottakerId: UUID,
    ) {
        tilbakekreving.fjernManuelBrevmottaker(behandler, manuellBrevmottakerId)
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

    fun lesKravgrunnlag() {
        val ikkeHåndterteKravgrunnlag = kravgrunnlagBufferRepository.hentUlesteKravgrunnlag()
        ikkeHåndterteKravgrunnlag.forEach { entity ->
            val kravgrunnlag = KravgrunnlagUtil.unmarshalKravgrunnlag(entity.kravgrunnlag)
            opprettTilbakekreving(KravgrunnlagMapper.tilOpprettTilbakekrevingHendelse(kravgrunnlag)) { tilbakekreving ->
                tilbakekreving.håndter(KravgrunnlagMapper.tilKravgrunnlagHendelse(kravgrunnlag))
            }

            kravgrunnlagBufferRepository.markerLest(entity.kravgrunnlagId)
        }
    }
}
