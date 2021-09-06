package no.nav.familie.tilbake.micrometer

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
import no.nav.familie.leader.LeaderClient
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.micrometer.domain.StatistikkRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service


@Service
class StatistikkService(private val statistikkRepository: StatistikkRepository) {

    val åpneBehandlingerGauge = MultiGauge.builder("UavsluttedeBehandlinger").register(Metrics.globalRegistry)
    val klarTilBehandlingGauge = MultiGauge.builder("KlarTilBehandling").register(Metrics.globalRegistry)
    val ventendeBehandlingGauge = MultiGauge.builder("VentendeBehandlinger").register(Metrics.globalRegistry)
    val vedtakDelvisTilbakebetalingGauge = MultiGauge.builder("VedtakDelvisTilbakebetaling").register(Metrics.globalRegistry)
    val vedtakFullTilbakebetalingGauge = MultiGauge.builder("VedtakFullTilbakebetaling").register(Metrics.globalRegistry)
    val vedtakIngenTilbakebetalingGauge = MultiGauge.builder("VedtakIngenTilbakebetaling").register(Metrics.globalRegistry)
    val vedtakHenlagteGauge = MultiGauge.builder("VedtakHenlagte").register(Metrics.globalRegistry)
    val kobledeKravgrunnlagGauge = MultiGauge.builder("KobledeKravgrunnlag").register(Metrics.globalRegistry)
    val ukobledeKravgrunnlagGauge = MultiGauge.builder("UkobledeKravgrunnlag").register(Metrics.globalRegistry)
    val sendteVarselbrevGauge = MultiGauge.builder("SendteVarselbrev").register(Metrics.globalRegistry)
    val sendteKorrigerteVarselbrevGauge = MultiGauge.builder("SendteKorrigerteVarselbrev").register(Metrics.globalRegistry)
    val sendteVedtaksbrevGauge = MultiGauge.builder("SendteVedtaksbrev").register(Metrics.globalRegistry)
    val sendteHenleggelsesbrevGauge = MultiGauge.builder("SendteHenleggelsesbrev").register(Metrics.globalRegistry)
    val sendInnhentDokumentasjonsbrevGauge = MultiGauge.builder("SendInnhentDokumentasjonsbrev").register(Metrics.globalRegistry)
    val sendteBrevGauge = MultiGauge.builder("SendteBrev").register(Metrics.globalRegistry)
    val vedtakGauge = MultiGauge.builder("Vedtak").register(Metrics.globalRegistry)

    @Scheduled(initialDelay = 10000, fixedDelay = 30000)
    fun åpneBehandlinger() {
        if (LeaderClient.isLeader() != true) return
        val behandlinger = statistikkRepository.finnÅpneBehandlinger()

        val rows = behandlinger.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }

        åpneBehandlingerGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 11000, fixedDelay = 30000)
    fun behandlingerKlarTilSaksbehandling() {
        if (LeaderClient.isLeader() != true) return
        val behandlinger = statistikkRepository.finnKlarTilBehandling()

        val rows = behandlinger.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "steg", it.behandlingssteg.name),
                              it.antall)
        }

        klarTilBehandlingGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 12000, fixedDelay = 30000)
    fun behandlingerPåVent() {
        if (LeaderClient.isLeader() != true) return
        val behandlinger = statistikkRepository.finnVentendeBehandlinger()

        val rows = behandlinger.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "steg", it.behandlingssteg.name),
                              it.antall)
        }

        ventendeBehandlingGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 13000, fixedDelay = 30000)
    fun vedtakDelvisTilbakebetaling() {
        if (LeaderClient.isLeader() != true) return
        val data = statistikkRepository.finnVedtakDelvisTilbakebetaling()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        vedtakDelvisTilbakebetalingGauge.register(rows)
    }

    @Scheduled(initialDelay = 14000, fixedDelay = 30000)
    fun vedtakFullTilbakebetaling() {
        if (LeaderClient.isLeader() != true) return
        val data = statistikkRepository.finnVedtakFullTilbakebetaling()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        vedtakFullTilbakebetalingGauge.register(rows)
    }

    @Scheduled(initialDelay = 15000, fixedDelay = 30000)
    fun vedtakIngenTilbakebetaling() {
        if (LeaderClient.isLeader() != true) return
        val data = statistikkRepository.finnVedtakIngenTilbakebetaling()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        vedtakIngenTilbakebetalingGauge.register(rows)
    }

    @Scheduled(initialDelay = 16000, fixedDelay = 30000)
    fun vedtakHenlagte() {
        if (LeaderClient.isLeader() != true) return
        val data = statistikkRepository.finnVedtakHenlagte()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        vedtakHenlagteGauge.register(rows)
    }

    @Scheduled(initialDelay = 17000, fixedDelay = 30000)
    fun kobledeKravgrunnlag() {
        val data = statistikkRepository.finnKobledeKravgrunnlag()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        kobledeKravgrunnlagGauge.register(rows)
    }

    @Scheduled(initialDelay = 18000, fixedDelay = 30000)
    fun ukobledeKravgrunnlag() {
        if (LeaderClient.isLeader() != true) return
        val data = statistikkRepository.finnUkobledeKravgrunnlag()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        ukobledeKravgrunnlagGauge.register(rows)
    }

    @Scheduled(initialDelay = 19000, fixedDelay = 30000)
    fun sendteVarselbrev() {
        if (LeaderClient.isLeader() != true) return
        val data = statistikkRepository.finnSendteVarselbrev()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        sendteVarselbrevGauge.register(rows)
    }

    @Scheduled(initialDelay = 20000, fixedDelay = 30000)
    fun sendteKorrigerteVarselbrev() {
        if (LeaderClient.isLeader() != true) return
        val data = statistikkRepository.finnSendteKorrigerteVarselbrev()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        sendteKorrigerteVarselbrevGauge.register(rows)
    }

    @Scheduled(initialDelay = 21000, fixedDelay = 30000)
    fun sendteVedtaksbrev() {
        if (LeaderClient.isLeader() != true) return
        val data = statistikkRepository.finnSendteVedtaksbrev()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        sendteVedtaksbrevGauge.register(rows)
    }

    @Scheduled(initialDelay = 22000, fixedDelay = 30000)
    fun sendteHenleggelsesbrev() {
        if (LeaderClient.isLeader() != true) return
        val data = statistikkRepository.finnSendteHenleggelsesbrev()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        sendteHenleggelsesbrevGauge.register(rows)
    }

    @Scheduled(initialDelay = 23000, fixedDelay = 30000)
    fun sendInnhentDokumentasjonsbrev() {
        if (LeaderClient.isLeader() != true) return
        val data = statistikkRepository.finnSendteInnhentDokumentasjonsbrev()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        sendInnhentDokumentasjonsbrevGauge.register(rows)
    }

    @Scheduled(initialDelay = 23000, fixedDelay = 30000)
    fun sendteBrev() {
        if (LeaderClient.isLeader() != true) return
        val data = statistikkRepository.finnSendteBrev()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "brevtype", it.brevtype.name,
                                      "dato", it.dato.toString()), it.antall)
        }
        sendteBrevGauge.register(rows)
    }

    @Scheduled(initialDelay = 16000, fixedDelay = 30000)
    fun vedtak() {
        if (LeaderClient.isLeader() != true) return
        val data = statistikkRepository.finnVedtak()

        val rows = data.map {

            val vedtakstype = if (it.vedtakstype in Behandlingsresultat.ALLE_HENLEGGELSESKODER)
                Behandlingsresultatstype.HENLAGT.navn else it.vedtakstype.navn

            MultiGauge.Row.of(Tags.of("ytelse", it.ytelsestype.kode,
                                      "vedtakstype", vedtakstype,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        vedtakGauge.register(rows)
    }

}