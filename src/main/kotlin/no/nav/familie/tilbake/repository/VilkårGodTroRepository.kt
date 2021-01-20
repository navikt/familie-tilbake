package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.VilkårsvurderingGodTro
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VilkårsvurderingGodTroRepository : RepositoryInterface<VilkårsvurderingGodTro, UUID>,
                                             InsertUpdateRepository<VilkårsvurderingGodTro>