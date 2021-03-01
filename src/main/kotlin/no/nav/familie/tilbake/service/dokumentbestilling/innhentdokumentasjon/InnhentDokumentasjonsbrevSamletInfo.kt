package no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon

import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevMetadata
import java.time.LocalDate

internal data class InnhentDokumentasjonsbrevSamletInfo(val fritekstFraSaksbehandler: String,
                                                        val fristDato: LocalDate,
                                                        val brevMetadata: BrevMetadata)
