package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Behandlingsresultat
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BehandlingsresultatRepository : RepositoryInterface<Behandlingsresultat, UUID>,
                                          InsertUpdateRepository<Behandlingsresultat>