package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.historikk.Historikk
import java.time.LocalDate
import java.util.UUID

sealed interface Brev : Historikk.HistorikkInnslag<UUID> {
    val opprettetDato: LocalDate
}
