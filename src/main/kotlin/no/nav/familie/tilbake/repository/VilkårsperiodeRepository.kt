package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Vilkårsperiode
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VilkårsperiodeRepository : RepositoryInterface<Vilkårsperiode, UUID>, InsertUpdateRepository<Vilkårsperiode>