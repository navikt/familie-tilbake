package no.nav.tilbakekreving.hendelse

import java.time.LocalDate

data class FagsysteminfoHendelse(
    val ident: String,
    val behandlingId: String,
    val revurderingsresultat: String,
    val revurderingsårsak: String,
    val begrunnelseForTilbakekreving: String,
    val revurderingsvedtaksdato: LocalDate,
)
