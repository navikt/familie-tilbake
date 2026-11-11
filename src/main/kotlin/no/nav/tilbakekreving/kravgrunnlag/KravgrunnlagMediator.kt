package no.nav.tilbakekreving.kravgrunnlag

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
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
    private val log = TracedLogger.getLogger<KravgrunnlagMediator>()

    @Scheduled(fixedRate = 1000L)
    fun lesKravgrunnlag() {
        kravgrunnlagBufferRepository.konsumerKravgrunnlag { entity ->
            val kravgrunnlag = KravgrunnlagUtil.unmarshalKravgrunnlag(entity.kravgrunnlag)
            val kravgrunnlagHendelse = KravgrunnlagMapper.tilKravgrunnlagHendelse(kravgrunnlag, applicationProperties.kravgrunnlagMapping, entity.mottatt)
            if (kravgrunnlagHendelse.skalOppretteNySak()) {
                tilbakekrevingService.opprettTilbakekreving(KravgrunnlagMapper.tilOpprettTilbakekrevingHendelse(kravgrunnlag)) { tilbakekreving, sideeffektContext ->
                    tilbakekreving.håndter(kravgrunnlagHendelse, sideeffektContext)
                }
            } else {
                tilbakekrevingService.hentOgLagreTilbakekreving(
                    filter = TilbakekrevingFilter.vedtakId(kravgrunnlag.vedtakId, kravgrunnlag.fagsystemId),
                    validerScope = false,
                ) { tilbakekreving, context ->
                    tilbakekreving.håndter(kravgrunnlagHendelse, context(Behandler.Vedtaksløsning))
                } ?: throw Feil(
                    message = "Fant ingen tilbakekreving for kravgrunnlag med vedtakId ${kravgrunnlag.vedtakId}",
                    logContext = SecureLog.Context.utenBehandling(kravgrunnlag.fagsystemId),
                )
            }
        }
    }

    @Scheduled(fixedRate = 1000L)
    fun lesStatusmelding() {
        statusmeldingBufferRepository.konsumerStatusmelding { entity, mottatt ->
            val statusmelding = KravgrunnlagUtil.unmarshalStatusmelding(entity.statusmelding)
            val statusmeldingHendelse = KravgrunnlagMapper.tilStatusmeldingHendelse(statusmelding, entity.id, mottatt)

            log.medContext(SecureLog.Context.utenBehandling(entity.fagsystemId)) {
                info("Leser statusmelding, status={}", entity.status)
            }
            tilbakekrevingService.hentOgLagreTilbakekreving(
                filter = TilbakekrevingFilter.vedtakId(statusmeldingHendelse.vedtakId, statusmeldingHendelse.fagsystemId),
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
            } ?: throw Feil(
                message = "Fant ingen tilbakekreving for statusmelding med vedtakId ${statusmeldingHendelse.vedtakId}",
                logContext = SecureLog.Context.utenBehandling(statusmeldingHendelse.fagsystemId),
            )
        }
    }
}
