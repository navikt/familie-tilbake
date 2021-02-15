package no.nav.familie.tilbake.integration.pdl


import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.http.sts.StsRestClient
import no.nav.familie.kontrakter.felles.journalpost.Tema
import no.nav.familie.tilbake.behandling.domain.Fagsystem
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.PdlConfig
import no.nav.familie.tilbake.integration.pdl.internal.PdlHentPersonResponse
import no.nav.familie.tilbake.integration.pdl.internal.PdlPerson
import no.nav.familie.tilbake.integration.pdl.internal.PdlPersonRequest
import no.nav.familie.tilbake.integration.pdl.internal.PdlPersonRequestVariables
import no.nav.familie.tilbake.integration.pdl.internal.PersonInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.time.LocalDate

@Service
class PdlClient(val pdlConfig: PdlConfig,
                @Qualifier("sts") val restTemplate: RestOperations,
                private val stsRestClient: StsRestClient) : AbstractRestClient(restTemplate, "pdl.personinfo") {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    fun hentPersoninfo(personIdent: String, fagsystem: Fagsystem): PersonInfo {
        val pdlPersonRequest = PdlPersonRequest(variables = PdlPersonRequestVariables(personIdent),
                                                query = PdlConfig.hentEnkelPersonQuery)
        val respons: PdlHentPersonResponse<PdlPerson> = postForEntity(pdlConfig.pdlUri,
                                                                      pdlPersonRequest,
                                                                      httpHeaders(fagsystem))
        if (!respons.harFeil()) {
            return Result.runCatching {
                respons.data.person!!.let {
                    PersonInfo(fødselsdato = LocalDate.parse(it.fødsel.first().fødselsdato!!),
                               navn = it.navn.first().fulltNavn(),
                               kjønn = it.kjønn.first().kjønn)
                }
            }.fold(
                    onSuccess = { it },
                    onFailure = {
                        throw Feil(message = "Fant ikke forespurte data på person.",
                                   frontendFeilmelding = "Kunne ikke slå opp data for person $personIdent",
                                   httpStatus = HttpStatus.NOT_FOUND,
                                   throwable = it)
                    }
            )
        } else {
            logger.warn("Respons fra PDL:$respons")
            throw Feil(message = "Feil ved oppslag på person: ${respons.errorMessages()}",
                       frontendFeilmelding = "Feil ved oppslag på person $personIdent: ${respons.errorMessages()}",
                       httpStatus = HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    private fun httpHeaders(fagsystem: Fagsystem): HttpHeaders {

        return HttpHeaders().apply {
            add("Nav-Consumer-Token", "Bearer ${stsRestClient.systemOIDCToken}")
            add("Tema", hentTema(fagsystem).name)
        }
    }

    private fun hentTema(fagsystem: Fagsystem): Tema {
        return Tema.valueOf(fagsystem.tema)
    }
}
