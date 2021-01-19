package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.ØkonomiXmlSendt
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ØkonomiXmlSendtRepository : RepositoryInterface<ØkonomiXmlSendt, UUID>, InsertUpdateRepository<ØkonomiXmlSendt>