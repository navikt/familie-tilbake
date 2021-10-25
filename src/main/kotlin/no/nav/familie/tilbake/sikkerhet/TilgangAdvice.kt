package no.nav.familie.tilbake.sikkerhet

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.api.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.RolleConfig
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.lang.reflect.Field
import java.util.UUID


@Aspect
@Configuration
class TilgangAdvice(val rolleConfig: RolleConfig,
                    val behandlingRepository: BehandlingRepository,
                    val fagsakRepository: FagsakRepository,
                    val integrasjonerClient: IntegrasjonerClient) {

    private val behandlingIdParam = "behandlingId"
    private val ytelsestypeParam = "ytelsestype"
    private val fagsystemParam = "fagsystem"
    private val eksternBrukIdParam = "eksternBrukId"

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Before("@annotation(rolletilgangssjekk) ")
    fun sjekkTilgang(joinpoint: JoinPoint, rolletilgangssjekk: Rolletilgangssjekk) {
        val minimumBehandlerRolle = rolletilgangssjekk.minimumBehandlerrolle
        val handling = rolletilgangssjekk.handling

        val brukerRolleOgFagsystemstilgang =
                ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(rolleConfig, handling)

        val httpRequest = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request

        if (HttpMethod.GET.matches(httpRequest.method) || rolletilgangssjekk.henteParam != "") {
            validateFagsystemTilgangIGetRequest(rolletilgangssjekk.henteParam,
                                                joinpoint.args,
                                                brukerRolleOgFagsystemstilgang,
                                                minimumBehandlerRolle,
                                                handling)
        } else if (HttpMethod.POST.matches(httpRequest.method) || HttpMethod.PUT.matches(httpRequest.method)) {
            validateFagsystemTilgangIPostRequest(joinpoint.args[0],
                                                 brukerRolleOgFagsystemstilgang,
                                                 minimumBehandlerRolle,
                                                 handling)
        } else {
            logger.warn("${httpRequest.requestURI} støtter ikke tilgangssjekk")
        }

    }

    private fun validateFagsystemTilgangIGetRequest(param: String,
                                                    requestBody: Array<Any>,
                                                    brukerRolleOgFagsystemstilgang: InnloggetBrukertilgang,
                                                    minimumBehandlerRolle: Behandlerrolle,
                                                    handling: String) {
        when (param) {
            behandlingIdParam -> {
                val behandlingId = requestBody.first() as UUID
                val fagsystem = hentFagsystemAvBehandlingId(behandlingId)
                val personerIBehandlingen = hentPersonerIBehandlingen(behandlingId)
                var behandlerRolle = minimumBehandlerRolle
                if (requestBody.size > 1) {
                    behandlerRolle = bestemBehandlerRolleForUtførFatteVedtakSteg(requestBody[1], minimumBehandlerRolle)
                }
                validate(fagsystem = fagsystem, brukerRolleOgFagsystemstilgang = brukerRolleOgFagsystemstilgang,
                         minimumBehandlerRolle = behandlerRolle,
                         personerIBehandlingen = personerIBehandlingen,
                         handling = handling)

            }
            ytelsestypeParam -> {
                val ytelsestype = Ytelsestype.valueOf(requestBody.first().toString())
                val fagsystem = Tilgangskontrollsfagsystem.fraYtelsestype(ytelsestype)

                validate(fagsystem = fagsystem, brukerRolleOgFagsystemstilgang = brukerRolleOgFagsystemstilgang,
                         minimumBehandlerRolle = minimumBehandlerRolle, personerIBehandlingen = emptyList(), handling = handling)
            }
            fagsystemParam -> {
                val fagsystem = Tilgangskontrollsfagsystem.fraKode(requestBody.first().toString())

                validate(fagsystem = fagsystem, brukerRolleOgFagsystemstilgang = brukerRolleOgFagsystemstilgang,
                         minimumBehandlerRolle = minimumBehandlerRolle, personerIBehandlingen = emptyList(), handling = handling)
            }
            eksternBrukIdParam -> {
                val eksternBrukId = requestBody.first() as UUID
                val fagsystem = hentFagsystemAvEksternBrukId(eksternBrukId)
                val personerIBehandlingen = hentPersonerIBehandlingenBasertPåEksternBrukId(eksternBrukId)

                validate(fagsystem = fagsystem, brukerRolleOgFagsystemstilgang = brukerRolleOgFagsystemstilgang,
                         minimumBehandlerRolle = minimumBehandlerRolle, personerIBehandlingen = personerIBehandlingen,
                         handling = handling)
            }
            else -> {
                kastTilgangssjekkException(handling)
            }
        }
    }

    private fun validateFagsystemTilgangIPostRequest(requestBody: Any,
                                                     brukerRolleOgFagsystemstilgang: InnloggetBrukertilgang,
                                                     minimumBehandlerRolle: Behandlerrolle,
                                                     handling: String) {
        val ytelsestypeFinnesIRequest = requestBody.javaClass.declaredFields.any { "ytelsestype" == it.name }
        val behandlingIdFinnesIRequest = requestBody.javaClass.declaredFields.any { "behandlingId" == it.name }
        val eksternBrukIdFinnesIRequest = requestBody.javaClass.declaredFields.any { "eksternBrukId" == it.name }

        when {
            ytelsestypeFinnesIRequest -> {
                val felt = hentFelt(feltNavn = ytelsestypeParam, requestBody = requestBody)
                val ytelsestype = Ytelsestype.valueOf(felt.get(requestBody).toString())
                val fagsystem = Tilgangskontrollsfagsystem.fraYtelsestype(ytelsestype)

                validate(fagsystem = fagsystem, brukerRolleOgFagsystemstilgang = brukerRolleOgFagsystemstilgang,
                         minimumBehandlerRolle = minimumBehandlerRolle, personerIBehandlingen = emptyList(),
                         handling = handling)
            }
            behandlingIdFinnesIRequest -> {
                val felt = hentFelt(feltNavn = behandlingIdParam, requestBody = requestBody)
                val behandlingId: UUID = felt.get(requestBody) as UUID
                val fagsystem = hentFagsystemAvBehandlingId(behandlingId)
                val personerIBehandlingen = hentPersonerIBehandlingen(behandlingId)

                validate(fagsystem = fagsystem, brukerRolleOgFagsystemstilgang = brukerRolleOgFagsystemstilgang,
                         minimumBehandlerRolle = minimumBehandlerRolle,
                         personerIBehandlingen = personerIBehandlingen,
                         handling = handling)
            }
            eksternBrukIdFinnesIRequest -> {
                val felt = hentFelt(feltNavn = eksternBrukIdParam, requestBody = requestBody)
                val eksternBrukId: UUID = felt.get(requestBody) as UUID
                val fagsystem = hentFagsystemAvEksternBrukId(eksternBrukId)
                val personerIBehandlingen = hentPersonerIBehandlingenBasertPåEksternBrukId(eksternBrukId)

                validate(fagsystem = fagsystem, brukerRolleOgFagsystemstilgang = brukerRolleOgFagsystemstilgang,
                         minimumBehandlerRolle = minimumBehandlerRolle,
                         personerIBehandlingen = personerIBehandlingen,
                         handling = handling)
            }
            else -> {
                kastTilgangssjekkException(handling)
            }
        }
    }

    private fun hentFelt(feltNavn: String, requestBody: Any): Field {
        val felt = requestBody.javaClass.declaredFields.filter { it.name == feltNavn }[0]
        felt.isAccessible = true
        return felt
    }

    private fun hentFagsystemAvBehandlingId(behandlingId: UUID): Tilgangskontrollsfagsystem {
        val behandling = behandlingRepository.findByIdOrNull(behandlingId)
                         ?: throw Feil(message = "Behandling finnes ikke for $behandlingId", httpStatus = HttpStatus.BAD_REQUEST)
        return Tilgangskontrollsfagsystem.fraFagsystem(fagsakRepository.findByIdOrThrow(behandling.fagsakId).fagsystem)
    }

    private fun hentPersonerIBehandlingen(behandlingId: UUID): List<String> {
        val behandling = behandlingRepository.findByIdOrNull(behandlingId)
                         ?: throw Feil(message = "Behandling finnes ikke for $behandlingId", httpStatus = HttpStatus.BAD_REQUEST)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        return listOf(fagsak.bruker.ident)
    }

    private fun hentPersonerIBehandlingenBasertPåEksternBrukId(eksternBrukId: UUID): List<String> {
        val behandling = behandlingRepository.findByEksternBrukId(eksternBrukId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        return listOf(fagsak.bruker.ident)
    }

    private fun hentFagsystemAvEksternBrukId(eksternBrukId: UUID): Tilgangskontrollsfagsystem {
        val behandling = behandlingRepository.findByEksternBrukId(eksternBrukId)
        return Tilgangskontrollsfagsystem.fraFagsystem(fagsakRepository.findByIdOrThrow(behandling.fagsakId).fagsystem)
    }

    private fun validate(fagsystem: Tilgangskontrollsfagsystem,
                         brukerRolleOgFagsystemstilgang: InnloggetBrukertilgang,
                         minimumBehandlerRolle: Behandlerrolle,
                         personerIBehandlingen: List<String>,
                         handling: String) {
        // når behandler har system tilgang, trenges ikke det validering på fagsystem eller rolle
        if (brukerRolleOgFagsystemstilgang.tilganger.contains(Tilgangskontrollsfagsystem.SYSTEM_TILGANG)) {
            return
        }
        // når behandler har forvaltningstilgang, blir rollen bare validert
        if (brukerRolleOgFagsystemstilgang.tilganger.contains(Tilgangskontrollsfagsystem.FORVALTER_TILGANG)) {
            validateRolle(brukersrolleTilFagsystemet = Behandlerrolle.FORVALTER,
                          minimumBehandlerRolle = minimumBehandlerRolle,
                          handling = handling)
            return
        }
        // sjekk om saksbehandler har riktig gruppe å aksessere denne ytelsestypen
        validateFagsystem(fagsystem, brukerRolleOgFagsystemstilgang, handling)

        // sjekk om saksbehandler har riktig rolle å aksessere denne ytelsestypen
        validateRolle(brukersrolleTilFagsystemet = brukerRolleOgFagsystemstilgang.tilganger.getValue(fagsystem),
                      minimumBehandlerRolle = minimumBehandlerRolle,
                      handling = handling)

        validateEgenAnsattKode6Kode7(personerIBehandlingen = personerIBehandlingen,
                                     handling = handling)
    }

    private fun validateEgenAnsattKode6Kode7(personerIBehandlingen: List<String>,
                                             handling: String) {

        val tilganger = integrasjonerClient.sjekkTilgangTilPersoner(personerIBehandlingen)
        if (tilganger.any { !it.harTilgang }) {
            throw Feil(message = "${ContextService.hentSaksbehandler()} har ikke tilgang til person i $handling",
                       frontendFeilmelding = "${ContextService.hentSaksbehandler()}  har ikke tilgang til person i $handling",
                       httpStatus = HttpStatus.FORBIDDEN)
        }
    }

    private fun validateFagsystem(fagsystem: Tilgangskontrollsfagsystem,
                                  brukerRolleOgFagsystemstilgang: InnloggetBrukertilgang,
                                  handling: String) {
        if (!brukerRolleOgFagsystemstilgang.tilganger.contains(fagsystem)) {
            throw Feil(message = "${ContextService.hentSaksbehandler()} har ikke tilgang til $handling",
                       frontendFeilmelding = "${ContextService.hentSaksbehandler()}  har ikke tilgang til $handling",
                       httpStatus = HttpStatus.FORBIDDEN)
        }
    }

    private fun validateRolle(brukersrolleTilFagsystemet: Behandlerrolle,
                              minimumBehandlerRolle: Behandlerrolle,
                              handling: String) {

        if (minimumBehandlerRolle.nivå > brukersrolleTilFagsystemet.nivå) {
            throw Feil(message = "${ContextService.hentSaksbehandler()} med rolle $brukersrolleTilFagsystemet " +
                                 "har ikke tilgang til å $handling. Krever $minimumBehandlerRolle.",
                       frontendFeilmelding = "Du har ikke tilgang til å $handling.",
                       httpStatus = HttpStatus.FORBIDDEN)
        }
    }

    private fun kastTilgangssjekkException(handling: String) {
        val feilmelding: String = "$handling kan ikke valideres for tilgangssjekk. " +
                                  "Det finnes ikke en av de påkrevde parameterne i request"
        throw Feil(message = feilmelding,
                   frontendFeilmelding = feilmelding,
                   httpStatus = HttpStatus.BAD_REQUEST)
    }

    private fun bestemBehandlerRolleForUtførFatteVedtakSteg(requestBody: Any,
                                                            minimumBehandlerRolle: Behandlerrolle): Behandlerrolle {
        // Behandlerrolle blir endret til Beslutter kun når FatteVedtak steg utføres
        if (requestBody is BehandlingsstegFatteVedtaksstegDto) {
            return Behandlerrolle.BESLUTTER
        }
        return minimumBehandlerRolle
    }

}
