package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.tilbakekreving.Fagsystem
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
@Transactional
interface FagsakRepository : RepositoryInterface<Fagsak, UUID>, InsertUpdateRepository<Fagsak> {

    fun findByFagsystemAndEksternFagsakId(fagsystem: Fagsystem, eksternFagsakId: String): Fagsak?
}
