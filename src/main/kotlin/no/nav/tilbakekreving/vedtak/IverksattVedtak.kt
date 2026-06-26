package no.nav.tilbakekreving.vedtak

import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.tilbakekreving.entities.AktørEntity
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.TilbakekrevingsvedtakRequestDto
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.util.UUID

data class IverksattVedtak(
    val id: UUID,
    val behandlingId: UUID,
    val nyModell: Boolean,
    val vedtakId: BigInteger,
    val aktør: AktørEntity,
    val ytelsestypeKode: String,
    val kvittering: String?,
    val perioder: List<IverksattPeriode>,
    val vedtaksdato: LocalDate,
    val behandlingstype: Behandlingstype,
) {
    data class IverksattPeriode(
        val id: UUID,
        val fom: LocalDate,
        val tom: LocalDate,
        val beløpTilbakekreves: BigDecimal,
        val skattebeløp: BigDecimal,
        val rentebeløp: BigDecimal,
    ) {
        companion object {
            fun fra(request: TilbakekrevingsvedtakRequest) = request.tilbakekrevingsvedtak.tilbakekrevingsperiode.map { periode ->
                IverksattPeriode(
                    id = UUID.randomUUID(),
                    fom = periode.periode.fom,
                    tom = periode.periode.tom,
                    beløpTilbakekreves = periode.tilbakekrevingsbelop.sumOf { it.belopTilbakekreves },
                    skattebeløp = periode.tilbakekrevingsbelop.sumOf { it.belopSkatt },
                    rentebeløp = periode.belopRenter,
                )
            }

            fun fra(request: TilbakekrevingsvedtakRequestDto): List<IverksattPeriode> = request.perioder.map { periode ->
                IverksattPeriode(
                    id = UUID.randomUUID(),
                    fom = periode.periodeFom,
                    tom = periode.periodeTom,
                    beløpTilbakekreves = periode.posteringer.sumOf { it.belopTilbakekreves },
                    skattebeløp = periode.posteringer.sumOf { it.belopSkatt },
                    rentebeløp = periode.belopRenter,
                )
            }
        }
    }
}
