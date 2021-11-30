package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import java.time.LocalDate
import java.util.UUID

interface MeldingstellingRepository : RepositoryInterface<Meldingstelling, UUID>,
                                      InsertUpdateRepository<Meldingstelling> {

    fun findByYtelsestypeAndAndTypeAndStatusAndDato(ytelsestype: Ytelsestype,
                                                    type: Meldingstype,
                                                    status: Mottaksstatus,
                                                    dato: LocalDate = LocalDate.now()): Meldingstelling?

    @Modifying
    @Query("""UPDATE meldingstelling set antall = antall + 1 
              WHERE ytelsestype = :ytelsestype
              AND type = :type
              AND status = :status
              AND dato = :dato""")
    fun oppdaterTeller(ytelsestype: Ytelsestype,
                       type: Meldingstype,
                       status: Mottaksstatus,
                       dato: LocalDate = LocalDate.now())

}