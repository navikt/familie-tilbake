package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse.Periode
import java.math.BigDecimal

data class KravgrunnlagPeriodeEntity(
    val periode: DatoperiodeEntity,
    val månedligSkattebeløp: BigDecimal,
    val ytelsesbeløp: List<BeløpEntity>,
    val feilutbetaltBeløp: List<BeløpEntity>,
) {
    fun fraEntity(): Periode {
        return Periode(
            periode = periode.fraEntity(),
            månedligSkattebeløp = månedligSkattebeløp,
            ytelsesbeløp = ytelsesbeløp.map { it.fraEntity() },
            feilutbetaltBeløp = feilutbetaltBeløp.map { it.fraEntity() },
        )
    }
}
