package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.VilkårsvurderingSærligGrunn
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VilkårsvurderingSærligGrunnRepository : RepositoryInterface<VilkårsvurderingSærligGrunn, UUID>,
                                                  InsertUpdateRepository<VilkårsvurderingSærligGrunn>