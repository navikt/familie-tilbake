package no.nav.tilbakekreving.integrasjoner.dokarkiv.domain

import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.Dokumentklass

class ArkivDokument(
    val tittel: String?,
    val brevkode: String?,
    val dokumentKategori: Dokumentklass?,
    val dokumentvarianter: List<ArkivDokumentvariant>,
)
