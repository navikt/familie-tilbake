package no.nav.familie.tilbake.api.dto

import no.nav.familie.tilbake.service.dokumentbestilling.brevmaler.Dokumentmalstype

data class ForhåndsvisningBrevDto(val malType: Dokumentmalstype,
                                  val fritekst: String) {
}
