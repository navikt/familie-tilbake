package no.nav.tilbakekreving.hendelse

import no.nav.tilbakekreving.aktør.Aktør
import java.time.LocalDate

data class FagsysteminfoHendelse(
    val aktør: Aktør,
    val behandlingId: String,
    val revurderingsresultat: String,
    val revurderingsårsak: String,
    val begrunnelseForTilbakekreving: String,
    val revurderingsvedtaksdato: LocalDate,
)
