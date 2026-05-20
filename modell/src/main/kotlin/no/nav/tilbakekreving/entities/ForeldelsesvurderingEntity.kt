package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg.Vurdering
import java.time.LocalDate

data class ForeldelsesvurderingEntity(
    val type: ForeldelsesvurderingType,
    val begrunnelse: String?,
    val frist: LocalDate?,
    val oppdaget: LocalDate?,
) {
    fun fraEntity(): Vurdering {
        return when (type) {
            ForeldelsesvurderingType.IKKE_FORELDET -> {
                Vurdering.IkkeForeldet(requireNotNull(begrunnelse) { "Begrunnelse kreves for IKKE_FORELDET" })
            }
            ForeldelsesvurderingType.TILLEGGSFRIST -> {
                Vurdering.Tilleggsfrist(
                    requireNotNull(frist) { "Frist kreves for TILLEGGSFRIST" },
                    requireNotNull(oppdaget) { "oppdaget kreves for TILLEGGSFRIST" },
                )
            }
            ForeldelsesvurderingType.FORELDET -> {
                Vurdering.Foreldet(requireNotNull(begrunnelse) { "Begrunnelse kreves for FORELDET" })
            }
            ForeldelsesvurderingType.IKKE_VURDERT -> Vurdering.IkkeVurdert
            ForeldelsesvurderingType.AUTOMATISK_IKKE_FORELDET -> Vurdering.AutomatiskIkkeForeldet
        }
    }
}

enum class ForeldelsesvurderingType {
    IKKE_FORELDET,
    TILLEGGSFRIST,
    FORELDET,
    IKKE_VURDERT,
    AUTOMATISK_IKKE_FORELDET,
}
