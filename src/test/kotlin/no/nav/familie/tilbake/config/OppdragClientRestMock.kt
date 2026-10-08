package no.nav.familie.tilbake.config

import io.kotest.inspectors.forOne
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.tilbakekreving.integrasjoner.oppdrag.OppdragRestClient
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.DetaljerPeriodeDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.DetaljerPosteringDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.HentKravgrunnlagDetaljerResponseDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.KodeAksjonDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.KravgrunnlagAnnulerResponseDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.KravgrunnlagDetaljerDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.TilbakekrevingsvedtakRequestDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.TilbakekrevingsvedtakResponseDto
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.util.kroner
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

@Primary
@Service
class OppdragClientRestMock : OppdragRestClient {
    private val iverksettelseRequests = ConcurrentLinkedQueue<TilbakekrevingsvedtakRequestDto>()

    private val mockIverksettelseSvar = mutableMapOf<BigInteger, TilbakekrevingsvedtakResponseDto>()

    private val mockKravgrunnlagPerioder = ConcurrentHashMap<BigInteger, List<DetaljerPeriodeDto>>()

    override fun iverksettVedtak(request: TilbakekrevingsvedtakRequestDto): TilbakekrevingsvedtakResponseDto {
        iverksettelseRequests.add(request)
        return mockIverksettelseSvar.remove(request.vedtakId) ?: TilbakekrevingsvedtakResponseDto(
            status = 0,
            melding = "OK",
            vedtakId = request.vedtakId,
            datoVedtakFagsystem = request.vedtaksDato,
        )
    }

    override fun hentKravgrunnlag(kravgrunnlagId: BigInteger, kodeAksjon: KodeAksjonDto): HentKravgrunnlagDetaljerResponseDto {
        return HentKravgrunnlagDetaljerResponseDto(
            status = 0,
            melding = "OK",
            kravgrunnlag = KravgrunnlagDetaljerDto(
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
                datoVedtakFagsystem = 1.januar(2021),
                vedtakIdOmgjort = 0,
                gjelderId = "1234",
                typeGjelder = "PERSON",
                utbetalesTilId = "1234",
                typeUtbetalesTilId = "PERSON",
                kontrollfelt = 1.januar(2021).atTime(12, 0).format(DateTimeFormatter.ofPattern("YYYY-MM-dd-HH.mm.ss.SSSSSS")),
                referanse = "0",
                perioder = mockKravgrunnlagPerioder.remove(kravgrunnlagId) ?: listOf(kravgrunnlagPeriode(1.januar(2021) til 31.januar(2021), 1000.kroner)),
            ),
        )
    }

    override fun annullerKravgrunnlag(vedtakId: BigInteger): KravgrunnlagAnnulerResponseDto = KravgrunnlagAnnulerResponseDto(
        status = 0,
        melding = "OK",
        vedtakId = vedtakId.toInt(),
        saksbehandlerId = "8020",
    )

    fun shouldHaveIverksettelse(
        vedtakId: BigInteger,
        callback: (vedtak: TilbakekrevingsvedtakRequestDto) -> Unit = {},
    ) {
        iverksettelseRequests.forOne { it.vedtakId shouldBe vedtakId }
        callback(iverksettelseRequests.single { it.vedtakId == vedtakId })
    }

    fun mockHentKravgrunnlag(kravgrunnlagId: BigInteger, perioder: List<DetaljerPeriodeDto>) {
        mockKravgrunnlagPerioder[kravgrunnlagId] = perioder
    }

    internal fun mockIversettelse(vedtakId: BigInteger, alvorlighetsgrad: String, kodeMelding: String) {
        mockIverksettelseSvar[vedtakId] = TilbakekrevingsvedtakResponseDto(
            status = alvorlighetsgrad.toInt(),
            melding = kodeMelding,
            vedtakId = vedtakId,
            datoVedtakFagsystem = LocalDate.now(),
        )
    }

    companion object {
        fun kravgrunnlagPeriode(
            periode: Datoperiode,
            feilutbetaltBeløp: BigDecimal,
        ) = DetaljerPeriodeDto(
            periodeFom = periode.fom,
            periodeTom = periode.tom,
            belopSkattMnd = BigDecimal.ZERO,
            posteringer = listOf(
                DetaljerPosteringDto(
                    kodeKlasse = Klassekode.KL_KODE_FEIL_BA.tilKlassekodeNavn(),
                    typeKlasse = "FEIL",
                    belopTilbakekreves = feilutbetaltBeløp,
                    belopNy = feilutbetaltBeløp,
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
                    belopTilbakekreves = feilutbetaltBeløp,
                    belopNy = 20000.kroner - feilutbetaltBeløp,
                    belopOpprinneligUtbetalt = 20000.kroner,
                    belopUinnkrevd = BigDecimal.ZERO,
                    skattProsent = BigDecimal.ZERO,
                    kodeResultat = "",
                    kodeAarsak = "",
                    kodeSkyld = "",
                ),
            ),
        )
    }
}
