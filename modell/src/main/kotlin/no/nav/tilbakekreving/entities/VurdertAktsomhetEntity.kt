package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.RelevanteMomentGodTro
import no.nav.tilbakekreving.behandling.saksbehandling.SærligGrunn
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr.ErOver4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr.IkkeVurdert
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr.SkalIkkeUnnlates
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr.Unnlates
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonMomenter.ReduksjonGodTro
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonMomenter.ReduksjonSærligeGrunner
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.MomentEllerSærligGrunnType
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.RelevanteMomentTypeGodTro
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType
import java.util.UUID

data class VurdertAktsomhetEntity(
    val periodeRef: UUID,
    val aktsomhetType: AktsomhetType,
    val begrunnelse: String,
    val skalIleggesRenter: Boolean?,
)

data class ReduksjonMomenterEntity(
    val periodeRef: UUID,
    val begrunnelse: String,
    val grunner: List<ReduksjonMomentEntity>,
    val skalReduseres: SkalReduseresEntity,
) {
    fun fraEntitySærligGrunn(): ReduksjonSærligeGrunner = ReduksjonSærligeGrunner(
        begrunnelse = begrunnelse,
        grunner = grunner.map { it.fraEntitySærligGrunn() }.toSet(),
        skalReduseres = skalReduseres.fraEntity(),
    )

    fun fraEntityRelevanteMomentGodTro(): ReduksjonGodTro {
        return ReduksjonGodTro(
            begrunnelse = begrunnelse,
            grunner = grunner.map { it.fraEntityRelevanteMomentGodTro() }.toSet(),
            skalReduseres = skalReduseres.fraEntity(),
        )
    }
}

data class ReduksjonMomentEntity(
    val type: MomentEllerSærligGrunnType,
    val annetBegrunnelse: String?,
) {
    fun fraEntitySærligGrunn(): SærligGrunn {
        return when (type) {
            SærligGrunnType.GRAD_AV_UAKTSOMHET -> SærligGrunn.GradAvUaktsomhet
            SærligGrunnType.HELT_ELLER_DELVIS_NAVS_FEIL -> SærligGrunn.HeltEllerDelvisNavsFeil
            SærligGrunnType.TID_FRA_UTBETALING -> SærligGrunn.TidFraUtbetaling
            SærligGrunnType.STØRRELSE_BELØP -> SærligGrunn.StørrelseBeløp
            SærligGrunnType.ANNET -> SærligGrunn.Annet(requireNotNull(annetBegrunnelse))
            else -> error("Det kreves særlig grunn type. Men type var $type")
        }
    }

    fun fraEntityRelevanteMomentGodTro(): RelevanteMomentGodTro {
        return when (type) {
            RelevanteMomentTypeGodTro.STØRRELSE_BELØP -> RelevanteMomentGodTro.StørrelseBeløp
            RelevanteMomentTypeGodTro.TID_FRA_UTBETALING -> RelevanteMomentGodTro.TidFraUtbetaling
            RelevanteMomentTypeGodTro.UTBETALING_TILLIT -> RelevanteMomentGodTro.UtbetalingTillit
            RelevanteMomentTypeGodTro.ANNET -> RelevanteMomentGodTro.Annet(requireNotNull(annetBegrunnelse))
            else -> error("Det kreves relevant moment type. Men type var $type")
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
        reduksjonMomenter: ReduksjonMomenterEntity?,
        begrunnelseForUnnlatelse: String?,
    ): KanUnnlates4xRettsgebyr = when (this) {
        UNNLATES -> Unnlates(begrunnelseForUnnlatelse)
        SKAL_IKKE_UNNLATES -> {
            when (reduksjonType) {
                ReduksjonType.REDUKSJON_SÆRLIGE_GRUNNER -> SkalIkkeUnnlates(
                    requireNotNull(reduksjonMomenter) { "SærligGrunner kreves for Særlig grunn reduksjon" }.fraEntitySærligGrunn(),
                )
                ReduksjonType.REDUKSJON_GOD_TRO -> SkalIkkeUnnlates(
                    requireNotNull(reduksjonMomenter) { "RelevanteMomenter kreves for God tro reduksjon" }.fraEntityRelevanteMomentGodTro(),
                )
            }
        }
        OVER_4_RETTSGEBYR -> ErOver4xRettsgebyr(
            requireNotNull(reduksjonMomenter) { "SærligGrunner kreves for OVER_4_RETTSGEBYR" }.fraEntitySærligGrunn(),
        )
        IKKE_VURDERT -> IkkeVurdert
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
