package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.SærligGrunn
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonSærligeGrunner
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType

data class VurdertAktsomhetEntity(
    val aktsomhetType: AktsomhetType,
    val begrunnelse: String,
    val skalIleggesRenter: Boolean?,
    val særligGrunner: SærligeGrunnerEntity?,
    val kanUnnlates: KanUnnlates4xRettsgebyr.KanUnnlates,
) {
    fun tilAktsomhet(): NivåAvForståelse.Aktsomhet {
        return when (aktsomhetType) {
            AktsomhetType.SIMPEL_UAKTSOMHET -> {
                NivåAvForståelse.Aktsomhet.Uaktsomhet(
                    begrunnelse = begrunnelse,
                    kanUnnlates4XRettsgebyr = when (kanUnnlates) {
                        KanUnnlates4xRettsgebyr.KanUnnlates.Ja -> KanUnnlates4xRettsgebyr.Unnlates
                        KanUnnlates4xRettsgebyr.KanUnnlates.Nei -> KanUnnlates4xRettsgebyr.ErOver4xRettsgebyr(
                            requireNotNull(særligGrunner) { "SærligGrunner kreves for Uaktsomhet" }.fraEntity(),
                        )
                    },
                )
            }
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

enum class AktsomhetType {
    SIMPEL_UAKTSOMHET,
    GROV_UAKTSOMHET,
    FORSETT,
    IKKE_UTVIST_SKYLD,
}
