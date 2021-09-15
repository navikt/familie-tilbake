package no.nav.familie.tilbake.micrometer

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import no.nav.familie.leader.LeaderClient
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.micrometer.domain.MålerRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class MålerService(private val målerRepository: MålerRepository) {

    val åpneBehandlingerGauge = MultiGauge.builder("UavsluttedeBehandlinger").register(Metrics.globalRegistry)
    val klarTilBehandlingGauge = MultiGauge.builder("KlarTilBehandling").register(Metrics.globalRegistry)
    val ventendeBehandlingGauge = MultiGauge.builder("VentendeBehandlinger").register(Metrics.globalRegistry)
    val sendteBrevGauge = MultiGauge.builder("SendteBrev").register(Metrics.globalRegistry)
    val vedtakGauge = MultiGauge.builder("Vedtak").register(Metrics.globalRegistry)

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(initialDelay = 10000, fixedDelay = OPPDATERINGSFREKVENS)
    fun åpneBehandlinger() {
        if (LeaderClient.isLeader() != true) return
        val behandlinger = målerRepository.finnÅpneBehandlinger()
        logger.info("Åpne behandlinger returnerte ${behandlinger.sumOf { it.antall }} fordelt på ${behandlinger.size} uker.")
        val rows = behandlinger.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "uke", it.år.toString() + "-" + it.uke.toString().padStart(2, '0')),
                              it.antall)
        }

        åpneBehandlingerGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 15000, fixedDelay = OPPDATERINGSFREKVENS)
    fun behandlingerKlarTilSaksbehandling() {
        if (LeaderClient.isLeader() != true) return
        val behandlinger = målerRepository.finnKlarTilBehandling()
        logger.info("Behandlinger klar til saksbehandling returnerte ${behandlinger.sumOf { it.antall }} " +
                    "fordelt på ${behandlinger.size} steg.")
        val rows = behandlinger.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "steg", it.behandlingssteg.name),
                              it.antall)
        }

        klarTilBehandlingGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 20000, fixedDelay = OPPDATERINGSFREKVENS)
    fun behandlingerPåVent() {
        if (LeaderClient.isLeader() != true) return
        val behandlinger = målerRepository.finnVentendeBehandlinger()
        logger.info("Behandlinger på vent returnerte ${behandlinger.sumOf { it.antall }} " +
                    "fordelt på ${behandlinger.size} steg.")

        val rows = behandlinger.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "steg", it.behandlingssteg.name),
                              it.antall)
        }

        ventendeBehandlingGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 25000, fixedDelay = OPPDATERINGSFREKVENS)
    fun sendteBrev() {
        if (LeaderClient.isLeader() != true) return
        val data = målerRepository.finnSendteBrev()
        logger.info("Sendte brev returnerte ${data.sumOf { it.antall }} fordelt på ${data.size} typer/uker.")

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "brevtype", it.brevtype.name,
                                      "uke", it.år.toString() + "-" + it.uke.toString().padStart(2, '0')),
                              it.antall)
        }
        sendteBrevGauge.register(rows)
    }

    @Scheduled(initialDelay = 30000, fixedDelay = OPPDATERINGSFREKVENS)
    fun vedtak() {
        if (LeaderClient.isLeader() != true) return
        val data = målerRepository.finnVedtak()
        logger.info("Vedtak returnerte ${data.sumOf { it.antall }} fordelt på ${data.size} typer/uker.")

        val rows = data.map {

            val vedtakstype = if (it.vedtakstype in Behandlingsresultat.ALLE_HENLEGGELSESKODER)
                Behandlingsresultatstype.HENLAGT.name else it.vedtakstype.name

            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "vedtakstype", vedtakstype,
                                      "uke", it.år.toString() + "-" + it.uke.toString().padStart(2, '0')),
                              it.antall)
        }
        vedtakGauge.register(rows)
    }

    companion object {

        const val OPPDATERINGSFREKVENS = 1800 * 1000L
    }


}