package no.nav.tilbakekreving.integrasjoner.dokarkiv.domain

class Dokumentvariant(
    val filtype: String,
    val variantformat: String,
    val fysiskDokument: ByteArray,
    val filnavn: String?,
)
