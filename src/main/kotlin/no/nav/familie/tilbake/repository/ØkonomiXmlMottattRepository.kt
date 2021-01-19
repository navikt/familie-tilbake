package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.ØkonomiXmlMottatt
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ØkonomiXmlMottattRepository : RepositoryInterface<ØkonomiXmlMottatt, UUID>, InsertUpdateRepository<ØkonomiXmlMottatt>