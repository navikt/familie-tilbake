package no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon

import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import java.time.LocalDate

internal data class InnhentDokumentasjonsbrevSamletInfo(val fritekstFraSaksbehandler: String,
                                                        val fristdato: LocalDate,
                                                        val brevmetadata: Brevmetadata)
