package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.behandling.domain.Varsel
import org.springframework.stereotype.Service

@Service
class VarselService(val varselRepository: VarselRepository) {

    fun lagre(payload: String, varseltekst: String, varselbeløp: Long) {

        varselRepository.insert(Varsel(varseltekst = varseltekst, varselbeløp = varselbeløp))

    }


}