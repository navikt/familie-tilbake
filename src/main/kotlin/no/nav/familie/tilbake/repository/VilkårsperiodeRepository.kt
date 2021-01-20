package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Vilkårsvurderingsperiode
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VilkårsperiodeRepository : RepositoryInterface<Vilkårsvurderingsperiode, UUID>,
                                     InsertUpdateRepository<Vilkårsvurderingsperiode>