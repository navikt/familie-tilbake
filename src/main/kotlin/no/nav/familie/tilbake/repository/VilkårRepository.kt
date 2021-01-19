package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Vilkår
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VilkårRepository : RepositoryInterface<Vilkår, UUID>, InsertUpdateRepository<Vilkår>