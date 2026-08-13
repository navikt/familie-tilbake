package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagSammenligning
import java.math.BigDecimal
import java.util.UUID

data class ForskjellEntity(
    val id: UUID,
    val faktavurderingPeriodeRef: UUID?,
    val vilkårsvurderingPeriodeRef: UUID?,
    val originalPeriode: DatoperiodeEntity,
    val endringIBeløp: BigDecimal,
) {
    fun fraEntity(): KravgrunnlagSammenligning.Forskjell.JustertBeløp {
        return KravgrunnlagSammenligning.Forskjell.JustertBeløp(
            periode = originalPeriode.fom til originalPeriode.tom,
            endringIBeløp = endringIBeløp,
        )
    }
}
