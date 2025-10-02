package no.nav.familie.tilbake.kontrakter.dokarkiv.v2

import jakarta.validation.constraints.NotEmpty
import no.nav.tilbakekreving.integrasjoner.dokarkiv.domain.Dokumenttype

class Dokument(
    @field:NotEmpty val dokument: ByteArray,
    @field:NotEmpty val filtype: Filtype,
    val filnavn: String? = null,
    val tittel: String? = null,
    @field:NotEmpty val dokumenttype: Dokumenttype,
)
