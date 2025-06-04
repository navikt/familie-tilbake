package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Vilkårsvurderingsteg
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunn

sealed class VurdertAktsomhetEntity {
    abstract val begrunnelse: String
    abstract val skalIleggesRenter: Boolean
    abstract val skalReduseres: SkalReduseresEntity
    abstract val vurderingstype: String
    abstract val type: String

    abstract fun fraEntity(): Vilkårsvurderingsteg.VurdertAktsomhet
}

data class SimpelUaktsomhetEntity(
    override val begrunnelse: String,
    override val skalReduseres: SkalReduseresEntity,
    val særligGrunner: SærligeGrunnerEntity?,
) : VurdertAktsomhetEntity() {
    override val type = "SimpelUaktsomhet"
    override val vurderingstype: String = "SimpelUaktsomhet"
    override val skalIleggesRenter: Boolean = false

    override fun fraEntity(): Vilkårsvurderingsteg.VurdertAktsomhet {
        return Vilkårsvurderingsteg.VurdertAktsomhet.SimpelUaktsomhet(
            begrunnelse = begrunnelse,
            særligeGrunner = særligGrunner!!.fraEntity(),
            skalReduseres = skalReduseres.fraEntity(),
        )
    }
}

data class GrovUaktsomhetEntity(
    override val begrunnelse: String,
    override val skalReduseres: SkalReduseresEntity,
    override val skalIleggesRenter: Boolean,
    val særligGrunner: SærligeGrunnerEntity?,
) : VurdertAktsomhetEntity() {
    override val vurderingstype: String = "GrovUaktsomhet"
    override val type: String = "GrovUaktsomhet"

    override fun fraEntity(): Vilkårsvurderingsteg.VurdertAktsomhet {
        return Vilkårsvurderingsteg.VurdertAktsomhet.GrovUaktsomhet(
            begrunnelse = begrunnelse,
            særligeGrunner = særligGrunner!!.fraEntity(),
            skalReduseres = skalReduseres.fraEntity(),
            skalIleggesRenter = skalIleggesRenter,
        )
    }
}

data class ForsettEntity(
    override val begrunnelse: String,
    override val skalIleggesRenter: Boolean,
) : VurdertAktsomhetEntity() {
    override val skalReduseres: SkalReduseresEntity = NeiEntitySkalReduseres
    override val vurderingstype: String = "Forsett"
    override val type: String = "Forsett"

    override fun fraEntity(): Vilkårsvurderingsteg.VurdertAktsomhet {
        return Vilkårsvurderingsteg.VurdertAktsomhet.Forsett(
            begrunnelse = begrunnelse,
            skalIleggesRenter = skalIleggesRenter,
        )
    }
}

data class SærligeGrunnerEntity(
    val begrunnelse: String,
    val grunner: List<String>,
) {
    fun fraEntity(): Vilkårsvurderingsteg.VurdertAktsomhet.SærligeGrunner =
        Vilkårsvurderingsteg.VurdertAktsomhet.SærligeGrunner(
            begrunnelse = begrunnelse,
            grunner = grunner.map { SærligGrunn.valueOf(it) }.toSet(),
        )
}
