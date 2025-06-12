package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse.Periode
import java.math.BigDecimal

@Serializable
data class KravgrunnlagPeriodeEntity(
    val periode: DatoperiodeEntity,
    val månedligSkattebeløp: String,
    val ytelsesbeløp: List<BeløpEntity>,
    val feilutbetaltBeløp: List<BeløpEntity>,
) {
    fun fraEntity(): Periode {
        return Periode(
            periode = periode.fraEntity(),
            månedligSkattebeløp = BigDecimal(månedligSkattebeløp),
            ytelsesbeløp = ytelsesbeløp.map { it.fraEntity() },
            feilutbetaltBeløp = feilutbetaltBeløp.map { it.fraEntity() },
        )
    }
}
