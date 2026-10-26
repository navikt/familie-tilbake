package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.hendelse.KravHendelse
import java.util.UUID

sealed class KravHendelseEntity : HistorikkInnslagEntity<UUID> {
    abstract val tilbakekrevingRef: String

    abstract fun fraEntity(): KravHendelse
}
