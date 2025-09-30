package no.nav.tilbakekreving.integrasjoner.dokarkiv.domain

class ArkivDokument(
    val tittel: String? = null,
    val brevkode: String? = null,
    val dokumentKategori: Dokumentkategori? = null,
    val dokumentvarianter: List<Dokumentvariant> = ArrayList(),
)
