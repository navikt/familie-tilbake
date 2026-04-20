package no.nav.tilbakekreving.brev.vedtaksbrev

import no.nav.tilbakekreving.breeeev.BegrunnetPeriode
import no.nav.tilbakekreving.breeeev.VedtaksbrevInfo
import no.nav.tilbakekreving.breeeev.begrunnelse.Forklaringstekster
import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler
import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler.Companion.forBegrunnelse
import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler.Companion.forPeriodeavsnitt
import no.nav.tilbakekreving.breeeev.begrunnelse.VilkårsvurderingBegrunnelse
import no.nav.tilbakekreving.breeeev.standardtekster.HjemmelForTilbakekreving
import no.nav.tilbakekreving.breeeev.standardtekster.HjemmelForTilbakekreving.Companion.formatter
import no.nav.tilbakekreving.kontrakter.frontend.models.AvsnittDto
import no.nav.tilbakekreving.kontrakter.frontend.models.BeregningsresultatVurderingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PakrevdBegrunnelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RentekstElementDto
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object BrevFormatterer {
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        .withLocale(Locale.of("nb"))
    val norskNumeriskDatoFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.of("nb"))
    val beløpFormat = NumberFormat.getCurrencyInstance(Locale.of("nb"))

    fun lagAvsnitt(periode: BegrunnetPeriode): AvsnittDto {
        return AvsnittDto(
            tittel = lagPeriodeavsnittTittel(periode.periode),
            id = periode.id,
            forklaring = Forklaringstekster.PERIODE_AVSNITT,
            meldingerTilSaksbehandler = periode.meldingerTilSaksbehandler.forPeriodeavsnitt()
                .map { it.melding },
            underavsnitt = listOf(RentekstElementDto("")) + periode.påkrevdeVurderinger.map {
                it.tilDto(periode.meldingerTilSaksbehandler.toList())
            },
        )
    }

    fun lagPeriodeavsnittTittel(periode: Datoperiode): String = "Dette er grunnen til at du har fått for mye utbetalt"

    fun lagHovedavsnittTittel(info: VedtaksbrevInfo) = when {
        info.skalTilbakekreves -> "Du må betale tilbake ${info.ytelse.ubestemtEntall}"
        else -> "Du må ikke betale tilbake ${info.ytelse.ubestemtEntall}"
    }

    fun lagHjemmelAvsnitt(hjemlerForTilbakekreving: List<HjemmelForTilbakekreving>): String {
        return "Vedtaket er gjort etter ${hjemlerForTilbakekreving.formatter()}"
    }

    fun norskDato(date: LocalDate): String = dateFormatter.format(date)

    fun norskNumeriskDato(date: LocalDate): String = norskNumeriskDatoFormatter.format(date)

    fun prosentString(int: Int?): String = "${int ?: 0}%"

    fun beløpString(beløp: Int): String = beløpFormat.format(beløp)

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

    fun BeregningsresultatVurderingDto.tilVisningstekst(): String =
        when (this) {
            BeregningsresultatVurderingDto.GodTro -> "God tro"
            BeregningsresultatVurderingDto.Uaktsomhet -> "Uaktsomhet"
            BeregningsresultatVurderingDto.GrovUaktsomhet -> "Grov uaktsomhet"
            BeregningsresultatVurderingDto.Forsett -> "Forsett"
        }
}
