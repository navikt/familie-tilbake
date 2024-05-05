package no.nav.familie.tilbake.historikkinnslag

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import java.util.UUID

interface HistorikkinnslagRepository : RepositoryInterface<Historikkinnslag, UUID>, InsertUpdateRepository<Historikkinnslag> {
    fun findByBehandlingId(behandlingId: UUID): List<Historikkinnslag>
}
