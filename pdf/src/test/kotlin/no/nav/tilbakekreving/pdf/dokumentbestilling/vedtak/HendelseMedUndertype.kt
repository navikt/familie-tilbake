package no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak

import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype

data class HendelseMedUndertype(
    val hendelsestype: Hendelsestype,
    val hendelsesundertype: Hendelsesundertype,
)
