package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbGrunnbeløpsperiode
import java.math.BigDecimal
import java.time.LocalDate

object HbUtils {

    val hbGrunnbeløpsperiode = listOf(
        HbGrunnbeløpsperiode(
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2021, 3, 31),
            BigDecimal(14034),
            true,
            true
        )
    )
}
