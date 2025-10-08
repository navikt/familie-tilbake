package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import java.time.LocalDate
import java.util.UUID

data class EksternFagsakBehandlingEntity(
    val id: UUID,
    val eksternFagsakRef: UUID,
    val type: EksternFagsakBehandlingType,
    val eksternId: String,
    val revurderingsårsak: RevurderingsårsakType?,
    val årsakTilFeilutbetaling: String?,
    val vedtaksdato: LocalDate?,
    val utvidedePerioder: List<UtvidetPeriodeEntity>?,
) {
    fun fraEntity(): EksternFagsakRevurdering {
        return when (type) {
            EksternFagsakBehandlingType.BEHANDLING -> EksternFagsakRevurdering.Revurdering(
                id = id,
                eksternId = eksternId,
                revurderingsårsak = requireNotNull(revurderingsårsak) { "årsak kreves for EksternFagsakBehandling" }.fraEntity(),
                årsakTilFeilutbetaling = requireNotNull(årsakTilFeilutbetaling) { "årsakTilFeilutbetaling kreves for EksternFagsakBehandling" },
                vedtaksdato = requireNotNull(vedtaksdato) { "vedtaksdato kreves for EksternFagsakBehandling" },
                utvidedePerioder = requireNotNull(utvidedePerioder) { "utvidetPerioder kreves for EksternFagsakBehandling" }.map { it.fraEntity() },
            )
            EksternFagsakBehandlingType.UKJENT -> EksternFagsakRevurdering.Ukjent(id = id, eksternId = eksternId, null)
        }
    }
}

data class UtvidetPeriodeEntity(
    val id: UUID,
    val eksternFagsakBehandlingRef: UUID,
    val kravgrunnlagPeriode: DatoperiodeEntity,
    val vedtaksperiode: DatoperiodeEntity,
) {
    fun fraEntity(): EksternFagsakRevurdering.UtvidetPeriode {
        return EksternFagsakRevurdering.UtvidetPeriode(
            id = id,
            kravgrunnlagPeriode = kravgrunnlagPeriode.fraEntity(),
            vedtaksperiode = vedtaksperiode.fraEntity(),
        )
    }
}

enum class RevurderingsårsakType {
    NYE_OPPLYSNINGER,
    KORRIGERING,
    KLAGE,
    UKJENT, ;

    fun fraEntity() = when (this) {
        NYE_OPPLYSNINGER -> EksternFagsakRevurdering.Revurderingsårsak.NYE_OPPLYSNINGER
        KORRIGERING -> EksternFagsakRevurdering.Revurderingsårsak.KORRIGERING
        KLAGE -> EksternFagsakRevurdering.Revurderingsårsak.KLAGE
        UKJENT -> EksternFagsakRevurdering.Revurderingsårsak.UKJENT
    }
}

enum class EksternFagsakBehandlingType {
    BEHANDLING,
    UKJENT,
}
