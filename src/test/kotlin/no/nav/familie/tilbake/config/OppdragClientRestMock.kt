package no.nav.familie.tilbake.config

import io.kotest.inspectors.forOne
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.tilbakekreving.integrasjoner.oppdrag.OppdragRestClient
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.DetaljerPeriodeDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.DetaljerPosteringDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.KravgrunnlagDetaljerDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.TilbakekrevingsvedtakRequestDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.TilbakekrevingsvedtakResponseDto
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.util.kroner
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentLinkedQueue

@Primary
@Service
class OppdragClientRestMock : OppdragRestClient {
    private val iverksettelseRequests = ConcurrentLinkedQueue<TilbakekrevingsvedtakRequestDto>()

    override fun iverksettVedtak(request: TilbakekrevingsvedtakRequestDto): TilbakekrevingsvedtakResponseDto {
        iverksettelseRequests.add(request)
        return TilbakekrevingsvedtakResponseDto(
            status = 0,
            melding = "OK",
            vedtakId = request.vedtakId,
            datoVedtakFagsystem = request.vedtaksDato,
        )
    }

    override fun hentKravgrunnlag(kravgrunnlagId: BigInteger, kodeAksjon: String): KravgrunnlagDetaljerDto {
        return KravgrunnlagDetaljerDto(
            kodeHjemmel = "22-15",
            renterBeregnes = true,
            kravgrunnlagId = kravgrunnlagId.toLong(),
            enhetAnsvarlig = "",
            enhetBehandl = "",
            enhetBosted = "",
            saksbehandlerId = "",
            kodeFagomraade = Fagområdekode.BA.name,
            vedtakId = 0,
            kodeStatusKrav = Kravstatuskode.NYTT.kode,
            fagsystemId = "0",
            datoVedtakFagsystem = LocalDate.now(),
            vedtakIdOmgjort = 0,
            gjelderId = "1234",
            typeGjelder = "PERSON",
            utbetalesTilId = "1234",
            typeUtbetalesTilId = "PERSON",
            kontrollfelt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYY-MM-dd-HH.mm.ss.SSSSSS")),
            referanse = "0",
            perioder = listOf(
                DetaljerPeriodeDto(
                    periodeFom = 1.januar(2021),
                    periodeTom = 31.januar(2021),
                    belopSkattMnd = BigDecimal.ZERO,
                    posteringer = listOf(
                        DetaljerPosteringDto(
                            kodeKlasse = Klassekode.KL_KODE_FEIL_BA.tilKlassekodeNavn(),
                            typeKlasse = "FEIL",
                            belopTilbakekreves = 1000.kroner,
                            belopNy = 1000.kroner,
                            belopOpprinneligUtbetalt = BigDecimal.ZERO,
                            belopUinnkrevd = BigDecimal.ZERO,
                            skattProsent = BigDecimal.ZERO,
                            kodeResultat = "",
                            kodeAarsak = "",
                            kodeSkyld = "",
                        ),
                        DetaljerPosteringDto(
                            kodeKlasse = Klassekode.KL_KODE_JUST_BA.tilKlassekodeNavn(),
                            typeKlasse = "YTEL",
                            belopTilbakekreves = 1000.kroner,
                            belopNy = 19000.kroner,
                            belopOpprinneligUtbetalt = 20000.kroner,
                            belopUinnkrevd = BigDecimal.ZERO,
                            skattProsent = BigDecimal.ZERO,
                            kodeResultat = "",
                            kodeAarsak = "",
                            kodeSkyld = "",
                        ),
                    ),
                ),
            ),
        )
    }

    fun shouldHaveIverksettelse(
        vedtakId: BigInteger,
        callback: (vedtak: TilbakekrevingsvedtakRequestDto) -> Unit = {},
    ) {
        iverksettelseRequests.forOne { it.vedtakId shouldBe vedtakId }
        callback(iverksettelseRequests.single { it.vedtakId == vedtakId })
    }
}
