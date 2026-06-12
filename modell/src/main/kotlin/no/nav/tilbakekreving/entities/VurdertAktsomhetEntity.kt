package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.SærligGrunn
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonSærligeGrunner
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

enum class KanUnnlates {
    UNNLATES,
    SKAL_IKKE_UNNLATES,
    OVER_4_RETTSGEBYR,
    ;

    fun fraEntity(særligeGrunner: SærligeGrunnerEntity?): KanUnnlates4xRettsgebyr = when (this) {
        UNNLATES -> KanUnnlates4xRettsgebyr.Unnlates
        SKAL_IKKE_UNNLATES -> KanUnnlates4xRettsgebyr.SkalIkkeUnnlates(
            requireNotNull(særligeGrunner) { "SærligGrunner kreves for SKAL_IKKE_UNNLATES" }.fraEntity(),
        )
        OVER_4_RETTSGEBYR -> KanUnnlates4xRettsgebyr.ErOver4xRettsgebyr(
            requireNotNull(særligeGrunner) { "SærligGrunner kreves for OVER_4_RETTSGEBYR" }.fraEntity(),
        )
    }
}

enum class AktsomhetType {
    SIMPEL_UAKTSOMHET,
    GROV_UAKTSOMHET,
    FORSETT,
    IKKE_UTVIST_SKYLD,
}
