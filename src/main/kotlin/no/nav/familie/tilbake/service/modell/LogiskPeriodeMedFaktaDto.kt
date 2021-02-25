package no.nav.familie.tilbake.service.modell

import no.nav.familie.tilbake.common.Periode
import java.math.BigDecimal

class LogiskPeriodeMedFaktaDto(val periode: Periode,
                               var belop: BigDecimal,
                               var feilutbetalingÅrsakDto: HendelseTypeMedUndertypeDto? = null)
