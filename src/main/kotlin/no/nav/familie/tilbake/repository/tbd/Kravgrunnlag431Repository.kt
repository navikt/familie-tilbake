package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.domain.tbd.Kravgrunnlag431
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface Kravgrunnlag431Repository : RepositoryInterface<Kravgrunnlag431, UUID>, InsertUpdateRepository<Kravgrunnlag431>