package no.nav.tilbakekreving.e2e

import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.ytelsesbeløp
import java.math.BigDecimal

internal fun kravgrunnlagMedBeløp(vararg perioder: Pair<Datoperiode, BigDecimal>): KravgrunnlagHendelse {
    return kravgrunnlag(
        perioder = perioder.map { (periode, beløp) ->
            kravgrunnlagPeriode(periode = periode, ytelsesbeløp = ytelsesbeløp(tilbakekrevesBeløp = beløp))
        },
    )
}
