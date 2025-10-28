package no.nav.tilbakekreving.kravgrunnlag

import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.config.ApplicationProperties
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class KravgrunnlagMediator(
    private val kravgrunnlagBufferRepository: KravgrunnlagBufferRepository,
    private val tilbakekrevingService: TilbakekrevingService,
    private val applicationProperties: ApplicationProperties,
) {
    @Scheduled(fixedRate = 1000L)
    fun håndterKravgrunnlag() {
        if (applicationProperties.toggles.nyModellEnabled) {
            lesKravgrunnlag()
        }
    }

    fun lesKravgrunnlag() {
        kravgrunnlagBufferRepository.konsumerKravgrunnlag { entity ->
            val kravgrunnlag = KravgrunnlagUtil.unmarshalKravgrunnlag(entity.kravgrunnlag)
            tilbakekrevingService.opprettTilbakekreving(KravgrunnlagMapper.tilOpprettTilbakekrevingHendelse(kravgrunnlag)) { tilbakekreving ->
                tilbakekreving.håndter(KravgrunnlagMapper.tilKravgrunnlagHendelse(kravgrunnlag, applicationProperties.kravgrunnlagMapping))
            }
        }
    }
}
