package no.nav.tilbakekreving.norg2

import no.tilbakekreving.integrasjoner.norg2.Norg2Client
import no.tilbakekreving.integrasjoner.norg2.kontrakter.NavKontorEnhet
import org.springframework.stereotype.Service

@Service
class Norg2Service(
    private val norg2Client: Norg2Client,
) {
    fun hentNavKontor(enhetsId: String): NavKontorEnhet {
        return norg2Client.hentNavkontor(enhetsId)
    }
}
