package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse.Periode
import java.math.BigDecimal
import java.util.UUID

data class KravgrunnlagPeriodeEntity(
    val id: UUID,
    val kravgrunnlagId: UUID,
    val periode: DatoperiodeEntity,
    val månedligSkattebeløp: BigDecimal,
    val beløp: List<BeløpEntity>,
) {
    fun fraEntity(): Periode {
        return Periode(
            id = id,
            periode = periode.fraEntity(),
            månedligSkattebeløp = månedligSkattebeløp,
            beløp = beløp.map { it.fraEntity() },
        )
    }
}
