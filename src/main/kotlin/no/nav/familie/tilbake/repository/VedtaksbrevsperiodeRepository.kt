package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Vedtaksbrevsperiode
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VedtaksbrevsperiodeRepository : RepositoryInterface<Vedtaksbrevsperiode, UUID>,
                                          InsertUpdateRepository<Vedtaksbrevsperiode>