package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottattArkiv
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class KravgrunnlagService(private val kravgrunnlagRepository: KravgrunnlagRepository,
                          private val behandlingRepository: BehandlingRepository,
                          private val mottattXmlRepository: ØkonomiXmlMottattRepository,
                          private val mottattXmlArkivRepository: ØkonomiXmlMottattArkivRepository,
                          private val stegService: StegService) {

    @Transactional
    fun håndterMottattKravgrunnlag(kravgrunnlagXml: String) {
        val kravgrunnlag: DetaljertKravgrunnlagDto = KravgrunnlagUtil.unmarshal(kravgrunnlagXml)
        // valider grunnlag
        KravgrunnlagValidator.validerGrunnlag(kravgrunnlag)

        val fagsystemId = kravgrunnlag.fagsystemId
        val ytelsestype: Ytelsestype = KravgrunnlagUtil.tilYtelsestype(kravgrunnlag.kodeFagomraade)

        val behandling: Behandling? = finnÅpenBehandling(ytelsestype, fagsystemId)
        if (behandling == null) {
            arkiverEksisterendeGrunnlag(kravgrunnlag)
            lagreMottattXml(kravgrunnlagXml, kravgrunnlag, ytelsestype)
            return
        }
        // mapper grunnlag til Kravgrunnlag431
        val kravgrunnlag431: Kravgrunnlag431 = KravgrunnlagMapper.tilKravgrunnlag431(kravgrunnlag, behandling.id)
        lagreKravgrunnlag(kravgrunnlag431)
        arkiverKravgrunnlagXml(kravgrunnlagXml)

        stegService.håndterSteg(behandling.id)
    }

    private fun finnÅpenBehandling(ytelsestype: Ytelsestype,
                                   fagsystemId: String): Behandling? {
        return behandlingRepository.finnÅpenTilbakekrevingsbehandling(ytelsestype = ytelsestype,
                                                                      eksternFagsakId = fagsystemId)
    }

    private fun arkiverKravgrunnlagXml(kravgrunnlagXml: String) {
        mottattXmlArkivRepository.insert(ØkonomiXmlMottattArkiv(melding = kravgrunnlagXml))
    }

    private fun arkiverEksisterendeGrunnlag(kravgrunnlag: DetaljertKravgrunnlagDto) {
        val eksisterendeKravgrunnlag: List<ØkonomiXmlMottatt> =
                mottattXmlRepository.findByEksternKravgrunnlagIdAndVedtakId(eksternKravgrunnlagId = kravgrunnlag.kravgrunnlagId,
                                                                            vedtakId = kravgrunnlag.vedtakId)
        eksisterendeKravgrunnlag.forEach { arkiverKravgrunnlagXml(it.melding) }
        eksisterendeKravgrunnlag.forEach { slettMottattXml(it.id) }
    }

    private fun lagreMottattXml(kravgrunnlagXml: String,
                                kravgrunnlag: DetaljertKravgrunnlagDto,
                                ytelsestype: Ytelsestype) {
        mottattXmlRepository.insert(ØkonomiXmlMottatt(melding = kravgrunnlagXml,
                                                      kravstatuskode = Kravstatuskode.fraKode(kravgrunnlag.kodeStatusKrav),
                                                      eksternFagsakId = kravgrunnlag.fagsystemId,
                                                      ytelsestype = ytelsestype,
                                                      referanse = kravgrunnlag.referanse,
                                                      eksternKravgrunnlagId = kravgrunnlag.kravgrunnlagId,
                                                      vedtakId = kravgrunnlag.vedtakId,
                                                      kontrollfelt = kravgrunnlag.kontrollfelt))
    }

    private fun slettMottattXml(mottattXmlId: UUID) {
        mottattXmlRepository.deleteById(mottattXmlId)
    }

    private fun lagreKravgrunnlag(kravgrunnlag431: Kravgrunnlag431) {
        val finnesKravgrunnlag =
                kravgrunnlagRepository.existsByBehandlingIdAndAktivTrueAndSperretFalse(kravgrunnlag431.behandlingId)
        if (finnesKravgrunnlag) {
            val eksisterendeKravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(kravgrunnlag431.behandlingId)
            kravgrunnlagRepository.update(eksisterendeKravgrunnlag.copy(aktiv = false))
        }
        kravgrunnlagRepository.insert(kravgrunnlag431)
    }

}
