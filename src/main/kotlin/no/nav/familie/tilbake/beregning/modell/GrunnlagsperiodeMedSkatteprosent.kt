package no.nav.familie.tilbake.beregning.modell

import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.math.BigDecimal

class GrunnlagsperiodeMedSkatteprosent(
    val periode: Datoperiode,
    val tilbakekrevingsbeløp: BigDecimal,
    val skatteprosent: BigDecimal,
)
