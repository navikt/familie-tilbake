package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Fagsystem
import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FagsakRepository : RepositoryInterface<Fagsak, UUID>, InsertUpdateRepository<Fagsak> {

    fun findByFagsystemAndEksternFagsakId(fagsystem: Fagsystem, eksternFagsakId: String): Fagsak?
}
