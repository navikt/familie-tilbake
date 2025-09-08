package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonSærligeGrunner
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunn

data class VurdertAktsomhetEntity(
    val aktsomhetType: AktsomhetType,
    val begrunnelse: String,
    val skalIleggesRenter: Boolean?,
    val særligGrunner: SærligeGrunnerEntity?,
) {
    fun tilAktsomhet(): NivåAvForståelse.Aktsomhet {
        return when (aktsomhetType) {
            AktsomhetType.SIMPEL_UAKTSOMHET -> NivåAvForståelse.Aktsomhet.Uaktsomhet(
                kanUnnlates4XRettsgebyr = KanUnnlates4xRettsgebyr.ErOver4xRettsgebyr(
                    requireNotNull(særligGrunner) { "SærligGrunner kreves for Uaktsomhet" }.fraEntity(),
                ),
                begrunnelse = begrunnelse,
            )
            AktsomhetType.GROV_UAKTSOMHET -> NivåAvForståelse.Aktsomhet.GrovUaktsomhet(
                reduksjonSærligeGrunner = requireNotNull(særligGrunner) { "SærligGrunner kreves for GrovUaktsomhet" }.fraEntity(),
                begrunnelse = begrunnelse,
            )
            AktsomhetType.FORSETT -> NivåAvForståelse.Aktsomhet.Forsett(
                begrunnelse = begrunnelse,
            )

            AktsomhetType.IKKE_UTVIST_SKYLD -> NivåAvForståelse.Aktsomhet.IkkeUtvistSkyld(
                kanUnnlates4XRettsgebyr = KanUnnlates4xRettsgebyr.ErOver4xRettsgebyr(
                    requireNotNull(særligGrunner) { "IkkeUtvistSkyld kreves for Uaktsomhet" }.fraEntity(),
                ),
                begrunnelse = begrunnelse,
            )
        }
    }
}

data class SærligeGrunnerEntity(
    val begrunnelse: String,
    val grunner: List<SærligGrunn>,
    val skalReduseres: SkalReduseresEntity,
) {
    fun fraEntity(): ReduksjonSærligeGrunner = ReduksjonSærligeGrunner(
        begrunnelse = begrunnelse,
        grunner = grunner.toSet(),
        skalReduseres = skalReduseres.fraEntity(),
    )
}

enum class AktsomhetType {
    SIMPEL_UAKTSOMHET,
    GROV_UAKTSOMHET,
    FORSETT,
    IKKE_UTVIST_SKYLD,
}
