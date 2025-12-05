package no.nav.tilbakekreving.integrasjoner.dokarkiv.domain

import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.DokumentKlasse

class ArkivDokument(
    val tittel: String?,
    val brevkode: String?,
    val dokumentKategori: DokumentKlasse?,
    val dokumentvarianter: List<ArkivDokumentvariant>,
)
