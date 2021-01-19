package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.GrupperingKravGrunnlag
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface GrupperingKravGrunnlagRepository : RepositoryInterface<GrupperingKravGrunnlag, UUID>,
                                             InsertUpdateRepository<GrupperingKravGrunnlag>