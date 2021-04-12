package no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto

import no.nav.familie.tilbake.common.Rettsgebyr
import java.math.BigDecimal

@Suppress("unused") // Handlebars
class HbKonfigurasjon(val fireRettsgebyr: BigDecimal = BigDecimal.valueOf(Rettsgebyr.BELØP * 4L),
                      val halvtGrunnbeløp: BigDecimal = BigDecimal.valueOf(49929), //TODO fjerne hardkoding,
                      val klagefristIUker: Int)
