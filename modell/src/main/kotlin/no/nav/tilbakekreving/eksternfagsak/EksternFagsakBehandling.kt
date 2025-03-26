package no.nav.tilbakekreving.eksternfagsak

import no.nav.tilbakekreving.historikk.Historikk
import java.util.UUID

class EksternFagsakBehandling(
    override val internId: UUID,
    internal val eksternId: String,
) : Historikk.HistorikkInnslag<UUID>
