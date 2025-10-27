package no.nav.tilbakekreving.integrasjoner.dokarkiv.domain

class ArkivDokument(
    val tittel: String?,
    val brevkode: String?,
    val dokumentKategori: Dokumentkategori?,
    val dokumentvarianter: List<Dokumentvariant>,
)
