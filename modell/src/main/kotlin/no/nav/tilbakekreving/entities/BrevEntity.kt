package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.brev.Brev
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import java.time.LocalDateTime
import java.util.UUID

data class BrevEntity(
    val id: UUID,
    val tilbakekrevingRef: String,
    val brevtype: Brevtype,
    val varselbrevEntity: VarselbrevEntity?,
    val vedtaksbrevEntity: VedtaksbrevEntity?,
    val opprettet: LocalDateTime,
) {
    fun fraEntity(kravgrunnlagHistorikk: KravgrunnlagHistorikk): Brev {
        return when (brevtype) {
            Brevtype.VARSELBREV, Brevtype.VARSEL_BREV -> {
                varselbrevEntity!!.fraEntity(
                    id,
                    kravgrunnlagHistorikk,
                    opprettet,
                )
            }
            Brevtype.VEDTAKSBREV -> {
                vedtaksbrevEntity!!.fraEntity(id, opprettet)
            }
        }
    }
}

enum class Brevtype {
    @Deprecated("midreltidig, fjernes etter prodsatt og migrering")
    VARSEL_BREV,
    VARSELBREV,
    VEDTAKSBREV,
}
