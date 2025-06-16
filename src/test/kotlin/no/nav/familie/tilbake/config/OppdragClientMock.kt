package no.nav.familie.tilbake.config

import no.nav.familie.tilbake.integration.økonomi.OppdragClient
import no.nav.familie.tilbake.kontrakter.simulering.FeilutbetalingerFraSimulering
import no.nav.familie.tilbake.kontrakter.simulering.HentFeilutbetalingerFraSimuleringRequest
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.log.SecureLog
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagAnnulerRequest
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentDetaljRequest
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagBelopDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagPeriodeDto
import no.nav.tilbakekreving.typer.v1.MmelDto
import no.nav.tilbakekreving.typer.v1.PeriodeDto
import no.nav.tilbakekreving.typer.v1.TypeGjelderDto
import no.nav.tilbakekreving.typer.v1.TypeKlasseDto
import no.nav.tilbakekreving.util.kroner
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Primary
@Service
class OppdragClientMock : OppdragClient {
    private val iverksettelseRequests = mutableMapOf<UUID, TilbakekrevingsvedtakRequest>()
    private val hentKravgrunnlagRequests = mutableMapOf<BigInteger, KravgrunnlagHentDetaljRequest>()

    override fun iverksettVedtak(
        behandlingId: UUID,
        tilbakekrevingsvedtakRequest: TilbakekrevingsvedtakRequest,
        logContext: SecureLog.Context,
    ): TilbakekrevingsvedtakResponse {
        iverksettelseRequests[behandlingId] = tilbakekrevingsvedtakRequest
        return TilbakekrevingsvedtakResponse().apply {
            mmel = MmelDto().apply {
                alvorlighetsgrad = "00"
            }
        }
    }

    override fun hentKravgrunnlag(
        kravgrunnlagId: BigInteger,
        hentKravgrunnlagRequest: KravgrunnlagHentDetaljRequest,
        logContext: SecureLog.Context,
    ): DetaljertKravgrunnlagDto {
        val detaljer = hentKravgrunnlagRequest.hentkravgrunnlag
        hentKravgrunnlagRequests[kravgrunnlagId] = hentKravgrunnlagRequest
        return DetaljertKravgrunnlagDto().apply {
            this.kravgrunnlagId = detaljer.kravgrunnlagId
            this.enhetAnsvarlig = detaljer.enhetAnsvarlig
            this.enhetBehandl = detaljer.enhetAnsvarlig
            this.enhetBosted = detaljer.enhetAnsvarlig
            this.saksbehId = detaljer.saksbehId
            this.kodeFagomraade = Fagområdekode.BA.name
            this.vedtakId = BigInteger.ZERO
            this.kodeStatusKrav = Kravstatuskode.NYTT.kode
            this.fagsystemId = "0"
            this.datoVedtakFagsystem = LocalDate.now()
            this.vedtakIdOmgjort = BigInteger.ZERO
            this.vedtakGjelderId = "1234"
            this.typeGjelderId = TypeGjelderDto.PERSON
            this.utbetalesTilId = "1234"
            this.typeUtbetId = TypeGjelderDto.PERSON
            this.kontrollfelt = LocalDateTime
                .now()
                .format(DateTimeFormatter.ofPattern("YYYY-MM-dd-HH.mm.ss.SSSSSS"))
            this.referanse = "0"
            this.tilbakekrevingsPeriode.add(
                DetaljertKravgrunnlagPeriodeDto().apply {
                    this.periode = PeriodeDto().apply {
                        this.fom = 1.januar(2021)
                        this.tom = 31.januar(2021)
                    }
                    this.belopSkattMnd = BigDecimal.ZERO
                    this.tilbakekrevingsBelop.add(
                        DetaljertKravgrunnlagBelopDto().apply {
                            this.kodeKlasse = Klassekode.KL_KODE_FEIL_BA.tilKlassekodeNavn()
                            this.typeKlasse = TypeKlasseDto.FEIL
                            this.belopTilbakekreves = 1000.kroner
                            this.belopNy = 1000.kroner
                            this.belopOpprUtbet = BigDecimal.ZERO
                            this.belopUinnkrevd = BigDecimal.ZERO
                            this.skattProsent = BigDecimal.ZERO
                        },
                    )
                    this.tilbakekrevingsBelop.add(
                        DetaljertKravgrunnlagBelopDto().apply {
                            this.kodeKlasse = Klassekode.KL_KODE_JUST_BA.tilKlassekodeNavn()
                            this.typeKlasse = TypeKlasseDto.YTEL
                            this.belopTilbakekreves = 1000.kroner
                            this.belopNy = 19000.kroner
                            this.belopOpprUtbet = 20000.kroner
                            this.belopUinnkrevd = BigDecimal.ZERO
                            this.skattProsent = BigDecimal.ZERO
                        },
                    )
                },
            )
        }
    }

    override fun annulerKravgrunnlag(
        eksternKravgrunnlagId: BigInteger,
        kravgrunnlagAnnulerRequest: KravgrunnlagAnnulerRequest,
        logContext: SecureLog.Context,
    ) {
    }

    override fun hentFeilutbetalingerFraSimulering(
        request: HentFeilutbetalingerFraSimuleringRequest,
        logContext: SecureLog.Context,
    ): FeilutbetalingerFraSimulering {
        TODO("Not yet implemented")
    }
}
