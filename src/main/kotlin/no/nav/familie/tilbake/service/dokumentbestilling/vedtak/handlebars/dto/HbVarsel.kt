package no.nav.familie.tilbake.service.dokumentbestilling.vedtak.handlebars.dto

import java.math.BigDecimal
import java.time.LocalDate

@Suppress("unused") // Handlebars
class HbVarsel(private val varsletDato: LocalDate,
               private val varsletBeløp: BigDecimal?)
