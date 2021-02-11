package no.nav.familie.tilbake.sikkerhet

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Fagsystem
import no.nav.familie.tilbake.behandling.domain.Ytelsestype
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.RolleConfig
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
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
                    val environment: Environment) {

    private final val behandlingIdParam = "behandlingId"
    private final val ytelsestypeParam = "ytelsestype"
    private final val eksternBrukIdParam = "eksternBrukId"

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Before("execution(* no.nav.familie.tilbake.api.*.*(..)) && @annotation(rolletilgangssjekk) ")
    fun sjekkTilgang(joinpoint: JoinPoint, rolletilgangssjekk: Rolletilgangssjekk) {
        val minimumBehandlerRolle = rolletilgangssjekk.minimumBehandlerrolle
        val handling = rolletilgangssjekk.handling

        val brukerRolleOgFagsystemstilgang =
                ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(rolleConfig, environment)

        val httpRequest = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
        if (HttpMethod.POST.matches(httpRequest.method)) {
            validateFagsystemTilgangIPostRequest(joinpoint.args[0], brukerRolleOgFagsystemstilgang, handling)
        } else if (HttpMethod.GET.matches(httpRequest.method)) {
            validateFagsystemTilgangIGetRequest(rolletilgangssjekk.henteParam,
                                                joinpoint.args[0],
                                                brukerRolleOgFagsystemstilgang,
                                                handling)
        }

        val høyesteRolletilgang = brukerRolleOgFagsystemstilgang.rolle

        if (minimumBehandlerRolle.nivå > høyesteRolletilgang.nivå) {
            throw Feil(
                    message = "${ContextService.hentSaksbehandler()} med rolle $høyesteRolletilgang " +
                              "har ikke tilgang til å $handling. Krever $minimumBehandlerRolle.",
                    frontendFeilmelding = "Du har ikke tilgang til å $handling.",
                    httpStatus = HttpStatus.FORBIDDEN
            )
        }
    }

    private fun validateFagsystemTilgangIGetRequest(param: String,
                                                    requestBody: Any,
                                                    brukerRolleOgFagsystemstilgang: InnloggetBrukertilgang,
                                                    handling: String) {
        when (param) {
            behandlingIdParam -> {
                val behandlingId = requestBody as UUID
                val fagsystem = hentFagsystemAvBehandlingId(behandlingId)

                // sjekk om saksbehandler har riktig gruppe å aksessere denne ytelsestypen
                validateFagsystem(fagsystem, brukerRolleOgFagsystemstilgang, handling)

            }
            ytelsestypeParam -> {
                val ytelsestype = Ytelsestype.valueOf(requestBody.toString())
                val fagsystem = Fagsystem.fraYtelsestype(ytelsestype)

                // sjekk om saksbehandler har riktig gruppe å aksessere denne ytelsestypen
                validateFagsystem(fagsystem, brukerRolleOgFagsystemstilgang, handling)
            }
            eksternBrukIdParam -> {
                val eksternBrukId = requestBody as UUID
                val fagsystem = hentFagsystemAvEksternBrukId(eksternBrukId)

                // sjekk om saksbehandler har riktig gruppe å aksessere denne ytelsestypen
                validateFagsystem(fagsystem, brukerRolleOgFagsystemstilgang, handling)
            }
            else -> {
                logger.info("$handling kan ikke valideres for tilgangssjekk. " +
                            "Det finnes ikke en av de påkrevde parameterne i request")
            }
        }
    }

    private fun validateFagsystemTilgangIPostRequest(requestBody: Any,
                                                     brukerRolleOgFagsystemstilgang: InnloggetBrukertilgang,
                                                     handling: String) {
        val ytelsestypeFinnesIRequest = requestBody.javaClass.declaredFields.any { "ytelsestype" == it.name }
        val behandlingIdFinnesIRequest = requestBody.javaClass.declaredFields.any { "behandlingId" == it.name }
        val eksternBrukIdFinnesIRequest = requestBody.javaClass.declaredFields.any { "eksternBrukId" == it.name }

        when {
            ytelsestypeFinnesIRequest -> {
                val felt = hentFelt(feltNavn = ytelsestypeParam, requestBody = requestBody)
                val ytelsestype = Ytelsestype.valueOf(felt.get(requestBody).toString())
                val fagsystem = Fagsystem.fraYtelsestype(ytelsestype)

                // sjekk om saksbehandler har riktig gruppe å aksessere denne ytelsestypen
                validateFagsystem(fagsystem, brukerRolleOgFagsystemstilgang, handling)
            }
            behandlingIdFinnesIRequest -> {
                val felt = hentFelt(feltNavn = behandlingIdParam, requestBody = requestBody)
                val behandlingId: UUID = felt.get(requestBody) as UUID
                val fagsystem = hentFagsystemAvBehandlingId(behandlingId)

                // sjekk om saksbehandler har riktig gruppe å aksessere denne ytelsestypen
                validateFagsystem(fagsystem, brukerRolleOgFagsystemstilgang, handling)
            }
            eksternBrukIdFinnesIRequest -> {
                val felt = hentFelt(feltNavn = eksternBrukIdParam, requestBody = requestBody)
                val eksternBrukId: UUID = felt.get(requestBody) as UUID
                val fagsystem = hentFagsystemAvEksternBrukId(eksternBrukId)

                // sjekk om saksbehandler har riktig gruppe å aksessere denne ytelsestypen
                validateFagsystem(fagsystem, brukerRolleOgFagsystemstilgang, handling)
            }
            else -> {
                logger.info("$handling kan ikke valideres for tilgangssjekk. " +
                            "Det finnes ikke en av de påkrevde parameterne i request")
            }
        }
    }


    private fun hentFelt(feltNavn: String, requestBody: Any): Field {
        val felt = requestBody.javaClass.declaredFields.filter { it.name == feltNavn }[0]
        felt.isAccessible = true
        return felt
    }

    private fun hentFagsystemAvBehandlingId(behandlingId: UUID): Fagsystem {
        val behandling = behandlingRepository.findByIdOrNull(behandlingId)
                         ?: throw Feil(message = "Behandling finnes ikke for $behandlingId", httpStatus = HttpStatus.BAD_REQUEST)
        return fagsakRepository.findByIdOrThrow(behandling.fagsakId).fagsystem
    }

    private fun hentFagsystemAvEksternBrukId(eksternBrukId: UUID): Fagsystem {
        val behandling = behandlingRepository.findByEksternBrukId(eksternBrukId)
        return fagsakRepository.findByIdOrThrow(behandling.fagsakId).fagsystem
    }

    private fun validateFagsystem(fagsystem: Fagsystem,
                                  brukerRolleOgFagsystemstilgang: InnloggetBrukertilgang,
                                  handling: String) {
        if (environment.activeProfiles.any { "local" == it } ||
            Behandlerrolle.SYSTEM == brukerRolleOgFagsystemstilgang.rolle) {
            return
        }
        if (brukerRolleOgFagsystemstilgang.tilganger.none { fagsystem == it }) {
            throw Feil(message = "${ContextService.hentSaksbehandler()} har ikke tilgang til $handling",
                       frontendFeilmelding = "${ContextService.hentSaksbehandler()}  har ikke tilgang til $handling",
                       httpStatus = HttpStatus.FORBIDDEN)
        }
    }

}
