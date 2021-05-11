package no.nav.familie.tilbake.person

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.tilbake.integration.pdl.PdlClient
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import org.springframework.stereotype.Service

@Service
class PersonService(val pdlClient: PdlClient) {

    fun hentPersoninfo(personIdent: String, fagsystem: Fagsystem): Personinfo {
        return pdlClient.hentPersoninfo(personIdent, fagsystem)
    }


    fun hentAktørId(personIdent: String, fagsystem: Fagsystem): List<String> {
        val hentIdenter = pdlClient.hentIdenter(personIdent, fagsystem)
        return hentIdenter.data.pdlIdenter!!.identer.filter { it.gruppe == "AKTORID" && !it.historisk }.map { it.ident }
    }

    fun hentAktivAktørId(ident: String, fagsystem: Fagsystem): String {
        val aktørId = hentAktørId(ident, fagsystem)
        if (aktørId.isEmpty()) error("Finner ingen aktiv aktørId for ident")
        return aktørId.first()
    }

}
