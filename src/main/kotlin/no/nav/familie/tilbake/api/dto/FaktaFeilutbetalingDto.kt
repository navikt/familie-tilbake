package no.nav.familie.tilbake.api.dto

import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import java.math.BigDecimal
import java.time.LocalDate

data class FaktaFeilutbetalingDto(val varsletBeløp: Long? = null,
                                  val totalFeilutbetaltPeriode: Periode,
                                  val feilutbetaltePerioder: Set<FeilutbetalingsperiodeDto>,
                                  val totalFeilutbetaltBeløp: BigDecimal,
                                  val revurderingsvedtaksdato: LocalDate,
                                  val begrunnelse: String,
                                  val faktainfo: Faktainfo)

data class FeilutbetalingsperiodeDto(val periode: Periode,
                                     val feilutbetaltBeløp: BigDecimal,
                                     val hendelsestype: Hendelsestype? = null,
                                     val hendelsesundertype: Hendelsesundertype? = null)
