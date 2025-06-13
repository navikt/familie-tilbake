package no.nav.tilbakekreving.eksternfagsak

import no.nav.tilbakekreving.entities.EksternFagsakBehandlingEntity
import no.nav.tilbakekreving.entities.EksternFagsakBehandlingType
import no.nav.tilbakekreving.historikk.Historikk
import java.time.LocalDate
import java.util.UUID

sealed class EksternFagsakBehandling(
    override val internId: UUID,
) : Historikk.HistorikkInnslag<UUID> {
    internal abstract val eksternId: String
    abstract val revurderingsresultat: String
    abstract val revurderingsårsak: String
    abstract val begrunnelseForTilbakekreving: String
    abstract val revurderingsvedtaksdato: LocalDate

    class Behandling(
        internId: UUID,
        override val eksternId: String,
        override val revurderingsresultat: String,
        override val revurderingsårsak: String,
        override val begrunnelseForTilbakekreving: String,
        override val revurderingsvedtaksdato: LocalDate,
    ) : EksternFagsakBehandling(internId)

    class Ukjent(
        internId: UUID,
        val revurderingsdatoFraKravgrunnlag: LocalDate?,
    ) : EksternFagsakBehandling(internId) {
        override val eksternId: String = "Ukjent"
        override val revurderingsresultat: String = "Ukjent"
        override val revurderingsårsak: String = "Ukjent - finn i fagsystem"
        override val begrunnelseForTilbakekreving: String = "Ukjent - finn i fagsystem"
        override val revurderingsvedtaksdato: LocalDate = revurderingsdatoFraKravgrunnlag ?: LocalDate.MIN
    }

    fun tilEntity(): EksternFagsakBehandlingEntity {
        return when (this) {
            is Behandling -> EksternFagsakBehandlingEntity(
                type = EksternFagsakBehandlingType.BEHANDLING,
                internId = internId,
                eksternId = eksternId,
                revurderingsresultat = revurderingsresultat,
                revurderingsårsak = revurderingsårsak,
                begrunnelseForTilbakekreving = begrunnelseForTilbakekreving,
                revurderingsvedtaksdato = revurderingsvedtaksdato,
            )
            is Ukjent -> EksternFagsakBehandlingEntity(
                type = EksternFagsakBehandlingType.UKJENT,
                null,
                null,
                null,
                null,
                null,
                null,
            )
        }
    }
}
