package no.nav.familie.tilbake.behandling.domain

import org.springframework.data.relational.core.mapping.Column

data class Institusjon(
    @Column("institusjon_org_nr")
    val organisasjonsnummer: String,
    @Column("institusjon_navn")
    val navn: String
)
