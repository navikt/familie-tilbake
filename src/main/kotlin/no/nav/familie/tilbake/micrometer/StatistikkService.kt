package no.nav.familie.tilbake.micrometer

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tags
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


    /**
     * åpne behandlinger, sum og aldersfordelt, fordelt på ytelse - dette gir til enhver tid overblikk over hele saksmassen i
     * applikasjonen. Viser i en periode også tilgangen på nye behandlinger (før saksbehandlerne tar tak i behandlingen).
     * Er et øyeblikksbilde som må friskes opp med jevne mellomrom.
     **/
    @Scheduled(initialDelay = 10000, fixedDelay = 30000)
    fun åpneBehandlinger() {
        val behandlinger = statistikkRepository.finnÅpneBehandlinger()

        val rows = behandlinger.map {
            MultiGauge.Row.of(Tags.of("Ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }

        åpneBehandlingerGauge.register(rows, true)
    }

    /**
    behandlinger som kan saksbehandles og hvor de er i prosessen - dette gir ytterligere innsikt i de åpne behandlingene.
    hvilke er klare for å saksbehandles eller er under saksbehandling. Er et øyeblikksbilde som må friskes opp med jevne mellomrom.
     **/
    @Scheduled(initialDelay = 11000, fixedDelay = 30000)
    fun behandlingerKlarTilSaksbehandling() {
        val behandlinger = statistikkRepository.finnKlarTilBehandling()

        val rows = behandlinger.map {
            MultiGauge.Row.of(Tags.of("Ytelse", it.ytelsestype.kode,
                                      "steg", it.behandlingssteg.name),
                              it.antall)
        }

        klarTilBehandlingGauge.register(rows, true)
    }

    /**
    behandlinger som ikke kan saksbehandles med årsak til at de ligger på vent -  gir tilsvarende innsikt i behandlinger som
    venter av ulike årsaker. Er et øyeblikksbilde som må friskes opp med jevne mellomrom.
     **/
    @Scheduled(initialDelay = 12000, fixedDelay = 30000)
    fun behandlingerPåVent() {
        val behandlinger = statistikkRepository.finnVentendeBehandlinger()

        val rows = behandlinger.map {
            MultiGauge.Row.of(Tags.of("Ytelse", it.ytelsestype.kode,
                                      "steg", it.behandlingssteg.name),
                              it.antall)
        }

        ventendeBehandlingGauge.register(rows, true)
    }

    @Scheduled(initialDelay = 13000, fixedDelay = 30000)
    fun vedtakDelvisTilbakebetaling() {
        val data = statistikkRepository.finnVedtakDelvisTilbakebetaling()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("Ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        vedtakDelvisTilbakebetalingGauge.register(rows)
    }

    @Scheduled(initialDelay = 14000, fixedDelay = 30000)
    fun vedtakFullTilbakebetaling() {
        val data = statistikkRepository.finnVedtakFullTilbakebetaling()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("Ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        vedtakFullTilbakebetalingGauge.register(rows)
    }

    @Scheduled(initialDelay = 15000, fixedDelay = 30000)
    fun vedtakIngenTilbakebetaling() {
        val data = statistikkRepository.finnVedtakIngenTilbakebetaling()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("Ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        vedtakIngenTilbakebetalingGauge.register(rows)
    }

    @Scheduled(initialDelay = 16000, fixedDelay = 30000)
    fun vedtakHenlagte() {
        val data = statistikkRepository.finnVedtakHenlagte()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("Ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        vedtakHenlagteGauge.register(rows)
    }

    @Scheduled(initialDelay = 17000, fixedDelay = 30000)
    fun kobledeKravgrunnlag() {
        val data = statistikkRepository.finnKobledeKravgrunnlag()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("Ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        kobledeKravgrunnlagGauge.register(rows)
    }

    @Scheduled(initialDelay = 18000, fixedDelay = 30000)
    fun ukobledeKravgrunnlag() {
        val data = statistikkRepository.finnUkobledeKravgrunnlag()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("Ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        ukobledeKravgrunnlagGauge.register(rows)
    }

    @Scheduled(initialDelay = 19000, fixedDelay = 30000)
    fun sendteVarselbrev() {
        val data = statistikkRepository.finnSendteVarselbrev()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("Ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        sendteVarselbrevGauge.register(rows)
    }

    @Scheduled(initialDelay = 20000, fixedDelay = 30000)
    fun sendteKorrigerteVarselbrev() {
        val data = statistikkRepository.finnSendteKorrigerteVarselbrev()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("Ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        sendteKorrigerteVarselbrevGauge.register(rows)
    }

    @Scheduled(initialDelay = 21000, fixedDelay = 30000)
    fun sendteVedtaksbrev() {
        val data = statistikkRepository.finnSendteVedtaksbrev()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("Ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        sendteVedtaksbrevGauge.register(rows)
    }

    @Scheduled(initialDelay = 22000, fixedDelay = 30000)
    fun sendteHenleggelsesbrev() {
        val data = statistikkRepository.finnSendteHenleggelsesbrev()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("Ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        sendteHenleggelsesbrevGauge.register(rows)
    }

    @Scheduled(initialDelay = 23000, fixedDelay = 30000)
    fun sendInnhentDokumentasjonsbrev() {
        val data = statistikkRepository.finnSendteInnhentDokumentasjonsbrev()

        val rows = data.map {
            MultiGauge.Row.of(Tags.of("Ytelse", it.ytelsestype.kode,
                                      "dato", it.dato.toString()),
                              it.antall)
        }
        sendInnhentDokumentasjonsbrevGauge.register(rows)
    }
}