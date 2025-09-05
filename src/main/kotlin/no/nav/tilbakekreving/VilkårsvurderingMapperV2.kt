package no.nav.tilbakekreving

import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonSærligeGrunner
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Skyldgrad
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat

object VilkårsvurderingMapperV2 {
    private fun VilkårsvurderingsperiodeDto.særligeGrunner(): ReduksjonSærligeGrunner {
        return ReduksjonSærligeGrunner(
            aktsomhetDto!!.særligeGrunnerBegrunnelse!!,
            aktsomhetDto!!.særligeGrunner!!.map { it.særligGrunn }.toSet(),
            when (aktsomhetDto!!.særligeGrunnerTilReduksjon) {
                true -> ReduksjonSærligeGrunner.SkalReduseres.Ja(
                    aktsomhetDto!!.andelTilbakekreves!!.toInt(),
                )
                false -> ReduksjonSærligeGrunner.SkalReduseres.Nei
            },
        )
    }

    private fun VilkårsvurderingsperiodeDto.aktsomhetsvurdering() =
        aktsomhetDto!!.let { aktsomhet ->
            when (aktsomhet.aktsomhet) {
                Aktsomhet.FORSETT ->
                    NivåAvForståelse.Aktsomhet.Forsett(begrunnelse = aktsomhet.begrunnelse)

                Aktsomhet.GROV_UAKTSOMHET ->
                    NivåAvForståelse.Aktsomhet.GrovUaktsomhet(
                        begrunnelse = aktsomhet.begrunnelse,
                        reduksjonSærligeGrunner = særligeGrunner(),
                    )

                Aktsomhet.SIMPEL_UAKTSOMHET ->
                    NivåAvForståelse.Aktsomhet.Uaktsomhet(
                        begrunnelse = aktsomhet.begrunnelse,
                        kanUnnlates4XRettsgebyr = KanUnnlates4xRettsgebyr.Tilbakekreves(særligeGrunner()),
                    )
            }
        }

    fun tilVurdering(periode: VilkårsvurderingsperiodeDto) =
        when (periode.vilkårsvurderingsresultat) {
            Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT ->
                NivåAvForståelse.BurdeForstått(
                    begrunnelse = periode.begrunnelse,
                    aktsomhet = periode.aktsomhetsvurdering(),
                )

            Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER ->
                Skyldgrad.GrovUaktsomhet(
                    begrunnelse = periode.begrunnelse,
                    reduksjonSærligeGrunner = periode.særligeGrunner(),
                )

            Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER ->
                Skyldgrad.Forsett(
                    begrunnelse = periode.begrunnelse,
                )

            Vilkårsvurderingsresultat.GOD_TRO ->
                NivåAvForståelse.GodTro(
                    beløpIBehold =
                        when (periode.godTroDto!!.beløpErIBehold) {
                            true -> NivåAvForståelse.GodTro.BeløpIBehold.Ja(periode.godTroDto!!.beløpTilbakekreves!!)
                            false -> NivåAvForståelse.GodTro.BeløpIBehold.Nei
                        },
                    begrunnelse = periode.godTroDto!!.begrunnelse,
                )

            Vilkårsvurderingsresultat.UDEFINERT -> ForårsaketAvBruker.IkkeVurdert
        }
}
