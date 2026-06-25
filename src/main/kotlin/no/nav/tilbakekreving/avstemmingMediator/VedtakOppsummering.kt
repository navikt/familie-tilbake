package no.nav.tilbakekreving.avstemmingMediator

import no.nav.tilbakekreving.vedtak.IverksattVedtak
import java.math.BigDecimal

class VedtakOppsummering(
    val økonomivedtakId: String,
    val tilbakekrevesBruttoUtenRenter: BigDecimal,
    val tilbakekrevesNettoUtenRenter: BigDecimal,
    val renter: BigDecimal,
    val skatt: BigDecimal,
) {
    fun harIngenTilbakekreving(): Boolean = tilbakekrevesBruttoUtenRenter.signum() == 0

    companion object {
        fun oppsummer(iverksattVedtak: IverksattVedtak): VedtakOppsummering {
            var bruttoUtenRenter = BigDecimal.ZERO
            var renter = BigDecimal.ZERO
            var skatt = BigDecimal.ZERO
            for (periode in iverksattVedtak.perioder) {
                renter = renter.add(periode.rentebeløp)
                bruttoUtenRenter = bruttoUtenRenter.add(periode.beløpTilbakekreves)
                skatt = skatt.add(periode.skattebeløp)
            }
            return VedtakOppsummering(
                økonomivedtakId = iverksattVedtak.vedtakId.toString(),
                tilbakekrevesBruttoUtenRenter = bruttoUtenRenter,
                tilbakekrevesNettoUtenRenter = bruttoUtenRenter.subtract(skatt),
                renter = renter,
                skatt = skatt,
            )
        }
    }
}
