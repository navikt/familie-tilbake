package no.nav.tilbakekreving.pdf

import java.time.format.DateTimeFormatter
import java.util.Locale

object DatoUtil {
    val DATO_FORMAT_DATO_MÅNEDSNAVN_ÅR = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale("no"))
}
