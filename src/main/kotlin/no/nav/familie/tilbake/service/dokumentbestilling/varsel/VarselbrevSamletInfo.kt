package no.nav.familie.tilbake.service.dokumentbestilling.varsel

import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import java.time.LocalDate

data class VarselbrevSamletInfo(val fritekstFraSaksbehandler: String? = null,
                                val feilutbetaltePerioder: List<Periode>,
                                val sumFeilutbetaling: Long,
                                val fristdato: LocalDate,
                                val brevmetadata: Brevmetadata,
                                val revurderingsvedtaksdato: LocalDate? = null)
