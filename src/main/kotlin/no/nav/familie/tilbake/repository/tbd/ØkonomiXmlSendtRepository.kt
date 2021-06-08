package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.domain.tbd.Meldingstype
import no.nav.familie.tilbake.domain.tbd.ØkonomiXmlSendt
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Repository
@Transactional
interface ØkonomiXmlSendtRepository : RepositoryInterface<ØkonomiXmlSendt, UUID>, InsertUpdateRepository<ØkonomiXmlSendt> {

    @Query("Select * from okonomi_xml_sendt WHERE meldingstype = :meldingstype and opprettet_tid::date = :opprettetTid")
    fun findByMeldingstypeOgOpprettetPåDato(meldingstype: Meldingstype, opprettetTid: LocalDate): Collection<ØkonomiXmlSendt>
}