package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import java.time.LocalDate

data class VarselbrevInfo(
    val ident: String,
    val navn: String,
    val språkkode: Språkkode,
    val behandlendeEnhetsNavn: String,
    val ansvarligSaksbehandler: String,
    val eksternFagsakId: String,
    val ytelseType: YtelsestypeDTO,
    val gjelderDødsfall: Boolean,
    val revurderingsvedtaksdato: LocalDate,
    val beløp: Long,
    val feilutbetaltePerioder: List<Datoperiode>,
)
