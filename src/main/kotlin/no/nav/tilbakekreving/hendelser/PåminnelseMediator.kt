package no.nav.tilbakekreving.hendelser

import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.repository.TilbakekrevingRepository
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

    @Scheduled(fixedRate = 10, timeUnit = TimeUnit.MINUTES)
    fun påminnSaker() {
        val tilbakekrevinger = tilbakekrevingRepository.hentTilbakekrevinger(TilbakekrevingRepository.FindTilbakekrevingStrategy.TrengerPåminnelse)
        for (tilbakekrevingEntity in tilbakekrevinger) {
            tilbakekrevinService.hentOgLagreTilbakekreving(TilbakekrevingRepository.FindTilbakekrevingStrategy.TilbakekrevingId(tilbakekrevingEntity.id)) { tilbakekreving ->
                val context = SecureLog.Context.fra(tilbakekreving)
                logger.medContext(context) {
                    info("Sender påminnelse")
                }
                tilbakekreving.håndter(Påminnelse(LocalDateTime.now()))
            }
        }
    }
}
