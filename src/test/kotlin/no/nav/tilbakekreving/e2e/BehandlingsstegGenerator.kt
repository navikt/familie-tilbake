package no.nav.tilbakekreving.e2e

import no.nav.tilbakekreving.api.v1.dto.AktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFaktaDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeslåVedtaksstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.FaktaFeilutbetalingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.ForeldelsesperiodeDto
import no.nav.tilbakekreving.api.v1.dto.FritekstavsnittDto
import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurdertTotrinnDto
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat

object BehandlingsstegGenerator {
    fun lagFaktastegVurderingFritekst(vararg perioder: Datoperiode = arrayOf(1.januar(2021) til 1.januar(2021))): BehandlingsstegFaktaDto {
        return BehandlingsstegFaktaDto(
            feilutbetaltePerioder = perioder.map { periode ->
                FaktaFeilutbetalingsperiodeDto(
                    periode = periode,
                    hendelsestype = Hendelsestype.ANNET,
                    hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
                )
            },
            begrunnelse = "Begrunnelse",
        )
    }

    fun lagIkkeForeldetVurdering(vararg perioder: Datoperiode = arrayOf(1.januar(2021) til 1.januar(2021))): BehandlingsstegForeldelseDto {
        return BehandlingsstegForeldelseDto(
            foreldetPerioder = perioder.map { periode ->
                ForeldelsesperiodeDto(
                    periode = periode,
                    begrunnelse = "Utbetalingen er ikke foreldet",
                    foreldelsesvurderingstype = Foreldelsesvurderingstype.IKKE_FORELDET,
                    foreldelsesfrist = null,
                    oppdagelsesdato = null,
                )
            },
        )
    }

    fun lagVilkårsvurderingFullTilbakekreving(vararg perioder: Datoperiode = arrayOf(1.januar(2021) til 1.januar(2021))): BehandlingsstegVilkårsvurderingDto = BehandlingsstegVilkårsvurderingDto(
        vilkårsvurderingsperioder = perioder.map { periode ->
            VilkårsvurderingsperiodeDto(
                periode = periode,
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                begrunnelse = "Jepp",
                godTroDto = null,
                aktsomhetDto = AktsomhetDto(
                    aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                    ileggRenter = false,
                    andelTilbakekreves = null,
                    beløpTilbakekreves = null,
                    begrunnelse = "Jaha",
                    særligeGrunner = emptyList(),
                    særligeGrunnerTilReduksjon = false,
                    tilbakekrevSmåbeløp = true,
                    særligeGrunnerBegrunnelse = "Særlige grunner",
                ),
            )
        },
    )

    fun lagForeslåVedtakVurdering() = BehandlingsstegForeslåVedtaksstegDto(
        fritekstavsnitt = FritekstavsnittDto(
            oppsummeringstekst = null,
            perioderMedTekst = emptyList(),
        ),
    )

    fun lagGodkjennVedtakVurdering(
        vararg behandlingssteg: Behandlingssteg = arrayOf(
            Behandlingssteg.FAKTA,
            Behandlingssteg.FORELDELSE,
            Behandlingssteg.VILKÅRSVURDERING,
            Behandlingssteg.FORESLÅ_VEDTAK,
        ),
    ) = BehandlingsstegFatteVedtaksstegDto(
        totrinnsvurderinger = behandlingssteg.map { VurdertTotrinnDto(behandlingssteg = it, godkjent = true, begrunnelse = null) },
    )
}
