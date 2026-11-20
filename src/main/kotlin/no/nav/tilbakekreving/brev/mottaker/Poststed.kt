package no.nav.tilbakekreving.brev.mottaker

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.tilbake.kontrakter.objectMapper

object Poststed {
    val alle = objectMapper.readValue<Map<String, String>>(Poststed::class.java.getResourceAsStream("/postnummer.json")!!)
}
