package no.nav.tilbakekreving.e2e

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class Under4xRettsgebyrTest : TilbakekrevingE2EBase() {
    @Test
    fun `behandler FORSTO_BURDE_FORSTÅTT under 4x rettsgebyr, ingen tilbakekreving`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Z999999")

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(),
        )

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
    fun `behandler FEIL_OPPLYSNINGER_FRA_BRUKER og under 4x rettsgebyr, ingen tilbakekreving`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Z999999")

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(),
        )

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(),
        )

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingUnder4xRettsgebyrIngenTilbakekreving(vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER),
        )

        val behandlingResultat = tilbakekreving(behandlingId).behandlingHistorikk.nåværende().entry.beregnForFrontend()

        behandlingResultat.beregningsresultatsperioder.firstOrNull()?.tilbakekrevingsbeløp shouldBe BigDecimal.ZERO
        behandlingResultat.vedtaksresultat shouldBe Vedtaksresultat.INGEN_TILBAKEBETALING
    }

    @Test
    fun `behandler MANGELFULLE_OPPLYSNINGER_FRA_BRUKER og under 4x rettsgebyr, ingen tilbakekreving`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Z999999")

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(),
        )

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(),
        )

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingUnder4xRettsgebyrIngenTilbakekreving(vilkårsvurderingsresultat = Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER),
        )

        val behandlingResultat = tilbakekreving(behandlingId).behandlingHistorikk.nåværende().entry.beregnForFrontend()

        behandlingResultat.beregningsresultatsperioder.firstOrNull()?.tilbakekrevingsbeløp shouldBe BigDecimal.ZERO
        behandlingResultat.vedtaksresultat shouldBe Vedtaksresultat.INGEN_TILBAKEBETALING
    }

    @Test
    fun `behandler FORSTO_BURDE_FORSTÅTT under 4x rettsgebyr, full tilbakekreving`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Z999999")

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(),
        )

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(),
        )
        val behandlingFørVilkårsvurdering = tilbakekreving(behandlingId).behandlingHistorikk.nåværende().entry.beregnForFrontend()

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingUnder4xRettsgebyrFullTilbakekreving(vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT),
        )

        val behandlingEtterVilkårsvurdering = tilbakekreving(behandlingId).behandlingHistorikk.nåværende().entry.beregnForFrontend()

        behandlingEtterVilkårsvurdering.beregningsresultatsperioder.firstOrNull()?.tilbakekrevingsbeløp shouldBe behandlingFørVilkårsvurdering.beregningsresultatsperioder.firstOrNull()?.tilbakekrevingsbeløp
        behandlingEtterVilkårsvurdering.vedtaksresultat shouldBe Vedtaksresultat.FULL_TILBAKEBETALING
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
        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Z999999")

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(),
        )

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
    fun `behandler MANGELFULLE_OPPLYSNINGER_FRA_BRUKER og under 4x rettsgebyr, full tilbakekreving`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Z999999")

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(),
        )

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(),
        )
        val behandlingFørVilkårsvurdering = tilbakekreving(behandlingId).behandlingHistorikk.nåværende().entry.beregnForFrontend()

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingUnder4xRettsgebyrFullTilbakekreving(vilkårsvurderingsresultat = Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER),
        )
        val behandlingEtterVilkårsvurdering = tilbakekreving(behandlingId).behandlingHistorikk.nåværende().entry.beregnForFrontend()

        behandlingEtterVilkårsvurdering.beregningsresultatsperioder.firstOrNull()?.tilbakekrevingsbeløp shouldBe behandlingFørVilkårsvurdering.beregningsresultatsperioder.firstOrNull()?.tilbakekrevingsbeløp
        behandlingEtterVilkårsvurdering.vedtaksresultat shouldBe Vedtaksresultat.FULL_TILBAKEBETALING
    }

    @Test
    fun `behandler FEIL_OPPLYSNINGER_FRA_BRUKER og under 4x rettsgebyr, delvis tilbakekreving`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        val ansvarligSaksbehandler = Behandler.Saksbehandler("Z999999")

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(),
        )

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(),
        )

        val behandlingFørVilkårsvurdering = tilbakekreving(behandlingId).behandlingHistorikk.nåværende().entry.beregnForFrontend()

        utførSteg(
            ident = ansvarligSaksbehandler.ident,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingUnder4xRettsgebyrDelvisTilbakekreving(vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER),
        )

        val behandlingEtterVilkårsvurdering = tilbakekreving(behandlingId).behandlingHistorikk.nåværende().entry.beregnForFrontend()

        behandlingEtterVilkårsvurdering.beregningsresultatsperioder.firstOrNull()?.tilbakekrevingsbeløp shouldBe (behandlingFørVilkårsvurdering.beregningsresultatsperioder.firstOrNull()?.tilbakekrevingsbeløp)?.div(BigDecimal(2))
        behandlingEtterVilkårsvurdering.vedtaksresultat shouldBe Vedtaksresultat.DELVIS_TILBAKEBETALING
    }
}
