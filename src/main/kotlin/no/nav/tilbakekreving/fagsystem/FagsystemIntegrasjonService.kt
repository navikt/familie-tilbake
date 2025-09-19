package no.nav.tilbakekreving.fagsystem

import no.nav.tilbakekreving.api.v2.fagsystem.svar.FagsysteminfoSvarHendelse

interface FagsystemIntegrasjonService {
    fun håndter(ytelse: Ytelse, fagsysteminfo: FagsysteminfoSvarHendelse)
}
