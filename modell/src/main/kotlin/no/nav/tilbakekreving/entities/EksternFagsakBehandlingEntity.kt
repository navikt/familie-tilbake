package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import java.time.LocalDate
import java.util.UUID

data class EksternFagsakBehandlingEntity(
    val type: EksternFagsakBehandlingType,
    val internId: UUID,
    val eksternId: String?,
    val revurderingsresultat: String?,
    val revurderingsårsak: String?,
    val begrunnelseForTilbakekreving: String?,
    val revurderingsvedtaksdato: LocalDate?,
    val utvidetPerioder: List<UtvidetPeriodeEntity>?,
) {
    fun fraEntity(): EksternFagsakBehandling {
        return when (type) {
            EksternFagsakBehandlingType.BEHANDLING -> EksternFagsakBehandling.Behandling(
                internId = internId,
                eksternId = requireNotNull(eksternId) { "eksternId kreves for EksternFagsakBehandling" },
                revurderingsresultat = requireNotNull(revurderingsresultat) { "revurderingsresultat kreves for EksternFagsakBehandling" },
                revurderingsårsak = requireNotNull(revurderingsårsak) { "revurderingsårsak kreves for EksternFagsakBehandling" },
                begrunnelseForTilbakekreving = requireNotNull(begrunnelseForTilbakekreving) { "begrunnelseForTilbakekreving kreves for EksternFagsakBehandling" },
                revurderingsvedtaksdato = requireNotNull(revurderingsvedtaksdato) { "revurderingsvedtaksdato kreves for EksternFagsakBehandling" },
                utvidetPerioder = requireNotNull(utvidetPerioder) { "utvidetPerioder kreves for EksternFagsakBehandling" }.map { it.fraEntity() },
            )
            EksternFagsakBehandlingType.UKJENT -> EksternFagsakBehandling.Ukjent(internId = internId, null)
        }
    }
}

data class UtvidetPeriodeEntity(
    val kravgrunnlagPeriode: DatoperiodeEntity,
    val vedtaksperiode: DatoperiodeEntity,
) {
    fun fraEntity(): EksternFagsakBehandling.UtvidetPeriode {
        return EksternFagsakBehandling.UtvidetPeriode(
            kravgrunnlagPeriode = kravgrunnlagPeriode.fraEntity(),
            vedtaksperiode = vedtaksperiode.fraEntity(),
        )
    }
}

enum class EksternFagsakBehandlingType {
    BEHANDLING,
    UKJENT,
}
