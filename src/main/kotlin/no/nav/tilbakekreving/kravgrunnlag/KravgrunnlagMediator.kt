package no.nav.tilbakekreving.kravgrunnlag

import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.hendelse.AvsluttetStatusmeldingHendelse
import no.nav.tilbakekreving.hendelse.EndretStatusmeldingHendelse
import no.nav.tilbakekreving.hendelse.SperretStatusmeldingHendelse
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

//    @Scheduled(fixedRate = 1000L)
    fun lesStatusmelding() {
        statusmeldingBufferRepository.konsumerStatusmelding { entity ->
            val statusmelding = KravgrunnlagUtil.unmarshalStatusmelding(entity.statusmelding)
            val statusmeldingHendelse = KravgrunnlagMapper.tilStatusmeldingHendelse(statusmelding, entity.id, SystemKlokke.nå())

            val fagsystem = KravgrunnlagMapper.ytelseFor(statusmeldingHendelse.kodeFagomraade, statusmeldingHendelse.fagsystemId).tilFagsystemDTO()
            tilbakekrevingService.hentOgLagreTilbakekreving(
                filter = TilbakekrevingFilter.fagsak(statusmeldingHendelse.fagsystemId, fagsystem),
                validerScope = false,
            ) { tilbakekreving, context ->
                when (statusmeldingHendelse) {
                    is EndretStatusmeldingHendelse -> {
                        tilbakekreving.håndter(statusmeldingHendelse, context(Behandler.Vedtaksløsning))
                    }
                    is SperretStatusmeldingHendelse -> {
                        tilbakekreving.håndter(statusmeldingHendelse, context(Behandler.Vedtaksløsning))
                    }
                    is AvsluttetStatusmeldingHendelse -> {
                        tilbakekreving.håndter(statusmeldingHendelse, context(Behandler.Vedtaksløsning))
                    }
                }
            }
        }
    }

    @Scheduled(fixedRate = 1000L)
    fun lagreGamleStatusmeldinger() {
        statusmeldingBufferRepository.konsumerStatusmeldingerPåNytt { entity, mottatt ->
            val statusmelding = KravgrunnlagUtil.unmarshalStatusmelding(entity.statusmelding)
            val statusmeldingHendelse = KravgrunnlagMapper.tilStatusmeldingHendelse(statusmelding, entity.id, mottatt)

            val fagsystem = KravgrunnlagMapper.ytelseFor(statusmeldingHendelse.kodeFagomraade, statusmeldingHendelse.fagsystemId).tilFagsystemDTO()
            tilbakekrevingService.hentOgLagreTilbakekreving(
                filter = TilbakekrevingFilter.fagsak(statusmeldingHendelse.fagsystemId, fagsystem),
                validerScope = false,
            ) { tilbakekreving, _ ->
                tilbakekreving.lagreStatusmelding(statusmeldingHendelse)
            }
        }
    }
}
