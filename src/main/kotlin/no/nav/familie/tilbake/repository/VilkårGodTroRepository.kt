package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.VilkårGodTro
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VilkårGodTroRepository : RepositoryInterface<VilkårGodTro, UUID>, InsertUpdateRepository<VilkårGodTro>