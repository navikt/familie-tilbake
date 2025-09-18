package no.nav.tilbakekreving

import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.behandling.saksbehandling.SærligGrunn
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonSærligeGrunner
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Skyldgrad
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat

object VilkårsvurderingMapperV2 {
    private fun VilkårsvurderingsperiodeDto.særligeGrunner(): ReduksjonSærligeGrunner {
        return ReduksjonSærligeGrunner(
            aktsomhetDto!!.særligeGrunnerBegrunnelse!!,
            aktsomhetDto!!.særligeGrunner!!.map {
                when (it.særligGrunn) {
                    SærligGrunnType.ANNET -> SærligGrunn.Annet(it.begrunnelse!!)
                    SærligGrunnType.STØRRELSE_BELØP -> SærligGrunn.StørrelseBeløp
                    SærligGrunnType.HELT_ELLER_DELVIS_NAVS_FEIL -> SærligGrunn.HeltEllerDelvisNavsFeil
                    SærligGrunnType.GRAD_AV_UAKTSOMHET -> SærligGrunn.GradAvUaktsomhet
                    SærligGrunnType.TID_FRA_UTBETALING -> SærligGrunn.TidFraUtbetaling
                }
            }.toSet(),
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
                        kanUnnlates4XRettsgebyr = when (aktsomhet.tilbakekrevSmåbeløp) {
                            true -> KanUnnlates4xRettsgebyr.SkalIkkeUnnlates(særligeGrunner())
                            false -> KanUnnlates4xRettsgebyr.Unnlates
                        },
                    )
            }
        }

    private fun VilkårsvurderingsperiodeDto.skyldgrad(feilaktigEllerMangelfull: Skyldgrad.FeilaktigEllerMangelfull): Skyldgrad {
        val aktsomhet = aktsomhetDto!!

        return when (aktsomhet.aktsomhet) {
            Aktsomhet.FORSETT -> Skyldgrad.Forsett(
                begrunnelse = begrunnelse,
                begrunnelseAktsomhet = aktsomhet.begrunnelse,
                feilaktigeEllerMangelfulleOpplysninger = feilaktigEllerMangelfull,
            )
            Aktsomhet.GROV_UAKTSOMHET -> Skyldgrad.GrovUaktsomhet(
                begrunnelse = begrunnelse,
                begrunnelseAktsomhet = aktsomhet.begrunnelse,
                reduksjonSærligeGrunner = særligeGrunner(),
                feilaktigeEllerMangelfulleOpplysninger = feilaktigEllerMangelfull,
            )
            Aktsomhet.SIMPEL_UAKTSOMHET -> Skyldgrad.Uaktsomt(
                begrunnelse = begrunnelse,
                begrunnelseAktsomhet = aktsomhet.begrunnelse,
                feilaktigeEllerMangelfulleOpplysninger = feilaktigEllerMangelfull,
                kanUnnlates4XRettsgebyr = when (aktsomhet.tilbakekrevSmåbeløp) {
                    true -> KanUnnlates4xRettsgebyr.SkalIkkeUnnlates(særligeGrunner())
                    false -> KanUnnlates4xRettsgebyr.Unnlates
                },
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

            Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER -> periode.skyldgrad(Skyldgrad.FeilaktigEllerMangelfull.FEILAKTIG)

            Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER -> periode.skyldgrad(Skyldgrad.FeilaktigEllerMangelfull.MANGELFULL)

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
