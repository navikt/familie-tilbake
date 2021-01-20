package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.VilkårsvurderingAktsomhet
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VilkårsvurderingAktsomhetRepository : RepositoryInterface<VilkårsvurderingAktsomhet, UUID>,
                                                InsertUpdateRepository<VilkårsvurderingAktsomhet>