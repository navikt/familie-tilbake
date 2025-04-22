package no.nav.tilbakekreving

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.ApplicationProperties
import no.nav.familie.tilbake.integration.pdl.PdlClient
import no.nav.familie.tilbake.integration.pdl.internal.PdlKjønnType
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFaktaDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeslåVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.api.v2.EksternFagsakDto
import no.nav.tilbakekreving.api.v2.OpprettTilbakekrevingEvent
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.behandling.BehandlingHistorikk
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg
import no.nav.tilbakekreving.behov.BrukerinfoBehov
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
import no.nav.tilbakekreving.kontrakter.bruker.Kjønn
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.Random
import java.util.UUID

@Service
class TilbakekrevingService(
    private val applicationProperties: ApplicationProperties,
    private val pdlClient: PdlClient,
) {
    private final val behovObservatør = Observatør()

    private val fnr = "20046912345"

    private val eksternFagsak =
        EksternFagsak(
            eksternId = "TEST-101010",
            ytelsestype = Ytelsestype.BARNETRYGD,
            fagsystem = Fagsystem.BA,
            behandlinger = EksternFagsakBehandlingHistorikk(mutableListOf()),
            behovObservatør = behovObservatør,
        )
    private val eksempelsaker =
        listOf(
            Tilbakekreving(
                eksternFagsak,
                opprettet = LocalDateTime.of(2025, Month.MARCH, 15, 12, 0),
                behandlingHistorikk =
                    BehandlingHistorikk(mutableListOf()),
                behovObservatør = behovObservatør,
                kravgrunnlagHistorikk = KravgrunnlagHistorikk(mutableListOf()),
                brevHistorikk = BrevHistorikk(mutableListOf()),
                opprettelsesvalg = Opprettelsesvalg.OPPRETT_BEHANDLING_MED_VARSEL,
            ).apply {
                håndter(
                    OpprettTilbakekrevingEvent(
                        EksternFagsakDto(
                            fagsystem = Fagsystem.BA,
                            ytelsestype = Ytelsestype.BARNETRYGD,
                            eksternId = "TEST-101010",
                        ),
                        opprettelsesvalg = Opprettelsesvalg.OPPRETT_BEHANDLING_MED_VARSEL,
                    ),
                )
                håndter(
                    KravgrunnlagHendelse(
                        internId = UUID.randomUUID(),
                        vedtakId = BigInteger(128, Random()),
                        kravstatuskode = KravgrunnlagHendelse.Kravstatuskode.NYTT,
                        fagsystemVedtaksdato = LocalDate.now(),
                        vedtakGjelder = KravgrunnlagHendelse.Aktør.Person(fnr),
                        utbetalesTil = KravgrunnlagHendelse.Aktør.Person(fnr),
                        skalBeregneRenter = false,
                        ansvarligEnhet = "0425",
                        kontrollfelt = UUID.randomUUID().toString(),
                        referanse = UUID.randomUUID().toString(),
                        kravgrunnlagId = UUID.randomUUID().toString(),
                        perioder =
                            listOf(
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
                                    ytelsesbeløp =
                                        listOf(
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
            },
        )

    fun hentTilbakekreving(
        fagsystem: Fagsystem,
        eksternFagsakId: String,
    ): Tilbakekreving? {
        if (!applicationProperties.toggles.nyModellEnabled) return null

        return eksempelsaker.firstOrNull { it.tilFrontendDto().fagsystem == fagsystem && it.tilFrontendDto().eksternFagsakId == eksternFagsakId }
    }

    fun hentTilbakekreving(behandlingId: UUID): Tilbakekreving? {
        if (!applicationProperties.toggles.nyModellEnabled) return null

        return eksempelsaker.firstOrNull { sak -> sak.tilFrontendDto().behandlinger.any { it.eksternBrukId == behandlingId } }
    }

    fun sjekkBehovOgHåndter(tilbakekreving: Tilbakekreving) {
        val behovListe = behovObservatør.behovListe
        while (behovListe.isNotEmpty()) {
            val behov = behovListe.first()
            when (behov) {
                is BrukerinfoBehov -> {
                    val personinfo = pdlClient.hentPersoninfo(
                        ident = tilbakekreving.bruker!!.ident,
                        fagsystem = behov.fagsystem,
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
                            eksternId = UUID.randomUUID().toString(),
                            ident = fnr,
                            revurderingsresultat = "revurderingsresultat",
                            revurderingsårsak = "revurderingsårsak",
                            begrunnelseForTilbakekreving = "begrunnelseForTilbakekreving",
                            revurderingsvedtaksdato = LocalDate.now(),
                        ),
                    )
                }
            }
            behovListe.remove(behov)
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
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry

        fakta.feilutbetaltePerioder.forEach {
            behandling.håndter(behandler, it)
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
            behandling.håndter(
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
            behandling.håndter(
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
        tilbakekreving.behandlingHistorikk.nåværende().entry.håndter(
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
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry
        for (stegVurdering in vurdering.totrinnsvurderinger) {
            behandling.håndter(
                beslutter = beslutter,
                behandlingssteg = stegVurdering.behandlingssteg,
                vurdering = when (stegVurdering.godkjent) {
                    true -> FatteVedtakSteg.Vurdering.Godkjent
                    else -> FatteVedtakSteg.Vurdering.Underkjent(stegVurdering.begrunnelse!!)
                },
            )
        }
    }
}
