package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Aksjonspunkt
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AksjonspunktRepository : RepositoryInterface<Aksjonspunkt, UUID>, InsertUpdateRepository<Aksjonspunkt>