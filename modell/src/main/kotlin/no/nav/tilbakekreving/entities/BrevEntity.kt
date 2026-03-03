package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.brev.Brev
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import java.util.UUID

data class BrevEntity(
    val id: UUID,
    val tilbakekrevingRef: String,
    val brevtype: Brevtype,
    val varselbrevEntity: VarselbrevEntity?,
    val vedtaksbrevEntity: VedtaksbrevEntity?,
) {
    fun fraEntity(kravgrunnlagHistorikk: KravgrunnlagHistorikk): Brev {
        return when (brevtype) {
            Brevtype.VARSELBREV, Brevtype.VARSEL_BREV -> {
                varselbrevEntity!!.fraEntity(
                    id,
                    brevtype,
                    kravgrunnlagHistorikk,
                )
            }
            Brevtype.VEDTAKSBREV -> {
                vedtaksbrevEntity!!.fraEntity(id, brevtype)
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
