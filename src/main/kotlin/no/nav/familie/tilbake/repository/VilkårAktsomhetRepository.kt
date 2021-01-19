package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.VilkårAktsomhet
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VilkårAktsomhetRepository : RepositoryInterface<VilkårAktsomhet, UUID>, InsertUpdateRepository<VilkårAktsomhet>