package no.nav.familie.tilbake.brev.dokumentbestilling.varsel

import no.nav.familie.tilbake.brev.dokumentbestilling.felles.BrevMetadata
import no.nav.familie.tilbake.brev.dokumentbestilling.handlebars.dto.periode.HbPeriode
import java.time.LocalDate

data class VarselbrevSamletInfo(val fritekstFraSaksbehandler: String? = null,
                                val feilutbetaltePerioder: List<HbPeriode>,
                                val sumFeilutbetaling: Long,
                                val fristdato: LocalDate,
                                val brevMetadata: BrevMetadata,
                                val revurderingVedtakDato: LocalDate? = null)
