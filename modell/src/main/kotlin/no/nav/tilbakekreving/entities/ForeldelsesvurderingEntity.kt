package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg.Vurdering
import java.time.LocalDate

data class ForeldelsesvurderingEntity(
    val type: ForeldelsesvurderingType,
    val begrunnelse: String? = null,
    val frist: LocalDate? = null,
    val oppdaget: LocalDate? = null,
) {
    fun fraEntity(): Vurdering {
        return when (type) {
            ForeldelsesvurderingType.IKKE_FORELDET -> {
                if (begrunnelse != null) {
                    Vurdering.IkkeForeldet(begrunnelse)
                } else {
                    throw IllegalArgumentException("Begrunnelse kreves for IKKE_FORELDET")
                }
            }
            ForeldelsesvurderingType.TILLEGGSFRIST -> {
                if (frist != null && oppdaget != null) {
                    Vurdering.Tilleggsfrist(frist, oppdaget)
                } else {
                    throw IllegalArgumentException("Frist og oppdaget kreves for TILLEGGSFRIST")
                }
            }
            ForeldelsesvurderingType.FORELDET -> {
                if (begrunnelse != null) {
                    Vurdering.Foreldet(begrunnelse)
                } else {
                    throw IllegalArgumentException("Begrunnelse kreves for FORELDET")
                }
            }
            ForeldelsesvurderingType.IKKE_VURDERT -> Vurdering.IkkeVurdert
        }
    }
}

enum class ForeldelsesvurderingType {
    IKKE_FORELDET,
    TILLEGGSFRIST,
    FORELDET,
    IKKE_VURDERT,
}
