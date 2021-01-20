package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Vilkårsvurdering
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VilkårsvurderingRepository : RepositoryInterface<Vilkårsvurdering, UUID>, InsertUpdateRepository<Vilkårsvurdering>