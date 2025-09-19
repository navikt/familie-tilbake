package no.nav.tilbakekreving.fagsystem

import no.nav.tilbakekreving.api.v2.fagsystem.svar.FagsysteminfoSvarHendelse

class FagsystemIntegrasjonServiceMock : FagsystemIntegrasjonService {
    private val hendelser: MutableList<FagsysteminfoSvarHendelse> = mutableListOf()

    override fun håndter(ytelse: Ytelse, fagsysteminfo: FagsysteminfoSvarHendelse) {
        hendelser.add(fagsysteminfo)
    }

    fun finnHendelser(eksternFagsakId: String): List<FagsysteminfoSvarHendelse> = hendelser.filter { it.eksternFagsakId == eksternFagsakId }
}
