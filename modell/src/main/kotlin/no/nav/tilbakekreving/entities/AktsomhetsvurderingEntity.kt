package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
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

            VurderingType.FORÅRSAKET_AV_BRUKER_UAKTSOMT -> {
                val aktsomhet = requireNotNull(aktsomhet) { "Aktsomhet kreves for uaktsomt" }
                Skyldgrad.Uaktsomt(
                    begrunnelse = requireNotNull(begrunnelse) { "Begrunnelse kreves for uaktsomt" },
                    reduksjonSærligeGrunner = requireNotNull(aktsomhet.særligGrunner) { "Særlige grunner kreves for uaktsomt" }.fraEntity(),
                    feilaktigeEllerMangelfulleOpplysninger = requireNotNull(feilaktigEllerMangelfull) { "Feilaktige eller mangelfulle opplysninger kreves for uaktsomt" }.fraEntity,
                )
            }

            VurderingType.FORÅRSAKET_AV_BRUKER_GROV_UAKTSOMHET -> {
                val aktsomhet = requireNotNull(aktsomhet) { "Aktsomhet kreves for grov uaktsomhet" }
                Skyldgrad.GrovUaktsomhet(
                    begrunnelse = requireNotNull(begrunnelse) { "Særlige grunner kreves for grov uaktsomhet" },
                    reduksjonSærligeGrunner = requireNotNull(aktsomhet.særligGrunner).fraEntity(),
                    feilaktigeEllerMangelfulleOpplysninger = requireNotNull(feilaktigEllerMangelfull) { "Feilaktige eller mangelfulle opplysninger kreves for grov uaktsomhet" }.fraEntity,
                )
            }

            VurderingType.FORÅRSAKET_AV_BRUKER_FORSETT -> {
                Skyldgrad.Forsett(
                    begrunnelse = requireNotNull(begrunnelse) { "Begrunnelse kreves for forsett" },
                    feilaktigeEllerMangelfulleOpplysninger = requireNotNull(feilaktigEllerMangelfull) { "Feilaktige eller mangelfulle opplysninger kreves for forsett" }.fraEntity,
                )
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
    FORÅRSAKET_AV_BRUKER_UAKTSOMT,
    FORÅRSAKET_AV_BRUKER_GROV_UAKTSOMHET,
    FORÅRSAKET_AV_BRUKER_FORSETT,
}

enum class FeilaktigEllerMangelfullType(val fraEntity: Skyldgrad.FeilaktigEllerMangelfull) {
    FEILAKTIG(Skyldgrad.FeilaktigEllerMangelfull.FEILAKTIG),
    MANGELFULL(Skyldgrad.FeilaktigEllerMangelfull.MANGELFULL),
}

enum class BeholdType {
    JA,
    NEI,
}
