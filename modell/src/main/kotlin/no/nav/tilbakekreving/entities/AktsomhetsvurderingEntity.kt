package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Skyldgrad
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Vilkårsvurderingsteg.Vilkårsvurderingsperiode
import java.math.BigDecimal
import java.util.UUID

data class AktsomhetsvurderingEntity(
    val vurderingType: VurderingType,
    val mottakersForståelse: MottakersForståelseEntity?,
    val begrunnelse: String?,
    val beløpIBehold: GodTroEntity?,
    val aktsomhet: VurdertAktsomhetEntity?,
    val kanUnnlates: KanUnnlates?,
    val særligGrunner: SærligeGrunnerEntity?,
    val feilaktigEllerMangelfull: FeilaktigEllerMangelfullType?,
    val forrigePeriodeId: UUID?,
) {
    fun fraEntity(vurderinger: Map<UUID, Vilkårsvurderingsperiode>): ForårsaketAvBruker {
        return when (vurderingType) {
            VurderingType.IKKE_FORÅRSAKET_AV_BRUKER_GOD_TRO -> {
                NivåAvForståelse.GodTro(
                    beløpIBehold = requireNotNull(beløpIBehold) { "beløpIBehold kreves i GOD_TRO " }.fraEntity(),
                    begrunnelse = requireNotNull(begrunnelse) { "begrunnelse kreves i GOD_TRO " },
                    begrunnelseForGodTro = requireNotNull(beløpIBehold) { "begrunnelse kreves i GOD_TRO " }.begrunnelse,
                )
            }

            VurderingType.IKKE_FORÅRSAKET_AV_BRUKER_BURDE_FORSTÅTT -> {
                val mottakersForståelse = requireNotNull(mottakersForståelse) { "mottakersForståelse kreves i BURDE_FORSTÅTT" }
                NivåAvForståelse.BurdeForstått(
                    grad = when (mottakersForståelse.mottakersForståelse) {
                        Forståelsesgrad.BURDE_FORSTÅTT -> NivåAvForståelse.Grad.BURDE_FORSTÅTT
                        Forståelsesgrad.MÅTTE_FORSTÅ -> NivåAvForståelse.Grad.MÅTTE_FORSTÅ
                        Forståelsesgrad.FORSTOD -> error("FORSTOD er ikke en gyldig grad for BURDE_FORSTÅTT")
                    },
                    begrunnelseMottakersForståelse = mottakersForståelse.begrunnelse,
                    begrunnelse = requireNotNull(begrunnelse) { "begrunnesle kreves i FORSTOD_ELLER_BURDE_FORSTÅTT " },
                    kanUnnlates4XRettsgebyr = requireNotNull(kanUnnlates) { "forårsaket av bruker trenger vurdering om beløp kan unnlates" }.fraEntity(særligGrunner),
                )
            }

            VurderingType.IKKE_FORÅRSAKET_AV_BRUKER_FORSTOD -> {
                NivåAvForståelse.Forstod(
                    begrunnelseMottakersForståelse = requireNotNull(mottakersForståelse) { "mottakersForståelse kreves i FORSTOD" }.begrunnelse,
                    begrunnelse = requireNotNull(begrunnelse) { "begrunnesle kreves i FORSTOD_ELLER_BURDE_FORSTÅTT " },
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
                            kanUnnlates4XRettsgebyr = requireNotNull(kanUnnlates) { "forårsaket av bruker trenger vurdering om beløp kan unnlates" }.fraEntity(særligGrunner),
                        )
                    }
                    AktsomhetType.GROV_UAKTSOMHET -> Skyldgrad.GrovUaktsomhet(
                        begrunnelse = requireNotNull(begrunnelse) { "Særlige grunner kreves for grov uaktsomhet" },
                        begrunnelseAktsomhet = aktsomhet.begrunnelse,
                        reduksjonSærligeGrunner = requireNotNull(særligGrunner) { "Særlige grunner kreves for grov uaktsomhet" }.fraEntity(),
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

            VurderingType.IKKE_VURDERT -> ForårsaketAvBruker.IkkeVurdert()
            VurderingType.KOPIERT_VURDERING -> ForårsaketAvBruker.KopiertVurdering(
                forrigeVurdering = requireNotNull(vurderinger[forrigePeriodeId]) { "Fant ikke vurdering å kopiere fra med id $forrigePeriodeId" },
                forrigePeriodeId = forrigePeriodeId,
            )
        }
    }
}

data class GodTroEntity(
    val periodeRef: UUID,
    val begrunnelse: String,
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
    KOPIERT_VURDERING,
}

enum class Forståelsesgrad {
    FORSTOD,
    BURDE_FORSTÅTT,
    MÅTTE_FORSTÅ,
}

data class MottakersForståelseEntity(
    val periodeRef: UUID,
    val mottakersForståelse: Forståelsesgrad,
    val begrunnelse: String,
)

enum class FeilaktigEllerMangelfullType(val fraEntity: Skyldgrad.FeilaktigEllerMangelfull) {
    FEILAKTIG(Skyldgrad.FeilaktigEllerMangelfull.FEILAKTIG),
    MANGELFULL(Skyldgrad.FeilaktigEllerMangelfull.MANGELFULL),
}

enum class BeholdType {
    JA,
    NEI,
}
