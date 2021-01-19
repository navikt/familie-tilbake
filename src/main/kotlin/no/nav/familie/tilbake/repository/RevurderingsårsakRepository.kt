package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Revurderingsårsak
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RevurderingsårsakRepository : RepositoryInterface<Revurderingsårsak, UUID>, InsertUpdateRepository<Revurderingsårsak>