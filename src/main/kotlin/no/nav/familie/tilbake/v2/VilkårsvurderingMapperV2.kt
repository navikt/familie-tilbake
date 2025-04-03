package no.nav.familie.tilbake.v2

import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderderingsteg
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat

object VilkårsvurderingMapperV2 {
    private fun VilkårsvurderingsperiodeDto.aktsomhetsvurdering() =
        aktsomhetDto!!.let { aktsomhet ->
            when (aktsomhet.aktsomhet) {
                Aktsomhet.FORSETT ->
                    Vilkårsvurderderingsteg.VurdertAktsomhet.Forsett(
                        begrunnelse = aktsomhet.begrunnelse,
                        skalIleggesRenter = aktsomhet.ileggRenter!!,
                    )

                Aktsomhet.GROV_UAKTSOMHET ->
                    Vilkårsvurderderingsteg.VurdertAktsomhet.GrovUaktsomhet(
                        begrunnelse = aktsomhet.begrunnelse,
                        særligeGrunner =
                            Vilkårsvurderderingsteg.VurdertAktsomhet.SærligeGrunner(
                                begrunnelse = aktsomhet.særligeGrunnerBegrunnelse!!,
                                grunner = aktsomhet.særligeGrunner!!.map { it.særligGrunn }.toSet(),
                            ),
                        skalReduseres =
                            when (aktsomhet.særligeGrunnerTilReduksjon) {
                                true -> Vilkårsvurderderingsteg.VurdertAktsomhet.SkalReduseres.Ja(aktsomhet.andelTilbakekreves!!.toInt())
                                false -> Vilkårsvurderderingsteg.VurdertAktsomhet.SkalReduseres.Nei
                            },
                        skalIleggesRenter = aktsomhet.ileggRenter!!,
                    )

                Aktsomhet.SIMPEL_UAKTSOMHET ->
                    Vilkårsvurderderingsteg.VurdertAktsomhet.SimpelUaktsomhet(
                        begrunnelse = aktsomhet.begrunnelse,
                        særligeGrunner =
                            Vilkårsvurderderingsteg.VurdertAktsomhet.SærligeGrunner(
                                begrunnelse = aktsomhet.særligeGrunnerBegrunnelse!!,
                                grunner = aktsomhet.særligeGrunner!!.map { it.særligGrunn }.toSet(),
                            ),
                        skalReduseres =
                            when (aktsomhet.særligeGrunnerTilReduksjon) {
                                true -> Vilkårsvurderderingsteg.VurdertAktsomhet.SkalReduseres.Ja(aktsomhet.andelTilbakekreves!!.toInt())
                                false -> Vilkårsvurderderingsteg.VurdertAktsomhet.SkalReduseres.Nei
                            },
                    )
            }
        }

    fun tilVurdering(periode: VilkårsvurderingsperiodeDto) =
        when (periode.vilkårsvurderingsresultat) {
            Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT ->
                Vilkårsvurderderingsteg.Vurdering.ForstodEllerBurdeForstått(
                    begrunnelse = periode.begrunnelse,
                    aktsomhet = periode.aktsomhetsvurdering(),
                )

            Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER ->
                Vilkårsvurderderingsteg.Vurdering.MangelfulleOpplysningerFraBruker(
                    begrunnelse = periode.begrunnelse,
                    aktsomhet = periode.aktsomhetsvurdering(),
                )

            Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER ->
                Vilkårsvurderderingsteg.Vurdering.FeilaktigeOpplysningerFraBruker(
                    begrunnelse = periode.begrunnelse,
                    aktsomhet = periode.aktsomhetsvurdering(),
                )

            Vilkårsvurderingsresultat.GOD_TRO ->
                Vilkårsvurderderingsteg.Vurdering.GodTro(
                    beløpIBehold =
                        when (periode.godTroDto!!.beløpErIBehold) {
                            true -> Vilkårsvurderderingsteg.Vurdering.GodTro.BeløpIBehold.Ja(periode.godTroDto!!.beløpTilbakekreves!!)
                            false -> Vilkårsvurderderingsteg.Vurdering.GodTro.BeløpIBehold.Nei
                        },
                    begrunnelse = periode.godTroDto!!.begrunnelse,
                )

            Vilkårsvurderingsresultat.UDEFINERT -> Vilkårsvurderderingsteg.Vurdering.IkkeVurdert
        }
}
