package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagSammenligning
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagSammenligning.ForskjellType
import java.math.BigDecimal
import java.util.UUID

data class ForskjellEntity(
    val id: UUID,
    val faktavurderingPeriodeRef: UUID?,
    val vilkårsvurderingPeriodeRef: UUID?,
    val foreldelsesvurderingPeriodeRef: UUID?,
    val type: ForskjellType,
    val originalPeriode: DatoperiodeEntity?,
    val endringIBeløp: BigDecimal?,
    val nyPeriode: DatoperiodeEntity?,
) {
    fun fraEntity(): KravgrunnlagSammenligning.Forskjell {
        return when (type) {
            ForskjellType.NyPeriode -> KravgrunnlagSammenligning.Forskjell.NyPeriode(
                nyPeriode!!.fom til nyPeriode.tom,
            )
            ForskjellType.JustertBeløp -> KravgrunnlagSammenligning.Forskjell.JustertBeløp(
                periode = originalPeriode!!.fom til originalPeriode.tom,
                endringIBeløp = endringIBeløp!!,
            )
        }
    }
}
