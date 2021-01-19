package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Vedtaksbrevsoppsummering
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VedtaksbrevsoppsummeringRepository : RepositoryInterface<Vedtaksbrevsoppsummering, UUID>,
                                               InsertUpdateRepository<Vedtaksbrevsoppsummering>