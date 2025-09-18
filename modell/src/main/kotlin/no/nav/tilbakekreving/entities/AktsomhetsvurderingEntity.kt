package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Skyldgrad
import java.math.BigDecimal

data class AktsomhetsvurderingEntity(
    val vurderingType: VurderingType,
    val begrunnelse: String?,
    val beløpIBehold: BeløpIBeholdEntity?,
    val aktsomhet: VurdertAktsomhetEntity?,
    val feilaktigEllerMangelfull: FeilaktigEllerMangelfullType?,
) {
    fun fraEntity(): ForårsaketAvBruker {
        return when (vurderingType) {
            VurderingType.IKKE_FORÅRSAKET_AV_BRUKER_GOD_TRO -> {
                NivåAvForståelse.GodTro(
                    beløpIBehold = requireNotNull(beløpIBehold) { "beløpIBehold kreves i GOD_TRO " }.fraEntity(),
                    begrunnelse = requireNotNull(begrunnelse) { "begrunnelse kreves i GOD_TRO " },
                )
            }

            VurderingType.IKKE_FORÅRSAKET_AV_BRUKER_BURDE_FORSTÅTT -> {
                NivåAvForståelse.BurdeForstått(
                    begrunnelse = requireNotNull(begrunnelse) { "begrunnesle kreves i FORSTOD_ELLER_BURDE_FORSTÅTT " },
                    aktsomhet = requireNotNull(aktsomhet) { "aktsomhet kreves i FORSTOD_ELLER_BURDE_FORSTÅTT " }.tilAktsomhet(),
                )
            }

            VurderingType.IKKE_FORÅRSAKET_AV_BRUKER_FORSTOD -> {
                NivåAvForståelse.Forstod(
                    begrunnelse = requireNotNull(begrunnelse) { "begrunnesle kreves i FORSTOD_ELLER_BURDE_FORSTÅTT " },
                    aktsomhet = requireNotNull(aktsomhet) { "aktsomhet kreves i FORSTOD_ELLER_BURDE_FORSTÅTT " }.tilAktsomhet(),
                )
            }

            VurderingType.FORÅRSAKET_AV_BRUKER -> {
                val aktsomhet = requireNotNull(aktsomhet) { "Aktsomhet kreves for uaktsomt" }
                when (aktsomhet.aktsomhetType) {
                    AktsomhetType.SIMPEL_UAKTSOMHET -> {
                        Skyldgrad.Uaktsomt(
                            begrunnelse = requireNotNull(begrunnelse) { "Begrunnelse kreves for uaktsomt" },
                            begrunnelseAktsomhet = aktsomhet.begrunnelse,
                            feilaktigeEllerMangelfulleOpplysninger = requireNotNull(feilaktigEllerMangelfull) { "Feilaktige eller mangelfulle opplysninger kreves for uaktsomt" }.fraEntity,
                            kanUnnlates4XRettsgebyr = when (aktsomhet.kanUnnlates) {
                                KanUnnlates.UNNLATES -> KanUnnlates4xRettsgebyr.Unnlates
                                KanUnnlates.SKAL_IKKE_UNNLATES -> KanUnnlates4xRettsgebyr.SkalIkkeUnnlates(aktsomhet.særligGrunner!!.fraEntity())
                                null -> error("Uaktsomhet må avklare om det kan unnlates eller ikke.")
                            },
                        )
                    }
                    AktsomhetType.GROV_UAKTSOMHET -> Skyldgrad.GrovUaktsomhet(
                        begrunnelse = requireNotNull(begrunnelse) { "Særlige grunner kreves for grov uaktsomhet" },
                        begrunnelseAktsomhet = aktsomhet.begrunnelse,
                        reduksjonSærligeGrunner = requireNotNull(aktsomhet.særligGrunner).fraEntity(),
                        feilaktigeEllerMangelfulleOpplysninger = requireNotNull(feilaktigEllerMangelfull) { "Feilaktige eller mangelfulle opplysninger kreves for grov uaktsomhet" }.fraEntity,
                    )
                    AktsomhetType.FORSETT -> Skyldgrad.Forsett(
                        begrunnelse = requireNotNull(begrunnelse) { "Begrunnelse kreves for forsett" },
                        begrunnelseAktsomhet = aktsomhet.begrunnelse,
                        feilaktigeEllerMangelfulleOpplysninger = requireNotNull(feilaktigEllerMangelfull) { "Feilaktige eller mangelfulle opplysninger kreves for forsett" }.fraEntity,
                    )
                    AktsomhetType.IKKE_UTVIST_SKYLD -> error("Ikke utvist skyld er ikke relevant når feilutbetaling er forårsaket av bruker")
                }
            }

            VurderingType.IKKE_VURDERT -> ForårsaketAvBruker.IkkeVurdert
        }
    }
}

data class BeløpIBeholdEntity(
    val beholdType: BeholdType,
    val beløp: BigDecimal?,
) {
    fun fraEntity(): NivåAvForståelse.GodTro.BeløpIBehold {
        return when (beholdType) {
            BeholdType.JA -> {
                NivåAvForståelse.GodTro.BeløpIBehold.Ja(requireNotNull(beløp) { "Beløp kreves i BeløpIBehold" })
            }

            BeholdType.NEI -> {
                NivåAvForståelse.GodTro.BeløpIBehold.Nei
            }
        }
    }
}

enum class VurderingType {
    IKKE_VURDERT,
    IKKE_FORÅRSAKET_AV_BRUKER_FORSTOD,
    IKKE_FORÅRSAKET_AV_BRUKER_BURDE_FORSTÅTT,
    IKKE_FORÅRSAKET_AV_BRUKER_GOD_TRO,
    FORÅRSAKET_AV_BRUKER,
}

enum class FeilaktigEllerMangelfullType(val fraEntity: Skyldgrad.FeilaktigEllerMangelfull) {
    FEILAKTIG(Skyldgrad.FeilaktigEllerMangelfull.FEILAKTIG),
    MANGELFULL(Skyldgrad.FeilaktigEllerMangelfull.MANGELFULL),
}

enum class BeholdType {
    JA,
    NEI,
}
