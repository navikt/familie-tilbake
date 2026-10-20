package no.nav.tilbakekreving.kravgrunnlag

import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.ytelsesbeløp
import java.math.BigDecimal

internal fun sammenlign(
    nåværende: List<Pair<Datoperiode, BigDecimal>>,
    oppdatert: List<Pair<Datoperiode, BigDecimal>>,
) = KravgrunnlagSammenligning(
    originaltKravgrunnlag = kravgrunnlag(perioder = nåværende.tilKravgrunnlagPerioder()),
    nyttKravgrunnlag = kravgrunnlag(perioder = oppdatert.tilKravgrunnlagPerioder()),
    sporing = Sporing("", ""),
)

internal fun sammendrag(
    nåværende: List<Pair<Datoperiode, BigDecimal>>,
    oppdatert: List<Pair<Datoperiode, BigDecimal>>,
) = sammenlign(nåværende, oppdatert).sammendrag()

internal fun List<Pair<Datoperiode, BigDecimal>>.tilKravgrunnlagPerioder(): List<KravgrunnlagHendelse.Periode> = map { (periode, beløp) ->
    kravgrunnlagPeriode(periode = periode, ytelsesbeløp = ytelsesbeløp(tilbakekrevesBeløp = beløp))
}
