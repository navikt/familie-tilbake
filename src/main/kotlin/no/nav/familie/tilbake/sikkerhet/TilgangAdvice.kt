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
import org.springframework.http.HttpMethod
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.lang.reflect.Field
import java.util.UUID


@Aspect
@Configuration
class TilgangAdvice(val rolleConfig: RolleConfig,
                    val behandlingRepository: BehandlingRepository,
                    val fagsakRepository: FagsakRepository) {

    private final val behandlingIdParam = "behandlingId"
    private final val ytelsestypeParam = "ytelsestype"
    private final val eksternBrukIdParam = "eksternBrukId"

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Before("execution(* no.nav.familie.tilbake.api.*.*(..)) && @annotation(rolleTilgangssjekk) ")
    fun sjekkTilgang(joinpoint: JoinPoint, rolleTilgangssjekk: RolleTilgangssjekk) {
        val minimumBehandlerRolle = rolleTilgangssjekk.minimumBehandlerRolle
        val handling = rolleTilgangssjekk.handling

        val brukerRolleOgFagsystemstilgang = ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(rolleConfig)

        val httpRequest = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
        if (HttpMethod.POST.matches(httpRequest.method)) {
            validateFagsystemTilgangIPostRequest(joinpoint.args[0], brukerRolleOgFagsystemstilgang, handling)
        } else if (HttpMethod.GET.matches(httpRequest.method)) {
            validateFagsystemTilgangIGetRequest(rolleTilgangssjekk.henteParam,
                                                joinpoint.args[0],
                                                brukerRolleOgFagsystemstilgang,
                                                handling)
        }

        val høyesteRolletilgang = brukerRolleOgFagsystemstilgang.behandlerRolle

        if (minimumBehandlerRolle.nivå > høyesteRolletilgang.nivå) {
            throw Feil(
                    message = "${ContextService.hentSaksbehandler()} med rolle $høyesteRolletilgang " +
                              "har ikke tilgang til å $handling. Krever $minimumBehandlerRolle.",
                    frontendFeilmelding = "Du har ikke tilgang til å $handling."
            )
        }
    }

    private fun validateFagsystemTilgangIGetRequest(param: String,
                                                    requestBody: Any,
                                                    brukerRolleOgFagsystemstilgang: InnloggetBrukerTilgang,
                                                    handling: String) {
        when {
            behandlingIdParam == param -> {
                val behandlingId = requestBody as UUID
                val fagsystem = hentFagsystemAvBehandlingId(behandlingId)

                // sjekk om saksbehandler har riktig gruppe å aksessere denne ytelsestypen
                validateFagsystem(fagsystem, brukerRolleOgFagsystemstilgang, handling)

            }
            ytelsestypeParam == param -> {
                val ytelsestype = Ytelsestype.valueOf(requestBody.toString())
                val fagsystem = Fagsystem.fraYtelsestype(ytelsestype)

                // sjekk om saksbehandler har riktig gruppe å aksessere denne ytelsestypen
                validateFagsystem(fagsystem, brukerRolleOgFagsystemstilgang, handling)
            }
            eksternBrukIdParam == param -> {
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
                                                     brukerRolleOgFagsystemstilgang: InnloggetBrukerTilgang,
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
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        return fagsakRepository.findByIdOrThrow(behandling.fagsakId).fagsystem
    }

    private fun hentFagsystemAvEksternBrukId(eksternBrukId: UUID): Fagsystem {
        val behandling = behandlingRepository.findByEksternBrukId(eksternBrukId)
        return fagsakRepository.findByIdOrThrow(behandling.fagsakId).fagsystem
    }

    private fun validateFagsystem(fagsystem: Fagsystem,
                                  brukerRolleOgFagsystemstilgang: InnloggetBrukerTilgang,
                                  handling: String) {
        if (rolleConfig.ENVIRONMENT_NAME == "local" || BehandlerRolle.SYSTEM == brukerRolleOgFagsystemstilgang.behandlerRolle) {
            return
        }
        if (fagsystem != brukerRolleOgFagsystemstilgang.fagsystem) {
            throw Feil(message = "${ContextService.hentSaksbehandler()} med ${brukerRolleOgFagsystemstilgang.fagsystem} tilgang" +
                                 " har ikke tilgang til $handling",
                       frontendFeilmelding = "${ContextService.hentSaksbehandler()} med " +
                                             "${brukerRolleOgFagsystemstilgang.fagsystem}" +
                                             " tilgang har ikke tilgang til $handling")
        }
    }

}
