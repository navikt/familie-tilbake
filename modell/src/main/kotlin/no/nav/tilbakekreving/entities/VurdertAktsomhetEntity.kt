package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.SærligGrunn
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr.ErOver4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr.IkkeVurdert
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr.SkalIkkeUnnlates
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr.Unnlates
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonÅrsaker.ReduksjonSærligeGrunner
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType
import java.util.UUID

data class VurdertAktsomhetEntity(
    val periodeRef: UUID,
    val aktsomhetType: AktsomhetType,
    val begrunnelse: String,
    val skalIleggesRenter: Boolean?,
)

data class SærligeGrunnerEntity(
    val periodeRef: UUID,
    val begrunnelse: String,
    val grunner: List<SærligGrunnEntity>,
    val skalReduseres: SkalReduseresEntity,
) {
    fun fraEntity(): ReduksjonSærligeGrunner = ReduksjonSærligeGrunner(
        begrunnelse = begrunnelse,
        grunner = grunner.map { it.fraEntity() }.toSet(),
        skalReduseres = skalReduseres.fraEntity(),
    )
}

data class SærligGrunnEntity(
    val type: SærligGrunnType,
    val annetBegrunnelse: String?,
) {
    fun fraEntity(): SærligGrunn {
        return when (type) {
            SærligGrunnType.GRAD_AV_UAKTSOMHET -> SærligGrunn.GradAvUaktsomhet
            SærligGrunnType.HELT_ELLER_DELVIS_NAVS_FEIL -> SærligGrunn.HeltEllerDelvisNavsFeil
            SærligGrunnType.TID_FRA_UTBETALING -> SærligGrunn.TidFraUtbetaling
            SærligGrunnType.STØRRELSE_BELØP -> SærligGrunn.StørrelseBeløp
            SærligGrunnType.ANNET -> SærligGrunn.Annet(requireNotNull(annetBegrunnelse))
        }
    }
}

enum class KanUnnlatesEntity {
    UNNLATES,
    SKAL_IKKE_UNNLATES,
    OVER_4_RETTSGEBYR,
    IKKE_VURDERT,
    ;

    fun fraEntity(
        reduksjonType: ReduksjonType,
        særligeGrunner: SærligeGrunnerEntity?,
        godTroRelevanteMomenter: GodTroRelevanteMomenterEntity?,
        begrunnelseForUnnlatelse: String?,
    ): KanUnnlates4xRettsgebyr = when (reduksjonType) {
        ReduksjonType.REDUKSJON_SÆRLIGE_GRUNNER -> fraEntitySærligeGrunner(særligeGrunner, begrunnelseForUnnlatelse)
        ReduksjonType.REDUKSJON_GOD_TRO -> fraEntityRelevanteMomenter(godTroRelevanteMomenter, begrunnelseForUnnlatelse)
    }

    fun fraEntitySærligeGrunner(særligeGrunner: SærligeGrunnerEntity?, begrunnelseForUnnlatelse: String?): KanUnnlates4xRettsgebyr = when (this) {
        UNNLATES -> Unnlates(begrunnelseForUnnlatelse)
        SKAL_IKKE_UNNLATES -> SkalIkkeUnnlates(
            requireNotNull(særligeGrunner) { "SærligGrunner kreves for SKAL_IKKE_UNNLATES" }.fraEntity(),
        )
        OVER_4_RETTSGEBYR -> ErOver4xRettsgebyr(
            requireNotNull(særligeGrunner) { "SærligGrunner kreves for OVER_4_RETTSGEBYR" }.fraEntity(),
        )
        IKKE_VURDERT -> IkkeVurdert
    }

    fun fraEntityRelevanteMomenter(
        godTroRelevanteMomenterEntity: GodTroRelevanteMomenterEntity?,
        begrunnelseForUnnlatelse: String?,
    ): KanUnnlates4xRettsgebyr = when (this) {
        UNNLATES -> Unnlates(begrunnelseForUnnlatelse)
        SKAL_IKKE_UNNLATES -> SkalIkkeUnnlates(
            requireNotNull(godTroRelevanteMomenterEntity) { "GodTroRelevanteMomenter kreves for SKAL_IKKE_UNNLATES" }.fraEntity(),
        )

        IKKE_VURDERT -> IkkeVurdert
        OVER_4_RETTSGEBYR -> error("OVER_4_RETTSGEBYR er ikke en gyldig kombinasjon for god tro-vurdering")
    }
}

enum class AktsomhetType {
    SIMPEL_UAKTSOMHET,
    GROV_UAKTSOMHET,
    FORSETT,
    IKKE_UTVIST_SKYLD,
}

enum class ReduksjonType {
    REDUKSJON_SÆRLIGE_GRUNNER,
    REDUKSJON_GOD_TRO,
}
