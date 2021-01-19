package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.VilkårSærligGrunn
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VilkårSærligGrunnRepository : RepositoryInterface<VilkårSærligGrunn, UUID>, InsertUpdateRepository<VilkårSærligGrunn>