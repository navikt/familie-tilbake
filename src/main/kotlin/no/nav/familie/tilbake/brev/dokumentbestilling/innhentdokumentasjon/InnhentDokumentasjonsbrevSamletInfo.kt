package no.nav.familie.tilbake.brev.dokumentbestilling.innhentdokumentasjon

import no.nav.familie.tilbake.brev.dokumentbestilling.felles.BrevMetadata
import java.time.LocalDate

internal data class InnhentDokumentasjonsbrevSamletInfo(val fritekstFraSaksbehandler: String,
                                                        val fristDato: LocalDate,
                                                        val brevMetadata: BrevMetadata)
