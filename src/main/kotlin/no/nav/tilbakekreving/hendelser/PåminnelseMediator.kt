package no.nav.tilbakekreving.hendelser

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
import no.nav.tilbakekreving.repository.TilbakekrevingRepository
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

@Service
class PåminnelseMediator(
    private val tilbakekrevingRepository: TilbakekrevingRepository,
    private val tilbakekrevinService: TilbakekrevingService,
) {
    private val logger = TracedLogger.getLogger<PåminnelseMediator>()
    private val sakerITilstand = MultiGauge.builder("current_state")
        .register(Metrics.globalRegistry)

    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    fun påminnSaker() {
        val tilbakekrevinger = tilbakekrevingRepository.hentTilbakekrevinger(TilbakekrevingFilter.trengerPåminnelse())
        for (tilbakekrevingEntity in tilbakekrevinger) {
            var logContext = SecureLog.Context.tom()
            try {
                tilbakekrevinService.hentOgLagreTilbakekreving(TilbakekrevingFilter.tilbakekreving(tilbakekrevingEntity.id)) { tilbakekreving, context ->
                    logContext = SecureLog.Context.fra(tilbakekreving)
                    logger.medContext(logContext) {
                        info("Sender påminnelse")
                    }
                    tilbakekreving.håndter(Påminnelse(LocalDateTime.now()), context(Behandler.Vedtaksløsning))
                }
            } catch (e: Exception) {
                logger.medContext(logContext) {
                    error("Feilet under påminnelse av sak med {}", keyValue("tilbakekrevingId", tilbakekrevingEntity.id), e)
                }
            }
        }
        sakerITilstand.register(
            tilbakekrevingRepository.antallSakerPerTilstand()
                .map { info ->
                    MultiGauge.Row.of(
                        Tags.of("state", info.tilstand).and("ytelse", info.ytelse),
                        info.antallSaker,
                    )
                },
            true,
        )
    }
}
