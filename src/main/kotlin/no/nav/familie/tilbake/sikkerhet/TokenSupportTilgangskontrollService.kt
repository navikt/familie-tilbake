package no.nav.familie.tilbake.sikkerhet

import io.ktor.util.toLowerCasePreservingASCIIRules
import kotlinx.coroutines.runBlocking
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.exceptionhandler.ForbiddenError
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.tilbakekreving.FagsystemUtil
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.auth.Authentication
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.tilbakekreving.integrasjoner.CallContext
import no.tilbakekreving.integrasjoner.feil.UnexpectedResponseException
import no.tilbakekreving.integrasjoner.persontilgang.Persontilgang
import no.tilbakekreving.integrasjoner.persontilgang.PersontilgangService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.util.UUID

@Service
class TokenSupportTilgangskontrollService(
    private val applicationProperties: ApplicationProperties,
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val auditLogger: AuditLogger,
    private val økonomiXmlMottattRepository: ØkonomiXmlMottattRepository,
    private val integrasjonerClient: IntegrasjonerClient,
    private val persontilgangService: PersontilgangService,
    private val tokenValidationContextHolder: TokenValidationContextHolder,
) : TilgangskontrollService {
    private val log = TracedLogger.getLogger<TilgangskontrollService>()

    override fun validerTilgangTilbakekreving(
        tilbakekreving: Tilbakekreving,
        behandlingId: UUID?,
        minimumBehandlerrolle: Behandlerrolle,
        auditLoggerEvent: AuditLoggerEvent,
        handling: String,
    ): Behandlerrolle {
        val saksbehandler = ContextService.hentSaksbehandler(SecureLog.Context.tom())
        val fagsystem = tilbakekreving.tilFrontendDto().fagsystem
        val logContext = SecureLog.Context.medBehandling(tilbakekreving.eksternFagsak.eksternId, behandlingId?.toString())
        val dto = tilbakekreving.tilFrontendDto()
        return validate(
            fagsystem = fagsystem,
            minimumBehandlerrolle = minimumBehandlerrolle,
            ident = dto.bruker.personIdent,
            eksternFagsakId = dto.eksternFagsakId,
            handling = handling,
            saksbehandler = saksbehandler,
            auditLoggerEvent = auditLoggerEvent,
            authentication = ContextService.hentInnloggetBruker(),
            logContext = logContext,
        )
    }

    override fun validerTilgangBehandlingID(behandlingId: UUID, minimumBehandlerrolle: Behandlerrolle, auditLoggerEvent: AuditLoggerEvent, handling: String) {
        val saksbehandler = ContextService.hentSaksbehandler(SecureLog.Context.tom())
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, behandling.id.toString())

        validate(
            fagsystem = fagsak.fagsystem.tilDTO(),
            minimumBehandlerrolle = minimumBehandlerrolle,
            ident = fagsak.bruker.ident,
            eksternFagsakId = fagsak.eksternFagsakId,
            handling = handling,
            saksbehandler = saksbehandler,
            auditLoggerEvent = auditLoggerEvent,
            authentication = ContextService.hentInnloggetBruker(),
            logContext = logContext,
        )
    }

    override fun validerTilgangYtelsetypeOgFagsakId(
        ytelsestype: Ytelsestype,
        eksternFagsakId: String,
        minimumBehandlerrolle: Behandlerrolle,
        auditLoggerEvent: AuditLoggerEvent,
        handling: String,
    ) {
        validerTilgangFagsystemOgFagsakId(
            fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype.tilDTO()),
            eksternFagsakId = eksternFagsakId,
            minimumBehandlerrolle = minimumBehandlerrolle,
            auditLoggerEvent = auditLoggerEvent,
            handling = handling,
        )
    }

    override fun validerTilgangFagsystemOgFagsakId(
        fagsystem: FagsystemDTO,
        eksternFagsakId: String,
        minimumBehandlerrolle: Behandlerrolle,
        auditLoggerEvent: AuditLoggerEvent,
        handling: String,
    ) {
        val saksbehandler = ContextService.hentSaksbehandler(SecureLog.Context.tom())
        val fagsak = fagsakRepository.findByFagsystemAndEksternFagsakId(Fagsystem.forDTO(fagsystem), eksternFagsakId)
        val logContext = SecureLog.Context.utenBehandling(fagsak?.eksternFagsakId)
        validate(
            fagsystem = fagsystem,
            minimumBehandlerrolle = minimumBehandlerrolle,
            ident = fagsak?.bruker?.ident,
            eksternFagsakId = fagsak?.eksternFagsakId,
            handling = handling,
            saksbehandler = saksbehandler,
            authentication = ContextService.hentInnloggetBruker(),
            auditLoggerEvent = auditLoggerEvent,
            logContext = logContext,
        )
    }

    override fun validerTilgangMottattXMLId(
        mottattXmlId: UUID,
        minimumBehandlerrolle: Behandlerrolle,
        auditLoggerEvent: AuditLoggerEvent,
        handling: String,
    ) {
        val saksbehandler = ContextService.hentSaksbehandler(SecureLog.Context.tom())
        val økonomiXmlMottatt = økonomiXmlMottattRepository.findByIdOrThrow(mottattXmlId)

        validate(
            fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(økonomiXmlMottatt.ytelsestype.tilDTO()),
            minimumBehandlerrolle = minimumBehandlerrolle,
            ident = null,
            eksternFagsakId = null,
            handling = handling,
            saksbehandler = saksbehandler,
            auditLoggerEvent = auditLoggerEvent,
            authentication = ContextService.hentInnloggetBruker(),
            logContext = SecureLog.Context.tom(),
        )
    }

    override fun validerTilgangKravgrunnlagId(
        eksternKravgrunnlagId: BigInteger,
        minimumBehandlerrolle: Behandlerrolle,
        auditLoggerEvent: AuditLoggerEvent,
        handling: String,
    ) {
        val saksbehandler = ContextService.hentSaksbehandler(SecureLog.Context.tom())
        val økonomiXmlMottatt = økonomiXmlMottattRepository.findByEksternKravgrunnlagId(eksternKravgrunnlagId)
        val kravgrunnlag = kravgrunnlagRepository.findByEksternKravgrunnlagIdAndAktivIsTrue(eksternKravgrunnlagId)
        if (økonomiXmlMottatt == null && kravgrunnlag == null) {
            throw Feil(
                message = "Finnes ikke eksternKravgrunnlagId=$eksternKravgrunnlagId",
                httpStatus = HttpStatus.BAD_REQUEST,
                logContext = SecureLog.Context.tom(),
            )
        }
        val ytelsestype =
            økonomiXmlMottatt?.ytelsestype
                ?: kravgrunnlag?.fagområdekode?.ytelsestype
                ?: throw Feil(
                    message = "Ukjent ytelsestype for kravgrunnlag",
                    httpStatus = HttpStatus.BAD_REQUEST,
                    logContext = SecureLog.Context.utenBehandling(kravgrunnlag?.fagsystemId),
                )

        validate(
            fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype.tilDTO()),
            minimumBehandlerrolle = minimumBehandlerrolle,
            ident = null,
            eksternFagsakId = null,
            handling = handling,
            saksbehandler = saksbehandler,
            auditLoggerEvent = auditLoggerEvent,
            authentication = ContextService.hentInnloggetBruker(),
            logContext = SecureLog.Context.tom(),
        )
    }

    private fun validate(
        fagsystem: FagsystemDTO,
        minimumBehandlerrolle: Behandlerrolle,
        ident: String?,
        eksternFagsakId: String?,
        handling: String,
        saksbehandler: String,
        auditLoggerEvent: AuditLoggerEvent,
        authentication: Authentication,
        logContext: SecureLog.Context,
    ): Behandlerrolle {
        if (saksbehandler == Constants.BRUKER_ID_VEDTAKSLØSNINGEN) {
            // når behandler har system tilgang, trenges ikke det validering på fagsystem eller rolle
            return Behandlerrolle.SYSTEM
        }

        val brukerRolleOgFagsystemstilgang = ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(
            applicationProperties.tilgangsstyring,
            handling,
            logContext,
        )

        if (!validateForvaltingsrolle(brukerRolleOgFagsystemstilgang = brukerRolleOgFagsystemstilgang, minimumBehandlerrolle = minimumBehandlerrolle)) {
            throw ForbiddenError(
                message = "$saksbehandler uten rolle ${Behandlerrolle.FORVALTER} har ikke tilgang til å kalle forvaltningstjeneste $handling.",
                frontendFeilmelding = "Du har rollen FORVALTER og trenger rollen $minimumBehandlerrolle når du ${handling.toLowerCasePreservingASCIIRules()}.",
                logContext = logContext,
            )
        }
        // sjekk om saksbehandler har riktig gruppe å aksessere denne ytelsestypen
        val tilgangskontrollsfagsystem = Tilgangskontrollsfagsystem.fraFagsystem(fagsystem)
        if (!validateFagsystem(tilgangskontrollsfagsystem, brukerRolleOgFagsystemstilgang)) {
            throw ForbiddenError(
                message = "$saksbehandler har ikke tilgang til $handling",
                frontendFeilmelding = "$saksbehandler  har ikke tilgang til $handling",
                logContext = logContext,
            )
        }

        val rolleForFagsystem = brukerRolleOgFagsystemstilgang.tilganger.getValue(tilgangskontrollsfagsystem)
        // sjekk om saksbehandler har riktig rolle å aksessere denne ytelsestypen
        if (!validateRolle(brukersrolleTilFagsystemet = rolleForFagsystem, minimumBehandlerrolle = minimumBehandlerrolle)) {
            throw ForbiddenError(
                message = "$saksbehandler med rolle $rolleForFagsystem har ikke tilgang til å $handling. Krever $minimumBehandlerrolle.",
                frontendFeilmelding = "Du har rollen $rolleForFagsystem og trenger rollen $minimumBehandlerrolle når du ${handling.toLowerCasePreservingASCIIRules()}.",
                logContext = logContext,
            )
        }

        if (!validateEgenAnsattKode6Kode7(personIBehandlingen = ident, authentication = authentication, logContext = logContext)) {
            throw ForbiddenError(
                message = "$saksbehandler har ikke tilgang til person i $handling",
                frontendFeilmelding = "$saksbehandler  har ikke tilgang til person i $handling",
                logContext = logContext,
            )
        }

        if (ident != null) {
            logAccess(auditLoggerEvent, ident, eksternFagsakId!!)
        }
        return rolleForFagsystem
    }

    override fun logAccess(
        auditLoggerEvent: AuditLoggerEvent,
        ident: String,
        eksternFagsakId: String,
        behandling: Behandling?,
    ) {
        auditLogger.log(
            Sporingsdata(
                auditLoggerEvent,
                ident,
                CustomKeyValue("eksternFagsakId", eksternFagsakId),
                behandling?.let {
                    CustomKeyValue("behandlingEksternBrukId", behandling.eksternBrukId.toString())
                },
            ),
        )
    }

    private fun validateEgenAnsattKode6Kode7(
        personIBehandlingen: String?,
        authentication: Authentication,
        logContext: SecureLog.Context,
    ): Boolean {
        if (personIBehandlingen == null) return true
        if (authentication is Authentication.Systembruker) return true

        try {
            val token = tokenValidationContextHolder.getTokenValidationContext().firstValidToken ?: return false
            val tilgang = runBlocking {
                persontilgangService.sjekkPersontilgang(
                    CallContext.Saksbehandler(
                        logContext.behandlingId,
                        logContext.fagsystemId,
                        userToken = token.encodedToken,
                    ),
                    personIdent = personIBehandlingen,
                )
            }
            if (tilgang is Persontilgang.Ok) {
                return true
            }
        } catch (e: UnexpectedResponseException) {
            SecureLog.medContext(logContext) {
                warn("Feilet validering med tilgangsmaskinen. Status={}, Svar={}", e.statusCode.value, e.response, e)
            }
            log.medContext(logContext) { warn("Feilet validering med tilgangsmaskinen.", e) }
        } catch (e: Exception) {
            log.medContext(logContext) { warn("Feilet validering med tilgangsmaskinen.", e) }
        }

        return false
    }

    private fun validateFagsystem(
        fagsystem: Tilgangskontrollsfagsystem,
        brukerRolleOgFagsystemstilgang: InnloggetBrukertilgang,
    ): Boolean {
        return brukerRolleOgFagsystemstilgang.tilganger.contains(fagsystem)
    }

    private fun validateRolle(
        brukersrolleTilFagsystemet: Behandlerrolle,
        minimumBehandlerrolle: Behandlerrolle,
    ): Boolean {
        return brukersrolleTilFagsystemet.nivå >= minimumBehandlerrolle.nivå
    }

    private fun validateForvaltingsrolle(
        brukerRolleOgFagsystemstilgang: InnloggetBrukertilgang,
        minimumBehandlerrolle: Behandlerrolle,
    ): Boolean {
        val tilganger = brukerRolleOgFagsystemstilgang.tilganger
        // Forvalter kan kun kalle forvaltningstjenestene og tjenestene som kan kalles av Veileder
        return minimumBehandlerrolle != Behandlerrolle.FORVALTER || tilganger[Tilgangskontrollsfagsystem.FORVALTER_TILGANG] == Behandlerrolle.FORVALTER
    }
}
