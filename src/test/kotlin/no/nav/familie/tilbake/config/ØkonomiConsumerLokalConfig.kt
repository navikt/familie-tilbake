package no.nav.familie.tilbake.config

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagAnnulerRequest
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagAnnulerResponse
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentDetaljRequest
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentDetaljResponse
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentListeRequest
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentListeResponse
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingPortType
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagBelopDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagPeriodeDto
import no.nav.tilbakekreving.typer.v1.MmelDto
import no.nav.tilbakekreving.typer.v1.PeriodeDto
import no.nav.tilbakekreving.typer.v1.TypeGjelderDto
import no.nav.tilbakekreving.typer.v1.TypeKlasseDto
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Configuration
@ComponentScan(value = ["no.nav.familie.tilbake.kravgrunnlag"])
@Profile("mock-økonomi")
class ØkonomiConsumerLokalConfig(private val kravgrunnlagRepository: KravgrunnlagRepository) {

    @Bean
    @Primary
    fun økonomiService(): TilbakekrevingPortType {
        return ØkonomiMockService(kravgrunnlagRepository)
    }

    internal class ØkonomiMockService(private val kravgrunnlagRepository: KravgrunnlagRepository) : TilbakekrevingPortType {

        override fun tilbakekrevingsvedtak(request: TilbakekrevingsvedtakRequest): TilbakekrevingsvedtakResponse {
            val respons = TilbakekrevingsvedtakResponse()
            respons.tilbakekrevingsvedtak = request.tilbakekrevingsvedtak
            respons.mmel = lagMmelDto()

            return respons
        }


        override fun kravgrunnlagHentListe(request: KravgrunnlagHentListeRequest): KravgrunnlagHentListeResponse {
            throw Feil(message = "Ikke implementert ennå")
        }

        override fun kravgrunnlagHentDetalj(request: KravgrunnlagHentDetaljRequest): KravgrunnlagHentDetaljResponse {
            val hentKravgrunnlagRequest = request.hentkravgrunnlag
            val eksisterendeKravgrunnlag = kravgrunnlagRepository
                    .findByEksternKravgrunnlagIdAndAktivIsTrue(hentKravgrunnlagRequest.kravgrunnlagId)
            val respons = KravgrunnlagHentDetaljResponse()
            respons.mmel = lagMmelDto()

            respons.detaljertkravgrunnlag = DetaljertKravgrunnlagDto().apply {
                kravgrunnlagId = hentKravgrunnlagRequest.kravgrunnlagId
                enhetAnsvarlig = hentKravgrunnlagRequest.enhetAnsvarlig
                enhetBehandl = hentKravgrunnlagRequest.enhetAnsvarlig
                enhetBosted = hentKravgrunnlagRequest.enhetAnsvarlig
                saksbehId = hentKravgrunnlagRequest.saksbehId
                kodeFagomraade = Fagområdekode.BA.name
                vedtakId = eksisterendeKravgrunnlag?.vedtakId ?: BigInteger.ZERO
                kodeStatusKrav = Kravstatuskode.NYTT.kode
                fagsystemId = eksisterendeKravgrunnlag?.fagsystemId ?: "0"
                datoVedtakFagsystem = eksisterendeKravgrunnlag?.fagsystemVedtaksdato ?: LocalDate.now()
                vedtakIdOmgjort = eksisterendeKravgrunnlag?.omgjortVedtakId ?: BigInteger.ZERO
                vedtakGjelderId = eksisterendeKravgrunnlag?.gjelderVedtakId ?: "1234"
                typeGjelderId = TypeGjelderDto.PERSON
                utbetalesTilId = eksisterendeKravgrunnlag?.utbetalesTilId ?: "1234"
                typeUtbetId = TypeGjelderDto.PERSON
                kontrollfelt = eksisterendeKravgrunnlag?.kontrollfelt ?: LocalDateTime.now()
                        .format(DateTimeFormatter.ofPattern("YYYY-MM-dd-HH.mm.ss.SSSSS"))
                referanse = eksisterendeKravgrunnlag?.referanse ?: "0"
                tilbakekrevingsPeriode.addAll(mapPeriode(eksisterendeKravgrunnlag?.perioder
                                                         ?: Testdata.kravgrunnlag431.perioder))
            }
            return respons
        }

        override fun kravgrunnlagAnnuler(request: KravgrunnlagAnnulerRequest): KravgrunnlagAnnulerResponse {
            throw Feil(message = "Ikke implementert ennå")
        }

        private fun lagMmelDto(): MmelDto {
            val mmelDto = MmelDto()
            mmelDto.alvorlighetsgrad = "00"
            mmelDto.kodeMelding = "OK"
            return mmelDto
        }

        private fun mapPeriode(perioder: Set<Kravgrunnlagsperiode432>): List<DetaljertKravgrunnlagPeriodeDto> {
            return perioder.map {
                DetaljertKravgrunnlagPeriodeDto().apply {
                    periode = PeriodeDto().apply {
                        fom = it.periode.fomDato
                        tom = it.periode.tomDato
                    }
                    belopSkattMnd = it.månedligSkattebeløp
                    tilbakekrevingsBelop.addAll(mapBeløp(it.beløp))
                }
            }
        }

        private fun mapBeløp(beløper: Set<Kravgrunnlagsbeløp433>): List<DetaljertKravgrunnlagBelopDto> {
            return beløper.map {
                DetaljertKravgrunnlagBelopDto().apply {
                    kodeKlasse = it.klassekode.name
                    typeKlasse = TypeKlasseDto.fromValue(it.klassetype.name)
                    belopNy = it.nyttBeløp
                    belopOpprUtbet = it.opprinneligUtbetalingsbeløp
                    belopUinnkrevd = it.uinnkrevdBeløp
                    belopTilbakekreves = it.tilbakekrevesBeløp
                    skattProsent = it.skatteprosent
                }
            }
        }
    }

}
