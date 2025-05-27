package no.nav.tilbakekreving.entities

import java.time.LocalDate

data class ForeldelsesvurderingEntity(
    val type: String,
    val begrunnelse: String? = null,
    val frist: LocalDate? = null,
    val oppdaget: LocalDate? = null,
)
