package no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto

import no.nav.familie.tilbake.config.Constants
import java.math.BigDecimal

@Suppress("unused") // Handlebars
class HbKonfigurasjon(val fireRettsgebyr: BigDecimal = BigDecimal.valueOf(Constants.rettsgebyr * 4),
                      val halvtGrunnbeløp: BigDecimal = BigDecimal.valueOf(Constants.grunnbeløp / 2),
                      val klagefristIUker: Int)
