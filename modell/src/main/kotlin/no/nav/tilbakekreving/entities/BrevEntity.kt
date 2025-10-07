package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.brev.Brev
import no.nav.tilbakekreving.brev.Varselbrev
import java.time.LocalDate
import java.util.UUID

data class BrevEntity(
    val brevType: Brevtype,
    val id: UUID,
    val opprettetDato: LocalDate,
    val brevInformasjonEntity: BrevInformasjonEntity,
    val varsletBeløp: Long?,
    val revurderingsvedtaksdato: LocalDate?,
    val fristdatoForTilbakemelding: LocalDate?,
    val varseltekstFraSaksbehandler: String?,
    val feilutbetaltePerioder: List<DatoperiodeEntity>?,
    val journalpostId: String?,
) {
    fun fraEntity(): Brev {
        return when (brevType) {
            Brevtype.VARSEL_BREV -> Varselbrev(
                id = requireNotNull(id) { "Id kreves for Brev" },
                opprettetDato = requireNotNull(opprettetDato) { "opprettetDato kreves for Brev" },
                brevInformasjon = requireNotNull(brevInformasjonEntity.tilBrevinformasjon()) { "brevmetadata kreves for Brev" },
                journalpostId = journalpostId,
                varsletBeløp = requireNotNull(varsletBeløp) { "beløp kreves for Brev" },
                revurderingsvedtaksdato = requireNotNull(revurderingsvedtaksdato) { "revurderingsvedtaksdato kreves for Brev" },
                fristdatoForTilbakemelding = requireNotNull(fristdatoForTilbakemelding) { "fristdatoForTilbakemelding kreves for Brev" },
                varseltekstFraSaksbehandler = requireNotNull(varseltekstFraSaksbehandler) { "varseltekstFraSaksbehandler kreves for Brev" },
                feilutbetaltePerioder = requireNotNull(feilutbetaltePerioder!!.map { it.fraEntity() }) { "feilutbetaltePerioder kreves for Brev" },
            )
        }
    }
}

enum class Brevtype {
    VARSEL_BREV,
}
