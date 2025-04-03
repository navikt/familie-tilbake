package no.nav.tilbakekreving.eksternfagsak

import no.nav.tilbakekreving.historikk.Historikk
import java.time.LocalDate
import java.util.UUID

class EksternFagsakBehandling(
    override val internId: UUID,
    internal val eksternId: String,
    val revurderingsresultat: String,
    val revurderingsårsak: String,
    val begrunnelseForTilbakekreving: String,
    val revurderingsvedtaksdato: LocalDate,
) : Historikk.HistorikkInnslag<UUID>
