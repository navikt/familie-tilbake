package no.nav.tilbakekreving.brev.vedtaksbrev

import no.nav.tilbakekreving.breeeev.BegrunnetPeriode
import no.nav.tilbakekreving.breeeev.begrunnelse.Forklaringstekster
import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler
import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler.Companion.forBegrunnelse
import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler.Companion.forPeriodeavsnitt
import no.nav.tilbakekreving.breeeev.begrunnelse.VilkårsvurderingBegrunnelse
import no.nav.tilbakekreving.kontrakter.frontend.models.AvsnittDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PakrevdBegrunnelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RentekstElementDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID

object BrevFormatterer {
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        .withLocale(Locale.of("nb"))

    fun lagAvsnitt(perioder: List<BegrunnetPeriode>): List<AvsnittDto> {
        val periode = perioder.first()
        return listOf(
            AvsnittDto(
                tittel = "Dette er grunnen til at du har fått for mye utbetalt",
                id = UUID.randomUUID(),
                forklaring = Forklaringstekster.PERIODE_AVSNITT,
                meldingerTilSaksbehandler = periode.meldingerTilSaksbehandler.forPeriodeavsnitt()
                    .map { it.melding },
                underavsnitt = listOf(RentekstElementDto("")) + periode.påkrevdeVurderinger.map {
                    it.tilDto(periode.meldingerTilSaksbehandler.toList())
                },
            ),
        )
    }

    fun norskDato(date: LocalDate): String = dateFormatter.format(date)

    fun VilkårsvurderingBegrunnelse.tilDto(
        meldingerTilSaksbehandler: List<MeldingTilSaksbehandler> = emptyList(),
        underavsnitt: List<RentekstElementDto> = listOf(RentekstElementDto("")),
    ): PakrevdBegrunnelseDto {
        return PakrevdBegrunnelseDto(
            tittel = tittel,
            forklaring = forklaring,
            begrunnelseType = name,
            meldingerTilSaksbehandler = meldingerTilSaksbehandler.forBegrunnelse(this)
                .map { it.melding },
            underavsnitt = underavsnitt,
        )
    }
}
