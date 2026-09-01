package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.RelevantMomentGodTro
import no.nav.tilbakekreving.behandling.saksbehandling.SærligGrunn
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr.ErOver4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr.IkkeVurdert
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr.SkalIkkeUnnlates
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr.Unnlates
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonMomenter.ReduksjonGodTro
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonMomenter.ReduksjonSærligeGrunner
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.RelevantMomentTypeGodTro
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
    val grunner: List<String>,
    val skalReduseres: SkalReduseresEntity,
    val annetBegrunnelse: String?,
) {
    fun fraEntitySærligGrunn(): ReduksjonSærligeGrunner = ReduksjonSærligeGrunner(
        begrunnelse = begrunnelse,
        grunner = grunner.map { mapTilSærligGrunn(it, annetBegrunnelse) }.toSet(),
        skalReduseres = skalReduseres.fraEntity(),
    )

    private fun mapTilSærligGrunn(type: String, annetBegrunnelse: String?): SærligGrunn {
        return when (SærligGrunnType.valueOf(type)) {
            SærligGrunnType.GRAD_AV_UAKTSOMHET -> SærligGrunn.GradAvUaktsomhet
            SærligGrunnType.STØRRELSE_BELØP -> SærligGrunn.StørrelseBeløp
            SærligGrunnType.TID_FRA_UTBETALING -> SærligGrunn.TidFraUtbetaling
            SærligGrunnType.HELT_ELLER_DELVIS_NAVS_FEIL -> SærligGrunn.HeltEllerDelvisNavsFeil
            SærligGrunnType.ANNET -> SærligGrunn.Annet(requireNotNull(annetBegrunnelse))
        }
    }

    fun fraEntityRelevantMomentGodTro(): ReduksjonGodTro {
        return ReduksjonGodTro(
            begrunnelse = begrunnelse,
            grunner = grunner.map { mapTilReduksjonGodTro(it, annetBegrunnelse) }.toSet(),
            skalReduseres = skalReduseres.fraEntity(),
        )
    }

    private fun mapTilReduksjonGodTro(type: String, annetBegrunnelse: String?): RelevantMomentGodTro {
        return when (RelevantMomentTypeGodTro.valueOf(type)) {
            RelevantMomentTypeGodTro.STØRRELSE_BELØP -> RelevantMomentGodTro.StørrelseBeløp
            RelevantMomentTypeGodTro.TID_FRA_UTBETALING -> RelevantMomentGodTro.TidFraUtbetaling
            RelevantMomentTypeGodTro.UTBETALING_TILLIT -> RelevantMomentGodTro.UtbetalingTillit
            RelevantMomentTypeGodTro.ANNET -> RelevantMomentGodTro.Annet(requireNotNull(annetBegrunnelse))
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
                    requireNotNull(reduksjonMomenter) { "RelevanteMomenter kreves for God tro reduksjon" }.fraEntityRelevantMomentGodTro(),
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
