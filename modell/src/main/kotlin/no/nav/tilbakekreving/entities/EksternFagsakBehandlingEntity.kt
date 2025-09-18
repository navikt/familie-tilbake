package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import java.time.LocalDate
import java.util.UUID

data class EksternFagsakBehandlingEntity(
    val type: EksternFagsakBehandlingType,
    val internId: UUID,
    val eksternId: String?,
    val revurderingsårsak: RevurderingsårsakType?,
    val årsakTilFeilutbetaling: String?,
    val vedtaksdato: LocalDate?,
    val utvidedePerioder: List<UtvidetPeriodeEntity>?,
) {
    fun fraEntity(): EksternFagsakRevurdering {
        return when (type) {
            EksternFagsakBehandlingType.BEHANDLING -> EksternFagsakRevurdering.Revurdering(
                internId = internId,
                eksternId = requireNotNull(eksternId) { "eksternId kreves for EksternFagsakBehandling" },
                revurderingsårsak = requireNotNull(revurderingsårsak) { "årsak kreves for EksternFagsakBehandling" }.fraEntity(),
                årsakTilFeilutbetaling = requireNotNull(årsakTilFeilutbetaling) { "årsakTilFeilutbetaling kreves for EksternFagsakBehandling" },
                vedtaksdato = requireNotNull(vedtaksdato) { "vedtaksdato kreves for EksternFagsakBehandling" },
                utvidedePerioder = requireNotNull(utvidedePerioder) { "utvidetPerioder kreves for EksternFagsakBehandling" }.map { it.fraEntity() },
            )
            EksternFagsakBehandlingType.UKJENT -> EksternFagsakRevurdering.Ukjent(internId = internId, null)
        }
    }
}

data class UtvidetPeriodeEntity(
    val kravgrunnlagPeriode: DatoperiodeEntity,
    val vedtaksperiode: DatoperiodeEntity,
) {
    fun fraEntity(): EksternFagsakRevurdering.UtvidetPeriode {
        return EksternFagsakRevurdering.UtvidetPeriode(
            kravgrunnlagPeriode = kravgrunnlagPeriode.fraEntity(),
            vedtaksperiode = vedtaksperiode.fraEntity(),
        )
    }
}

enum class RevurderingsårsakType(private val modellType: EksternFagsakRevurdering.Revurderingsårsak) {
    NYE_OPPLYSNINGER(EksternFagsakRevurdering.Revurderingsårsak.NYE_OPPLYSNINGER),
    KORRIGERING(EksternFagsakRevurdering.Revurderingsårsak.KORRIGERING),
    KLAGE(EksternFagsakRevurdering.Revurderingsårsak.KLAGE),
    UKJENT(EksternFagsakRevurdering.Revurderingsårsak.UKJENT),
    ;

    fun fraEntity() = modellType
}

enum class EksternFagsakBehandlingType {
    BEHANDLING,
    UKJENT,
}
