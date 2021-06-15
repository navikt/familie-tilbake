package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.util.UUID

@Repository
@Transactional
interface ØkonomiXmlMottattRepository : RepositoryInterface<ØkonomiXmlMottatt, UUID>, InsertUpdateRepository<ØkonomiXmlMottatt> {

    fun findByEksternKravgrunnlagIdAndVedtakId(eksternKravgrunnlagId: BigInteger, vedtakId: BigInteger): List<ØkonomiXmlMottatt>

    fun findByEksternFagsakIdAndYtelsestypeAndVedtakId(eksternFagsakId: String,
                                                       ytelsestype: Ytelsestype,
                                                       vedtakId: BigInteger): List<ØkonomiXmlMottatt>

    fun findByEksternFagsakIdAndYtelsestype(eksternFagsakId: String,
                                            ytelsestype: Ytelsestype): List<ØkonomiXmlMottatt>

}
