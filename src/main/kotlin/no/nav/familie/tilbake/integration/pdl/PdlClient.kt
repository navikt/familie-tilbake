package no.nav.familie.tilbake.integration.pdl


import no.nav.familie.http.client.AbstractPingableRestClient
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.PdlConfig
import no.nav.familie.tilbake.integration.pdl.internal.PdlHentIdenterResponse
import no.nav.familie.tilbake.integration.pdl.internal.PdlHentPersonResponse
import no.nav.familie.tilbake.integration.pdl.internal.PdlPerson
import no.nav.familie.tilbake.integration.pdl.internal.PdlPersonRequest
import no.nav.familie.tilbake.integration.pdl.internal.PdlPersonRequestVariables
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.net.URI
import java.time.LocalDate

@Service
class PdlClient(val pdlConfig: PdlConfig,
                @Qualifier("azureClientCredential") restTemplate: RestOperations)
    : AbstractPingableRestClient(restTemplate, "pdl.personinfo") {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)


    fun hentPersoninfo(ident: String, fagsystem: Fagsystem): Personinfo {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(ident),
                                                query = PdlConfig.hentEnkelPersonQuery)
        val respons: PdlHentPersonResponse<PdlPerson> = postForEntity(pdlConfig.pdlUri,
                                                                      pdlPersonRequest,
                                                                      httpHeaders(fagsystem))
        if (!respons.harFeil()) {
            return respons.data.person!!.let {
                Personinfo(ident = ident,
                           fødselsdato = LocalDate.parse(it.fødsel.first().fødselsdato!!),
                           navn = it.navn.first().fulltNavn(),
                           kjønn = it.kjønn.first().kjønn)
            }
        } else {
            logger.warn("Respons fra PDL:${objectMapper.writeValueAsString(respons)}")
            throw Feil(message = "Feil ved oppslag på person: ${respons.errorMessages()}",
                       frontendFeilmelding = "Feil ved oppslag på person $ident: ${respons.errorMessages()}",
                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    fun hentIdenter(personIdent: String, fagsystem: Fagsystem): PdlHentIdenterResponse {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = PdlConfig.hentIdenterQuery)
        val response = postForEntity<PdlHentIdenterResponse>(pdlConfig.pdlUri,
                                                             pdlPersonRequest,
                                                             httpHeaders(fagsystem))

        if (!response.harFeil()) return response
        throw Feil(message = "Feil mot pdl: ${response.errorMessages()}",
                   frontendFeilmelding = "Fant ikke identer for person $personIdent: ${response.errorMessages()}",
                   httpStatus = HttpStatus.NOT_FOUND)
    }



    private fun httpHeaders(fagsystem: Fagsystem): HttpHeaders {

        return HttpHeaders().apply {
            add("Tema", hentTema(fagsystem).name)
        }
    }

    private fun hentTema(fagsystem: Fagsystem): Tema {
        return Tema.valueOf(fagsystem.tema)
    }

    override val pingUri: URI
        get() = pdlConfig.pdlUri

    override fun ping() {
        operations.optionsForAllow(pingUri)
    }
}

