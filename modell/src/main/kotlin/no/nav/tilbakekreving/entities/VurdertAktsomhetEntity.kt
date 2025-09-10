package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.SærligGrunn
import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType

data class VurdertAktsomhetEntity(
    val aktsomhetType: AktsomhetType,
    val begrunnelse: String,
    val skalIleggesRenter: Boolean?,
    val skalReduseres: SkalReduseresEntity?,
    val særligGrunner: SærligeGrunnerEntity?,
) {
    fun fraEntity(): Vilkårsvurderingsteg.VurdertAktsomhet {
        return when (aktsomhetType) {
            AktsomhetType.SIMPEL_UAKTSOMHET -> {
                Vilkårsvurderingsteg.VurdertAktsomhet.SimpelUaktsomhet(
                    begrunnelse = begrunnelse,
                    særligeGrunner = requireNotNull(særligGrunner) { "SærligGrunner kreves for SimpelUaktsomhet" }.fraEntity(),
                    skalReduseres = requireNotNull(skalReduseres) { "skalReduseres kreves for SimpelUaktsomhet" }.fraEntity(),
                )
            }
            AktsomhetType.GROV_UAKTSOMHET -> {
                Vilkårsvurderingsteg.VurdertAktsomhet.GrovUaktsomhet(
                    begrunnelse = begrunnelse,
                    særligeGrunner = requireNotNull(særligGrunner) { "SærligGrunner kreves for GrovUaktsomhet" }.fraEntity(),
                    skalReduseres = requireNotNull(skalReduseres) { "skalReduseres kreves for GrovUaktsomhet" }.fraEntity(),
                    skalIleggesRenter = requireNotNull(skalIleggesRenter) { "skalIleggesRenter kreves for GrovUaktsomhet" },
                )
            }
            AktsomhetType.FORSETT -> {
                Vilkårsvurderingsteg.VurdertAktsomhet.Forsett(
                    begrunnelse = begrunnelse,
                    skalIleggesRenter = requireNotNull(skalIleggesRenter) { "skalIleggesRenter kreves for FORSETT" },
                )
            }
        }
    }
}

data class SærligeGrunnerEntity(
    val begrunnelse: String,
    val grunner: List<SærligGrunnEntity>,
) {
    fun fraEntity(): Vilkårsvurderingsteg.VurdertAktsomhet.SærligeGrunner =
        Vilkårsvurderingsteg.VurdertAktsomhet.SærligeGrunner(
            begrunnelse = begrunnelse,
            grunner = grunner.map { it.fraEntity() }.toSet(),
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

enum class AktsomhetType {
    SIMPEL_UAKTSOMHET,
    GROV_UAKTSOMHET,
    FORSETT,
}
