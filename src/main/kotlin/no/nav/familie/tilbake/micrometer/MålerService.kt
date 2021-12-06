package no.nav.familie.tilbake.micrometer

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import no.nav.familie.leader.LeaderClient
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.micrometer.domain.MeldingstellingRepository
import no.nav.familie.tilbake.micrometer.domain.Meldingstype
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class MålerService(private val meldingstellingRepository: MeldingstellingRepository) {

    private val åpneBehandlingerGauge = MultiGauge.builder("UavsluttedeBehandlinger").register(Metrics.globalRegistry)
    private val klarTilBehandlingGauge = MultiGauge.builder("KlarTilBehandling").register(Metrics.globalRegistry)
    private val ventendeBehandlingGauge = MultiGauge.builder("VentendeBehandlinger").register(Metrics.globalRegistry)
    private val sendteBrevGauge = MultiGauge.builder("SendteBrev").register(Metrics.globalRegistry)
    private val vedtakGauge = MultiGauge.builder("Vedtak").register(Metrics.globalRegistry)
    private val mottatteKravgrunnlagGauge = MultiGauge.builder("MottatteKravgrunnlag").register(Metrics.globalRegistry)
    private val mottatteStatusmeldingerGauge = MultiGauge.builder("mottatteStatusmeldinger").register(Metrics.globalRegistry)

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(initialDelay = 60000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun åpneBehandlinger() {
        if (LeaderClient.isLeader() != true) return
        val behandlinger = meldingstellingRepository.finnÅpneBehandlinger()
        logger.info("Åpne behandlinger returnerte ${behandlinger.sumOf { it.antall }} fordelt på ${behandlinger.size} uker.")
        val rows = behandlinger.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "uke", it.år.toString() + "-" + it.uke.toString().padStart(2, '0')),
                              it.antall)
        }

        åpneBehandlingerGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 90000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun behandlingerKlarTilSaksbehandling() {
        if (LeaderClient.isLeader() != true) return
        val behandlinger = meldingstellingRepository.finnKlarTilBehandling()
        logger.info("Behandlinger klar til saksbehandling returnerte ${behandlinger.sumOf { it.antall }} " +
                    "fordelt på ${behandlinger.size} steg.")
        val rows = behandlinger.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "steg", it.behandlingssteg.name),
                              it.antall)
        }

        klarTilBehandlingGauge.register(rows,true)
    }

    @Scheduled(initialDelay = 120000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun behandlingerPåVent() {
        if (LeaderClient.isLeader() != true) return
        val behandlinger = meldingstellingRepository.finnVentendeBehandlinger()
        logger.info("Behandlinger på vent returnerte ${behandlinger.sumOf { it.antall }} " +
                    "fordelt på ${behandlinger.size} steg.")

        val rows = behandlinger.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "steg", it.behandlingssteg.name),
                              it.antall)
        }

        ventendeBehandlingGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 150000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun sendteBrev() {
        if (LeaderClient.isLeader() != true) return
        val data = meldingstellingRepository.finnSendteBrev()
        logger.info("Sendte brev returnerte ${data.sumOf { it.antall }} fordelt på ${data.size} typer/uker.")

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "brevtype", it.brevtype.name,
                                      "uke", it.år.toString() + "-" + it.uke.toString().padStart(2, '0')),
                              it.antall)
        }
        sendteBrevGauge.register(rows)
    }

    @Scheduled(initialDelay = 180000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun vedtak() {
        if (LeaderClient.isLeader() != true) return
        val data = meldingstellingRepository.finnVedtak()
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

    @Scheduled(initialDelay = 210000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun mottatteKravgrunnlagKoblet() {
        val data = meldingstellingRepository.findByType(Meldingstype.KRAVGRUNNLAG)
        logger.info("Mottatte kravgrunnlag koblet returnerte ${data.sumOf { it.antall }} fordelt på ${data.size} ytelser/dager.")

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "type", it.type.name,
                                      "status", it.status.name,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        mottatteKravgrunnlagGauge.register(rows)
    }

    @Scheduled(initialDelay = 240000L, fixedDelay = OPPDATERINGSFREKVENS)
    fun mottatteStatusmeldinger() {
        val data = meldingstellingRepository.summerAntallForType(Meldingstype.STATUSMELDING)
        logger.info("Mottatte statusmeldinger returnerte ${data.sumOf { it.antall }} fordelt på ${data.size} ytelse/dager.")

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        mottatteStatusmeldingerGauge.register(rows)
    }

    companion object {

        const val OPPDATERINGSFREKVENS = 1800 * 1000L
    }
}
