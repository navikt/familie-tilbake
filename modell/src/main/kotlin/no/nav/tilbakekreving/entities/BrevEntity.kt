package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.brev.Brev
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import java.util.UUID

data class BrevEntity(
    val id: UUID,
    val tilbakekrevingRef: String,
    val brevType: Brevtype,
    val kravgrunnlagRef: HistorikkReferanseEntity<UUID>,
    val varselbrevEntity: VarselbrevEntity?,
) {
    fun fraEntity(kravgrunnlagHistorikk: KravgrunnlagHistorikk): Brev {
        val sporing = Sporing("Ukjent", id.toString())
        return when (brevType) {
            Brevtype.VARSEL_BREV -> {
                varselbrevEntity!!.fraEntity(
                    id,
                    kravgrunnlagHistorikk.finn(kravgrunnlagRef.id, sporing),
                )
            }
        }
    }
}

enum class Brevtype {
    VARSEL_BREV,
}
