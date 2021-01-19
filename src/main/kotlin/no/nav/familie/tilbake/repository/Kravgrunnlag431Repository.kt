package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Kravgrunnlag431
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface Kravgrunnlag431Repository : RepositoryInterface<Kravgrunnlag431, UUID>, InsertUpdateRepository<Kravgrunnlag431>