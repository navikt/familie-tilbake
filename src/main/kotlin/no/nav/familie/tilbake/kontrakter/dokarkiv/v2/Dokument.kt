package no.nav.familie.tilbake.kontrakter.dokarkiv.v2

import jakarta.validation.constraints.NotEmpty
import no.nav.familie.tilbake.kontrakter.dokarkiv.Dokumenttype

class Dokument(
    @field:NotEmpty val dokument: ByteArray,
    @field:NotEmpty val filtype: Filtype,
    val filnavn: String? = null,
    val tittel: String? = null,
    val dokumenttype: Dokumenttype? = null,
)
