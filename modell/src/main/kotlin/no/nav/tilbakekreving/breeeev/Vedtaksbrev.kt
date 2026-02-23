package no.nav.tilbakekreving.breeeev

import no.nav.tilbakekreving.kontrakter.frontend.models.BrevmottakerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.YtelseDto

data class Vedtaksbrev(
    val brukerdata: BrevmottakerDto,
    val ytelse: YtelseDto,
    val signatur: Signatur,
    val perioder: List<BegrunnetPeriode>,
)
