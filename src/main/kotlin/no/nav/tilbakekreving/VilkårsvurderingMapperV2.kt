package no.nav.tilbakekreving

import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.behandling.saksbehandling.SærligGrunn
import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnTyper
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat

object VilkårsvurderingMapperV2 {
    private fun VilkårsvurderingsperiodeDto.aktsomhetsvurdering() =
        aktsomhetDto!!.let { aktsomhet ->
            when (aktsomhet.aktsomhet) {
                Aktsomhet.FORSETT ->
                    Vilkårsvurderingsteg.VurdertAktsomhet.Forsett(
                        begrunnelse = aktsomhet.begrunnelse,
                        skalIleggesRenter = aktsomhet.ileggRenter ?: false,
                    )

                Aktsomhet.GROV_UAKTSOMHET ->
                    Vilkårsvurderingsteg.VurdertAktsomhet.GrovUaktsomhet(
                        begrunnelse = aktsomhet.begrunnelse,
                        særligeGrunner =
                            Vilkårsvurderingsteg.VurdertAktsomhet.SærligeGrunner(
                                begrunnelse = aktsomhet.særligeGrunnerBegrunnelse!!,
                                grunner = aktsomhet.særligeGrunner!!.map {
                                    when (it.særligGrunn) {
                                        SærligGrunnTyper.ANNET -> SærligGrunn.Annet(it.begrunnelse!!)
                                        SærligGrunnTyper.STØRRELSE_BELØP -> SærligGrunn.StørrelseBeløp
                                        SærligGrunnTyper.HELT_ELLER_DELVIS_NAVS_FEIL -> SærligGrunn.HeltEllerDelvisNavsFeil
                                        SærligGrunnTyper.GRAD_AV_UAKTSOMHET -> SærligGrunn.GradAvUaktsomhet
                                        SærligGrunnTyper.TID_FRA_UTBETALING -> SærligGrunn.TidFraUtbetaling
                                    }
                                }.toSet(),
                            ),
                        skalReduseres =
                            when (aktsomhet.særligeGrunnerTilReduksjon) {
                                true -> Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres.Ja(aktsomhet.andelTilbakekreves!!.toInt())
                                false -> Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres.Nei
                            },
                        skalIleggesRenter = aktsomhet.ileggRenter!!,
                    )

                Aktsomhet.SIMPEL_UAKTSOMHET ->
                    Vilkårsvurderingsteg.VurdertAktsomhet.SimpelUaktsomhet(
                        begrunnelse = aktsomhet.begrunnelse,
                        særligeGrunner =
                            Vilkårsvurderingsteg.VurdertAktsomhet.SærligeGrunner(
                                begrunnelse = aktsomhet.særligeGrunnerBegrunnelse!!,
                                grunner = aktsomhet.særligeGrunner!!.map {
                                    when (it.særligGrunn) {
                                        SærligGrunnTyper.ANNET -> SærligGrunn.Annet(it.begrunnelse!!)
                                        SærligGrunnTyper.STØRRELSE_BELØP -> SærligGrunn.StørrelseBeløp
                                        SærligGrunnTyper.HELT_ELLER_DELVIS_NAVS_FEIL -> SærligGrunn.HeltEllerDelvisNavsFeil
                                        SærligGrunnTyper.GRAD_AV_UAKTSOMHET -> SærligGrunn.GradAvUaktsomhet
                                        SærligGrunnTyper.TID_FRA_UTBETALING -> SærligGrunn.TidFraUtbetaling
                                    }
                                }.toSet(),
                            ),
                        skalReduseres =
                            when (aktsomhet.særligeGrunnerTilReduksjon) {
                                true -> Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres.Ja(aktsomhet.andelTilbakekreves!!.toInt())
                                false -> Vilkårsvurderingsteg.VurdertAktsomhet.SkalReduseres.Nei
                            },
                    )
            }
        }

    fun tilVurdering(periode: VilkårsvurderingsperiodeDto) =
        when (periode.vilkårsvurderingsresultat) {
            Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT ->
                Vilkårsvurderingsteg.Vurdering.ForstodEllerBurdeForstått(
                    begrunnelse = periode.begrunnelse,
                    aktsomhet = periode.aktsomhetsvurdering(),
                )

            Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER ->
                Vilkårsvurderingsteg.Vurdering.MangelfulleOpplysningerFraBruker(
                    begrunnelse = periode.begrunnelse,
                    aktsomhet = periode.aktsomhetsvurdering(),
                )

            Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER ->
                Vilkårsvurderingsteg.Vurdering.FeilaktigeOpplysningerFraBruker(
                    begrunnelse = periode.begrunnelse,
                    aktsomhet = periode.aktsomhetsvurdering(),
                )

            Vilkårsvurderingsresultat.GOD_TRO ->
                Vilkårsvurderingsteg.Vurdering.GodTro(
                    beløpIBehold =
                        when (periode.godTroDto!!.beløpErIBehold) {
                            true -> Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold.Ja(periode.godTroDto!!.beløpTilbakekreves!!)
                            false -> Vilkårsvurderingsteg.Vurdering.GodTro.BeløpIBehold.Nei
                        },
                    begrunnelse = periode.godTroDto!!.begrunnelse,
                )

            Vilkårsvurderingsresultat.UDEFINERT -> Vilkårsvurderingsteg.Vurdering.IkkeVurdert
        }
}
