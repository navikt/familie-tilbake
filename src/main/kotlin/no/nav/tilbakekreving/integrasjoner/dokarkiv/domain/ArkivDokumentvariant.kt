package no.nav.tilbakekreving.integrasjoner.dokarkiv.domain

class ArkivDokumentvariant(
    val filtype: String,
    val variantformat: String,
    val fysiskDokument: ByteArray,
    val filnavn: String?,
)
