package no.nav.familie.tilbake.sikkerhet

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.api.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.FagsystemUtil
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.RolleConfig
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.util.UUID
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

enum class HenteParam {
    BEHANDLING_ID,
    YTELSESTYPE_OG_EKSTERN_FAGSAK_ID,
    FAGSYSTEM_OG_EKSTERN_FAGSAK_ID,
    MOTTATT_XML_ID,
    INGEN
}

@Aspect
@Configuration
class TilgangAdvice(val rolleConfig: RolleConfig,
                    val behandlingRepository: BehandlingRepository,
                    val fagsakRepository: FagsakRepository,
                    val økonomiXmlMottattRepository: ØkonomiXmlMottattRepository,
                    val integrasjonerClient: IntegrasjonerClient) {

    private val feltnavnFagsystem = "fagsystem"
    private val feltnavnYtelsestype = "ytelsestype"
    private val feltnavnBehandlingId = "behandlingId"
    private val feltnavnEksternBrukId = "eksternBrukId"
    private val feltnavnEksternFagsakId = "eksternFagsakId"

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Before("@annotation(rolletilgangssjekk) ")
    fun sjekkTilgang(joinpoint: JoinPoint, rolletilgangssjekk: Rolletilgangssjekk) {
        val minimumBehandlerRolle = rolletilgangssjekk.minimumBehandlerrolle
        val handling = rolletilgangssjekk.handling

        val httpRequest = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request

        if (HttpMethod.GET.matches(httpRequest.method) || rolletilgangssjekk.henteParam != HenteParam.INGEN) {
            validateFagsystemTilgangIGetRequest(rolletilgangssjekk.henteParam,
                                                joinpoint.args,
                                                minimumBehandlerRolle,
                                                handling)
        } else if (HttpMethod.POST.matches(httpRequest.method) || HttpMethod.PUT.matches(httpRequest.method)) {
            validateFagsystemTilgangIPostRequest(joinpoint.args[0],
                                                 minimumBehandlerRolle,
                                                 handling)
        } else {
            logger.error("${httpRequest.requestURI} støtter ikke tilgangssjekk")
        }
    }

    private fun validateFagsystemTilgangIGetRequest(param: HenteParam,
                                                    requestBody: Array<Any>,
                                                    minimumBehandlerRolle: Behandlerrolle,
                                                    handling: String) {
        when (param) {
            HenteParam.BEHANDLING_ID -> {
                val behandlingId = requestBody.first() as UUID
                val fagsak = fagsakRepository.finnFagsakForBehandlingId(behandlingId)
                var behandlerRolle = minimumBehandlerRolle
                if (requestBody.size > 1) {
                    behandlerRolle = bestemBehandlerRolleForUtførFatteVedtakSteg(requestBody[1], minimumBehandlerRolle)
                }
                validate(fagsystem = fagsak.fagsystem,
                         minimumBehandlerRolle = behandlerRolle,
                         fagsak = fagsak,
                         handling = handling)

            }
            HenteParam.YTELSESTYPE_OG_EKSTERN_FAGSAK_ID -> {
                val ytelsestype = Ytelsestype.valueOf(requestBody.first().toString())
                val fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype)
                val eksternFagsakId = requestBody[1].toString()
                val fagsak = fagsakRepository.findByFagsystemAndEksternFagsakId(fagsystem, eksternFagsakId)

                validate(fagsystem = fagsystem,
                         minimumBehandlerRolle = minimumBehandlerRolle,
                         fagsak = fagsak,
                         handling = handling)
            }
            HenteParam.FAGSYSTEM_OG_EKSTERN_FAGSAK_ID -> {
                val fagsystem = Fagsystem.valueOf(requestBody.first().toString())
                val eksternFagsakId = requestBody[1].toString()
                val fagsak = fagsakRepository.findByFagsystemAndEksternFagsakId(fagsystem, eksternFagsakId)

                validate(fagsystem = fagsystem,
                         minimumBehandlerRolle = minimumBehandlerRolle,
                         fagsak = fagsak,
                         handling = handling)
            }
            HenteParam.MOTTATT_XML_ID -> {
                val mottattXmlId = requestBody.first() as UUID
                val økonomiXmlMottatt = økonomiXmlMottattRepository.findByIdOrThrow(mottattXmlId)

                validate(fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(økonomiXmlMottatt.ytelsestype),
                         minimumBehandlerRolle = minimumBehandlerRolle,
                         fagsak = null,
                         handling = handling)
            }
            else -> {
                kastTilgangssjekkException(handling)
            }
        }
    }

    private fun validateFagsystemTilgangIPostRequest(requestBody: Any,
                                                     minimumBehandlerRolle: Behandlerrolle,
                                                     handling: String) {
        val fields: Collection<KProperty1<out Any, *>> = requestBody::class.declaredMemberProperties

        val ytelsestypeFraRequest: KProperty1<out Any, *>? = fields.find { feltnavnYtelsestype == it.name }
        val fagsystemFraRequest = fields.find { feltnavnFagsystem == it.name }
        val behandlingIdFraRequest = fields.find { feltnavnBehandlingId == it.name }
        val eksternBrukIdFraRequest = fields.find { feltnavnEksternBrukId == it.name }
        val eksternFagsakIdFraRequest = fields.find { feltnavnEksternFagsakId == it.name }

        when {
            behandlingIdFraRequest != null -> {
                val behandlingId: UUID = behandlingIdFraRequest.getter.call(requestBody) as UUID
                val fagsak = fagsakRepository.finnFagsakForBehandlingId(behandlingId)

                validate(fagsystem = fagsak.fagsystem,
                         minimumBehandlerRolle = minimumBehandlerRolle,
                         fagsak = fagsak,
                         handling = handling)
            }
            eksternBrukIdFraRequest != null -> {
                val eksternBrukId: UUID = eksternBrukIdFraRequest.getter.call(requestBody) as UUID
                val fagsak = fagsakRepository.finnFagsakForEksternBrukId(eksternBrukId)

                validate(fagsystem = fagsak.fagsystem,
                         minimumBehandlerRolle = minimumBehandlerRolle,
                         fagsak = fagsak,
                         handling = handling)
            }
            fagsystemFraRequest != null && eksternFagsakIdFraRequest != null -> {
                val fagsystem = fagsystemFraRequest.getter.call(requestBody) as Fagsystem
                val eksternFagsakId = eksternFagsakIdFraRequest.getter.call(requestBody).toString()
                val fagsak = fagsakRepository.findByFagsystemAndEksternFagsakId(fagsystem, eksternFagsakId)

                validate(fagsystem = fagsystem,
                         minimumBehandlerRolle = minimumBehandlerRolle,
                         fagsak = fagsak,
                         handling = handling)
            }
            ytelsestypeFraRequest != null && eksternFagsakIdFraRequest != null -> {
                val ytelsestype = Ytelsestype.valueOf(ytelsestypeFraRequest.getter.call(requestBody).toString())
                val fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype)
                val eksternFagsakId = eksternFagsakIdFraRequest.getter.call(requestBody).toString()
                val fagsak = fagsakRepository.findByFagsystemAndEksternFagsakId(fagsystem, eksternFagsakId)

                validate(fagsystem = fagsystem,
                         minimumBehandlerRolle = minimumBehandlerRolle,
                         fagsak = fagsak,
                         handling = handling)
            }
            ytelsestypeFraRequest != null -> {
                val ytelsestype = Ytelsestype.valueOf(ytelsestypeFraRequest.getter.call(requestBody).toString())

                validate(fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype),
                         minimumBehandlerRolle = minimumBehandlerRolle,
                         fagsak = null,
                         handling = handling)
            }
            else -> {
                kastTilgangssjekkException(handling)
            }
        }
    }

    private fun validate(fagsystem: Fagsystem,
                         minimumBehandlerRolle: Behandlerrolle,
                         fagsak: Fagsak?,
                         handling: String) {

        val brukerRolleOgFagsystemstilgang =
                ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(rolleConfig, handling)

        // når behandler har system tilgang, trenges ikke det validering på fagsystem eller rolle
        if (brukerRolleOgFagsystemstilgang.tilganger.contains(Tilgangskontrollsfagsystem.SYSTEM_TILGANG)) {
            return
        }
        // når behandler har forvaltningstilgang, blir rollen bare validert
        if (brukerRolleOgFagsystemstilgang.tilganger.contains(Tilgangskontrollsfagsystem.FORVALTER_TILGANG)) {
            validateForvaltingsrolle(brukerRolleOgFagsystemstilgang = brukerRolleOgFagsystemstilgang,
                                     minimumBehandlerRolle = minimumBehandlerRolle,
                                     handling = handling)
            return
        }
        val tilgangskontrollsfagsystem = Tilgangskontrollsfagsystem.fraFagsystem(fagsystem)
        // sjekk om saksbehandler har riktig gruppe å aksessere denne ytelsestypen
        validateFagsystem(tilgangskontrollsfagsystem, brukerRolleOgFagsystemstilgang, handling)

        // sjekk om saksbehandler har riktig rolle å aksessere denne ytelsestypen
        validateRolle(brukersrolleTilFagsystemet = brukerRolleOgFagsystemstilgang.tilganger.getValue(tilgangskontrollsfagsystem),
                      minimumBehandlerRolle = minimumBehandlerRolle,
                      handling = handling)

        validateEgenAnsattKode6Kode7(fagsak = fagsak,
                                     handling = handling)

//        logTilgangTilPerson()
    }

    private fun validateEgenAnsattKode6Kode7(fagsak: Fagsak?,
                                             handling: String) {

        val personerIBehandlingen = fagsak?.bruker?.ident?.let { listOf(it) } ?: listOf()
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
        if (minimumBehandlerRolle == Behandlerrolle.FORVALTER) {
            throw Feil(message = "${ContextService.hentSaksbehandler()} med rolle $brukersrolleTilFagsystemet " +
                                 "har ikke tilgang til å kalle forvaltningstjeneste $handling. Krever FORVALTER.",
                       frontendFeilmelding = "Du har ikke tilgang til å $handling.",
                       httpStatus = HttpStatus.FORBIDDEN)
        }
        if (minimumBehandlerRolle.nivå > brukersrolleTilFagsystemet.nivå) {
            throw Feil(message = "${ContextService.hentSaksbehandler()} med rolle $brukersrolleTilFagsystemet " +
                                 "har ikke tilgang til å $handling. Krever $minimumBehandlerRolle.",
                       frontendFeilmelding = "Du har ikke tilgang til å $handling.",
                       httpStatus = HttpStatus.FORBIDDEN)
        }
    }

    private fun validateForvaltingsrolle(brukerRolleOgFagsystemstilgang: InnloggetBrukertilgang,
                                         minimumBehandlerRolle: Behandlerrolle,
                                         handling: String) {
        val tilganger = brukerRolleOgFagsystemstilgang.tilganger
        // Forvalter kan kun kalle forvaltningstjenestene og tjenestene som kan kalles av Veileder
        if (minimumBehandlerRolle.nivå > Behandlerrolle.FORVALTER.nivå &&
            tilganger.all { it.value == Behandlerrolle.FORVALTER }) {
            throw Feil(message = "${ContextService.hentSaksbehandler()} med rolle FORVALTER " +
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
