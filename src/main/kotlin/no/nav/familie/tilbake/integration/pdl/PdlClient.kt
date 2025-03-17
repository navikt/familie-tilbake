package no.nav.familie.tilbake.integration.pdl

import AbstractPingableRestClient
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.PdlConfig
import no.nav.familie.tilbake.integration.pdl.internal.PdlAdressebeskyttelsePerson
import no.nav.familie.tilbake.integration.pdl.internal.PdlBolkResponse
import no.nav.familie.tilbake.integration.pdl.internal.PdlHentIdenterResponse
import no.nav.familie.tilbake.integration.pdl.internal.PdlHentPersonResponse
import no.nav.familie.tilbake.integration.pdl.internal.PdlPerson
import no.nav.familie.tilbake.integration.pdl.internal.PdlPersonBolkRequest
import no.nav.familie.tilbake.integration.pdl.internal.PdlPersonBolkRequestVariables
import no.nav.familie.tilbake.integration.pdl.internal.PdlPersonRequest
import no.nav.familie.tilbake.integration.pdl.internal.PdlPersonRequestVariables
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.integration.pdl.internal.feilsjekkOgReturnerData
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.kontrakter.ytelse.Tema
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import java.time.LocalDate

@Service
class PdlClient(
    private val pdlConfig: PdlConfig,
    @Qualifier("azureClientCredential") restTemplate: RestOperations,
) : AbstractPingableRestClient(restTemplate, "pdl.personinfo") {
    private val logger = TracedLogger.getLogger<PdlClient>()

    fun hentPersoninfo(
        ident: String,
        fagsystem: Fagsystem,
        logContext: SecureLog.Context,
    ): Personinfo {
        val pdlPersonRequest =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(ident),
                query = PdlConfig.hentEnkelPersonQuery,
            )
        val response: PdlHentPersonResponse<PdlPerson> =
            postForEntity(
                pdlConfig.pdlUri,
                pdlPersonRequest,
                httpHeaders(mapTilTema(fagsystem)),
            )
        if (response.harAdvarsel()) {
            logger.medContext(logContext) {
                warn("Advarsel ved henting av personinfo fra PDL. Se securelogs for detaljer.")
            }
            SecureLog.medContext(logContext) {
                warn("Advarsel ved henting av personinfo fra PDL: {}", response.extensions?.warnings.toString())
            }
        }
        if (!response.harFeil()) {
            return response.data.person!!.let {
                val aktivtIdent = it.identer.first().identifikasjonsnummer ?: error("Kan ikke hente aktivt ident fra PDL")
                Personinfo(
                    ident = aktivtIdent,
                    fødselsdato = LocalDate.parse(it.fødsel.first().fødselsdato!!),
                    navn = it.navn.first().fulltNavn(),
                    kjønn = it.kjønn.first().kjønn,
                    dødsdato = it.dødsfall.firstOrNull()?.let { dødsfall -> LocalDate.parse(dødsfall.dødsdato) },
                )
            }
        } else {
            logger.medContext(logContext) {
                warn("Response fra PDL: {}", objectMapper.writeValueAsString(response))
            }
            throw Feil(
                message = "Feil ved oppslag på person: ${response.errorMessages()}",
                frontendFeilmelding = "Feil ved oppslag på person $ident: ${response.errorMessages()}",
                logContext = logContext,
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
            )
        }
    }

    fun hentIdenter(
        personIdent: String,
        fagsystem: Fagsystem,
        logContext: SecureLog.Context,
    ): PdlHentIdenterResponse {
        val pdlPersonRequest =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(personIdent),
                query = PdlConfig.hentIdenterQuery,
            )
        val response =
            postForEntity<PdlHentIdenterResponse>(
                pdlConfig.pdlUri,
                pdlPersonRequest,
                httpHeaders(mapTilTema(fagsystem)),
            )
        if (response.harAdvarsel()) {
            logger.medContext(logContext) {
                warn("Advarsel ved henting av personidenter fra PDL. Se securelogs for detaljer.")
            }
            SecureLog.medContext(logContext) {
                warn("Advarsel ved henting av personidenter fra PDL: {}", response.extensions?.warnings.toString())
            }
        }
        if (!response.harFeil()) return response
        throw Feil(
            message = "Feil mot pdl: ${response.errorMessages()}",
            frontendFeilmelding = "Fant ikke identer for person $personIdent: ${response.errorMessages()}",
            logContext = logContext,
            httpStatus = HttpStatus.NOT_FOUND,
        )
    }

    fun hentAdressebeskyttelseBolk(
        personIdentList: List<String>,
        fagsystem: Fagsystem,
        logContext: SecureLog.Context,
    ): Map<String, PdlAdressebeskyttelsePerson> {
        val pdlRequest =
            PdlPersonBolkRequest(
                variables = PdlPersonBolkRequestVariables(personIdentList),
                query = PdlConfig.hentAdressebeskyttelseBolkQuery,
            )
        val pdlResponse =
            postForEntity<PdlBolkResponse<PdlAdressebeskyttelsePerson>>(
                pdlConfig.pdlUri,
                pdlRequest,
                httpHeaders(mapTilTema(fagsystem)),
            )
        return feilsjekkOgReturnerData(
            pdlResponse = pdlResponse,
            logContext = logContext,
        )
    }

    private fun httpHeaders(tema: Tema): HttpHeaders =
        HttpHeaders().apply {
            add("Tema", tema.name)
            add("behandlingsnummer", tema.behandlingsnummer)
        }

    private fun mapTilTema(fagsystem: Fagsystem): Tema =
        when (fagsystem) {
            Fagsystem.EF -> Tema.ENF
            Fagsystem.KONT -> Tema.KON
            Fagsystem.BA -> Tema.BAR
            else -> error("Ugyldig fagsystem=${fagsystem.navn}")
        }
}
