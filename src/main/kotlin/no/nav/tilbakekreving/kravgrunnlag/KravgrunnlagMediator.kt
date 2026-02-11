package no.nav.tilbakekreving.kravgrunnlag

import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.repository.TilbakekrevingRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class KravgrunnlagMediator(
    private val kravgrunnlagBufferRepository: KravgrunnlagBufferRepository,
    private val tilbakekrevingService: TilbakekrevingService,
    private val applicationProperties: ApplicationProperties,
) {
    @Scheduled(fixedRate = 1000L)
    fun lesKravgrunnlag() {
        kravgrunnlagBufferRepository.konsumerKravgrunnlag { entity ->
            val kravgrunnlag = KravgrunnlagUtil.unmarshalKravgrunnlag(entity.kravgrunnlag)
            val kravgrunnlagHendelse = KravgrunnlagMapper.tilKravgrunnlagHendelse(kravgrunnlag, applicationProperties.kravgrunnlagMapping)
            if (kravgrunnlagHendelse.skalOppretteNySak()) {
                tilbakekrevingService.opprettTilbakekreving(KravgrunnlagMapper.tilOpprettTilbakekrevingHendelse(kravgrunnlag)) { tilbakekreving ->
                    tilbakekreving.håndter(kravgrunnlagHendelse)
                }
            } else {
                val fagsystem = KravgrunnlagMapper.ytelseFor(kravgrunnlag).tilFagsystemDTO()
                tilbakekrevingService.hentOgLagreTilbakekreving(TilbakekrevingRepository.FindTilbakekrevingStrategy.EksternFagsakId(kravgrunnlag.fagsystemId, fagsystem)) { tilbakekreving ->
                    tilbakekreving.håndter(kravgrunnlagHendelse)
                }
            }
        }
    }
}
