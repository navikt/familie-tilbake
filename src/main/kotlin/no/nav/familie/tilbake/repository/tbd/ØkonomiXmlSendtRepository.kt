package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.domain.tbd.ØkonomiXmlSendt
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
@Transactional
interface ØkonomiXmlSendtRepository : RepositoryInterface<ØkonomiXmlSendt, UUID>, InsertUpdateRepository<ØkonomiXmlSendt>