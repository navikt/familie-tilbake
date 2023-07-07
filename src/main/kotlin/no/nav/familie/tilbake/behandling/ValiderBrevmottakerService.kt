package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerService
import no.nav.familie.tilbake.person.PersonService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ValiderBrevmottakerService(
    private val manuellBrevmottakerService: ManuellBrevmottakerService,
    private val fagsakService: FagsakService,
    private val personService: PersonService
) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    fun validerAtBehandlingIkkeInneholderStrengtFortroligPersonMedManuelleBrevmottakere(behandlingId: UUID, fagsakId: UUID) {
        val manuelleBrevmottakere = manuellBrevmottakerService.hentBrevmottakere(behandlingId).takeIf { it.isNotEmpty() } ?: return
        val fagsak = fagsakService.hentFagsak(fagsakId)
        val bruker = fagsak.bruker
        val fagsystem = fagsak.fagsystem
        val personIdenter = listOfNotNull(bruker.ident)
        if (personIdenter.isEmpty()) return
        val strengtFortroligePersonIdenter = personService.hentIdenterMedStrengtFortroligAdressebeskyttelse(personIdenter, fagsystem)
        logger.info("strengtFortroligePersonIdenter ${strengtFortroligePersonIdenter.size}")
        if (strengtFortroligePersonIdenter.isNotEmpty()) {
            val melding =
                "Behandlingen (id: $behandlingId) inneholder person med strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere (${manuelleBrevmottakere.size} stk)."
            val frontendFeilmelding =
                "Behandlingen inneholder person med strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere."
            throw Feil(melding, frontendFeilmelding)
        }
    }
}
