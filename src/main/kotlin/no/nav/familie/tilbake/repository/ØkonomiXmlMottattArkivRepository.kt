package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.ØkonomiXmlMottattArkiv
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ØkonomiXmlMottattArkivRepository : RepositoryInterface<ØkonomiXmlMottattArkiv, UUID>,
                                             InsertUpdateRepository<ØkonomiXmlMottattArkiv>