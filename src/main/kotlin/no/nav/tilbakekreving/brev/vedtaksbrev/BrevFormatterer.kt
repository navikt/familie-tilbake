package no.nav.tilbakekreving.brev.vedtaksbrev

import no.nav.tilbakekreving.breeeev.BegrunnetPeriode
import no.nav.tilbakekreving.kontrakter.frontend.models.AvsnittDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RentekstElementDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UnderavsnittElementDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID

object BrevFormatterer {
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        .withLocale(Locale.of("nb"))

    fun lagAvsnitt(perioder: List<BegrunnetPeriode>): List<AvsnittDto> {
        val startPeriode = perioder.minOf { it.periode.fom }
        val sluttPeriode = perioder.maxOf { it.periode.tom }
        return listOf(
            AvsnittDto(
                tittel = "Perioden fra og med ${norskDato(startPeriode)} til og med ${norskDato(sluttPeriode)}",
                id = UUID.randomUUID(),
                underavsnitt = listOf(RentekstElementDto("")) + perioder.first().påkrevdeVurderinger.map {
                    UnderavsnittElementDto(
                        tittel = it.tittel,
                        underavsnitt = listOf(RentekstElementDto("")),
                    )
                },
            ),
        )
    }

    fun norskDato(date: LocalDate): String = dateFormatter.format(date)
}
