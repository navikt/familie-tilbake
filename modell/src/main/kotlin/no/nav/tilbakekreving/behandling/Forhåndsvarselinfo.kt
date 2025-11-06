package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.saksbehandler.Behandler
import java.time.LocalDate

data class Forhåndsvarselinfo(
    val behandlendeEnhetNavn: String,
    val ansvarligSaksbehandler: Behandler,
    val beløp: Long,
    val feilutbetaltePerioder: List<Datoperiode>,
    val revurderingsvedtaksdato: LocalDate,
)
