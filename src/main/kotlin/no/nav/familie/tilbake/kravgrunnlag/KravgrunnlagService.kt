package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.ØkonomiXmlMottatt
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class KravgrunnlagService(private val kravgrunnlagRepository: KravgrunnlagRepository,
                          private val behandlingRepository: BehandlingRepository,
                          private val mottattXmlService: ØkonomiXmlMottattService,
                          private val stegService: StegService) {

    @Transactional
    fun håndterMottattKravgrunnlag(kravgrunnlagXml: String) {
        val kravgrunnlag: DetaljertKravgrunnlagDto = KravgrunnlagUtil.unmarshalKravgrunnlag(kravgrunnlagXml)
        // valider grunnlag
        KravgrunnlagValidator.validerGrunnlag(kravgrunnlag)

        val fagsystemId = kravgrunnlag.fagsystemId
        val ytelsestype: Ytelsestype = KravgrunnlagUtil.tilYtelsestype(kravgrunnlag.kodeFagomraade)

        val behandling: Behandling? = finnÅpenBehandling(ytelsestype, fagsystemId)
        if (behandling == null) {
            arkiverEksisterendeGrunnlag(kravgrunnlag)
            mottattXmlService.lagreMottattXml(kravgrunnlagXml, kravgrunnlag, ytelsestype)
            return
        }
        // mapper grunnlag til Kravgrunnlag431
        val kravgrunnlag431: Kravgrunnlag431 = KravgrunnlagMapper.tilKravgrunnlag431(kravgrunnlag, behandling.id)
        lagreKravgrunnlag(kravgrunnlag431)
        mottattXmlService.arkiverMottattXml(kravgrunnlagXml, fagsystemId, ytelsestype)

        stegService.håndterSteg(behandling.id)
    }

    private fun finnÅpenBehandling(ytelsestype: Ytelsestype,
                                   fagsystemId: String): Behandling? {
        return behandlingRepository.finnÅpenTilbakekrevingsbehandling(ytelsestype = ytelsestype,
                                                                      eksternFagsakId = fagsystemId)
    }

    private fun arkiverEksisterendeGrunnlag(kravgrunnlag: DetaljertKravgrunnlagDto) {
        val eksisterendeKravgrunnlag: List<ØkonomiXmlMottatt> =
                mottattXmlService.hentMottattKravgrunnlag(eksternKravgrunnlagId = kravgrunnlag.kravgrunnlagId,
                                                          vedtakId = kravgrunnlag.vedtakId)
        eksisterendeKravgrunnlag.forEach {
            mottattXmlService.arkiverMottattXml(mottattXml = it.melding,
                                                fagsystemId = it.eksternFagsakId,
                                                ytelsestype = it.ytelsestype)
        }
        eksisterendeKravgrunnlag.forEach { mottattXmlService.slettMottattXml(it.id) }
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
