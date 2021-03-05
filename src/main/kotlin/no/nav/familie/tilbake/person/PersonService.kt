package no.nav.familie.tilbake.person

import no.nav.familie.kontrakter.felles.tilbakekreving.Fagsystem
import no.nav.familie.tilbake.integration.pdl.PdlClient
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import org.springframework.stereotype.Service

@Service
class PersonService(val pdlClient: PdlClient) {

    fun hentPersoninfo(personIdent: String, fagsystem: Fagsystem): Personinfo {
        return pdlClient.hentPersoninfo(personIdent, fagsystem)
    }
}
