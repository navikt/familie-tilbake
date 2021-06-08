package no.nav.familie.tilbake.config

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagAnnulerRequest
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagAnnulerResponse
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentDetaljRequest
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentDetaljResponse
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentListeRequest
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentListeResponse
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingPortType
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import no.nav.tilbakekreving.typer.v1.MmelDto
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("mock-økonomi")
class ØkonomiConsumerLokalConfig {

    @Bean
    @Primary
    fun økonomiService(): TilbakekrevingPortType {
        return ØkonomiMockService()
    }

    internal class ØkonomiMockService : TilbakekrevingPortType {

        override fun tilbakekrevingsvedtak(request: TilbakekrevingsvedtakRequest): TilbakekrevingsvedtakResponse {
            val respons = TilbakekrevingsvedtakResponse()
            respons.tilbakekrevingsvedtak = request.tilbakekrevingsvedtak

            val mmelDto = MmelDto()
            mmelDto.alvorlighetsgrad = "00"
            mmelDto.kodeMelding = "OK"
            respons.mmel = mmelDto

            return respons
        }

        override fun kravgrunnlagHentListe(request: KravgrunnlagHentListeRequest): KravgrunnlagHentListeResponse {
            throw Feil(message = "Ikke implementert ennå")
        }

        override fun kravgrunnlagHentDetalj(request: KravgrunnlagHentDetaljRequest): KravgrunnlagHentDetaljResponse {
            throw Feil(message = "Ikke implementert ennå")
        }

        override fun kravgrunnlagAnnuler(request: KravgrunnlagAnnulerRequest): KravgrunnlagAnnulerResponse {
            throw Feil(message = "Ikke implementert ennå")
        }

    }

}
