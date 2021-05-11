package no.nav.familie.tilbake.api.dto

import no.nav.familie.tilbake.service.dokumentbestilling.brevmaler.Dokumentmalstype

data class ForhåndsvisningBrevDto(val maltype: Dokumentmalstype, val fritekst: String)
