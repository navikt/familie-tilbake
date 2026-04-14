package no.nav.tilbakekreving.e2e

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.SkalUnnlates
import no.nav.tilbakekreving.builders.VilkårsvurderingDtoBuilder.forårsaketAvBruker
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.standardPeriode
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.over4Rettsgebyr
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal

class Under4xRettsgebyrTest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Test
    fun `behandler FORSTO_BURDE_FORSTÅTT under 4x rettsgebyr, ingen tilbakekreving`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))
        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        lagreUttalelse(behandlingId)
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Z999999")

        somSaksbehandler(ansvarligSaksbehandler.ident) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(),
        )

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingUnder4xRettsgebyrIngenTilbakekreving(vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT),
        )

        val behandlingResultat = tilbakekreving(behandlingId).behandlingHistorikk.nåværende().entry.beregnForFrontend()

        behandlingResultat.beregningsresultatsperioder.firstOrNull()?.tilbakekrevingsbeløp shouldBe BigDecimal.ZERO
        behandlingResultat.vedtaksresultat shouldBe Vedtaksresultat.INGEN_TILBAKEBETALING
    }

    @Test
    fun `behandler FEIL_OPPLYSNINGER_FRA_BRUKER og under 4x rettsgebyr, full tilbakekreving`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))
        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        lagreUttalelse(behandlingId)
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Z999999")

        somSaksbehandler(ansvarligSaksbehandler.ident) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(),
        )

        val behandlingFørVilkårsvurdering = tilbakekreving(behandlingId).behandlingHistorikk.nåværende().entry.beregnForFrontend()

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingUnder4xRettsgebyrFullTilbakekreving(vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER),
        )

        val behandlingEtterVilkårsvurdering = tilbakekreving(behandlingId).behandlingHistorikk.nåværende().entry.beregnForFrontend()

        behandlingEtterVilkårsvurdering.beregningsresultatsperioder.firstOrNull()?.tilbakekrevingsbeløp shouldBe behandlingFørVilkårsvurdering.beregningsresultatsperioder.firstOrNull()?.tilbakekrevingsbeløp
        behandlingEtterVilkårsvurdering.vedtaksresultat shouldBe Vedtaksresultat.FULL_TILBAKEBETALING
    }

    @Test
    fun `beløp over 4 rettsgebyr - blir lagret riktig`() {
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Z999999")
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)

        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
                perioder = listOf(
                    standardPeriode(1.januar(2021) til 1.januar(2021), feilutbetaltBeløp = 6900.kroner),
                ),
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))
        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        lagreUttalelse(behandlingId)

        somSaksbehandler(ansvarligSaksbehandler.ident) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(),
        )

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegVilkårsvurderingDto(
                listOf(
                    forårsaketAvBruker().uaktsomt(over4Rettsgebyr())
                        .copy(periode = 1.januar(2021) til 1.januar(2021)),
                ),
            ),
        )

        val vilkårsvurderingDto = tilbakekreving(behandlingId).behandlingHistorikk.nåværende().entry.vilkårsvurderingsstegDto.tilFrontendDto()
        vilkårsvurderingDto.perioder.single().vilkårsvurderingsresultatInfo?.aktsomhet?.unnlates4Rettsgebyr shouldBe SkalUnnlates.OVER_4_RETTSGEBYR
    }
}
