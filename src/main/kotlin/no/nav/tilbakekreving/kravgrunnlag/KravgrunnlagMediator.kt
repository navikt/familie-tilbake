package no.nav.tilbakekreving.kravgrunnlag

import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class KravgrunnlagMediator(
    private val kravgrunnlagBufferRepository: KravgrunnlagBufferRepository,
    private val tilbakekrevingService: TilbakekrevingService,
    private val applicationProperties: ApplicationProperties,
    private val statusmeldingBufferRepository: StatusmeldingBufferRepository,
) {
    @Scheduled(fixedRate = 1000L)
    fun lesKravgrunnlag() {
        kravgrunnlagBufferRepository.konsumerKravgrunnlag { entity ->
            val kravgrunnlag = KravgrunnlagUtil.unmarshalKravgrunnlag(entity.kravgrunnlag)
            val kravgrunnlagHendelse = KravgrunnlagMapper.tilKravgrunnlagHendelse(kravgrunnlag, applicationProperties.kravgrunnlagMapping, SystemKlokke)
            if (kravgrunnlagHendelse.skalOppretteNySak()) {
                tilbakekrevingService.opprettTilbakekreving(KravgrunnlagMapper.tilOpprettTilbakekrevingHendelse(kravgrunnlag)) { tilbakekreving, sideeffektContext ->
                    tilbakekreving.håndter(kravgrunnlagHendelse, sideeffektContext)
                }
            } else {
                val fagsystem = KravgrunnlagMapper.ytelseFor(kravgrunnlag.kodeFagomraade, kravgrunnlag.fagsystemId).tilFagsystemDTO()
                tilbakekrevingService.hentOgLagreTilbakekreving(
                    filter = TilbakekrevingFilter.fagsak(kravgrunnlag.fagsystemId, fagsystem),
                    validerScope = false,
                ) { tilbakekreving, context ->
                    tilbakekreving.håndter(kravgrunnlagHendelse, context(Behandler.Vedtaksløsning))
                }
            }
        }
    }

    @Scheduled(fixedRate = 1000L)
    fun lesStatusmelding() {
        statusmeldingBufferRepository.konsumerStatusmelding { entity ->
            val statusmelding = KravgrunnlagUtil.unmarshalStatusmelding(entity.statusmelding)
            val statusmeldingHendelse = KravgrunnlagMapper.tilStatusmeldingHendelse(statusmelding, SystemKlokke)

            val fagsystem = KravgrunnlagMapper.ytelseFor(statusmeldingHendelse.kodeFagomraade, statusmeldingHendelse.fagsystemId).tilFagsystemDTO()
            tilbakekrevingService.hentOgLagreTilbakekreving(
                filter = TilbakekrevingFilter.fagsak(statusmeldingHendelse.fagsystemId, fagsystem),
                validerScope = false,
            ) { tilbakekreving, context ->
                tilbakekreving.håndter(statusmeldingHendelse, context(Behandler.Vedtaksløsning))
            }
        }
    }
}
