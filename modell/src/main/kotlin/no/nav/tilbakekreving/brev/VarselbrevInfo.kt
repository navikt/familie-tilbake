package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.aktør.Brukerinfo
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import java.time.LocalDate

data class VarselbrevInfo(
    val brukerinfo: Brukerinfo,
    val behandlendeEnhetsNavn: String,
    val ansvarligSaksbehandler: String,
    val eksternFagsakId: String,
    val ytelseType: YtelsestypeDTO,
    val revurderingsvedtaksdato: LocalDate,
    val beløp: Long,
    val feilutbetaltePerioder: List<Datoperiode>,
)
