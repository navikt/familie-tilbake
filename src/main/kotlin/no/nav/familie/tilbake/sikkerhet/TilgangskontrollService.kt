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
import no.nav.tilbakekreving.auth.Approlle
import no.nav.tilbakekreving.auth.Authentication
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kontrakter.ytelse.Tema
import no.tilbakekreving.integrasjoner.CallContext
import no.tilbakekreving.integrasjoner.feil.UnexpectedResponseException
import no.tilbakekreving.integrasjoner.persontilgang.Persontilgang
import no.tilbakekreving.integrasjoner.persontilgang.PersontilgangService
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import java.math.BigInteger
import java.util.UUID

@Configuration
class TilgangskontrollService(
    private val applicationProperties: ApplicationProperties,
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val auditLogger: AuditLogger,
    private val økonomiXmlMottattRepository: ØkonomiXmlMottattRepository,
    private val integrasjonerClient: IntegrasjonerClient,
    private val persontilgangService: PersontilgangService,
    private val tokenValidationContextHolder: TokenValidationContextHolder,
) {
    private val log = TracedLogger.getLogger<TilgangskontrollService>()

    fun validerTilgangTilbakekreving(
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
            logContext = logContext,
        )
    }

    fun validerTilgangBehandlingID(
        behandlingId: UUID,
        minimumBehandlerrolle: Behandlerrolle,
        auditLoggerEvent: AuditLoggerEvent,
        handling: String,
    ) {
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
            logContext = logContext,
        )
    }

    fun validerTilgangYtelsetypeOgFagsakId(
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

    fun validerTilgangForFagsystem(
        fagsystem: FagsystemDTO,
        eksternFagsakId: String,
        auditLoggerEvent: AuditLoggerEvent,
        handling: String,
    ) {
        val maskinToken = ContextService.hentInnloggetBruker()

        SecureLog.medContext(SecureLog.Context.utenBehandling(eksternFagsakId)) {
            info(
                "Tilgangsjekk med maskin token type={} harRolle={}",
                maskinToken::class.simpleName,
                (maskinToken as? Authentication.Systembruker)?.harRolle(Approlle.Fagsystem),
            )
        }
        return validerTilgangFagsystemOgFagsakId(fagsystem, eksternFagsakId, Behandlerrolle.VEILEDER, auditLoggerEvent, handling)
    }

    fun validerTilgangFagsystemOgFagsakId(
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
            auditLoggerEvent = auditLoggerEvent,
            logContext = logContext,
        )
    }

    fun validerTilgangMottattXMLId(
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
            logContext = SecureLog.Context.tom(),
        )
    }

    fun validerTilgangKravgrunnlagId(
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
        logContext: SecureLog.Context,
    ): Behandlerrolle {
        if (saksbehandler == Constants.BRUKER_ID_VEDTAKSLØSNINGEN) {
            // når behandler har system tilgang, trenges ikke det validering på fagsystem eller rolle
            return Behandlerrolle.SYSTEM
        }

        val brukerRolleOgFagsystemstilgang = ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(
            applicationProperties.tilgangsstyring,
            handling,
            SecureLog.Context.tom(),
        )

        validateForvaltingsrolle(
            brukerRolleOgFagsystemstilgang = brukerRolleOgFagsystemstilgang,
            minimumBehandlerrolle = minimumBehandlerrolle,
            handling = handling,
            saksbehandler = saksbehandler,
        )
        // sjekk om saksbehandler har riktig gruppe å aksessere denne ytelsestypen
        val tilgangskontrollsfagsystem = Tilgangskontrollsfagsystem.fraFagsystem(fagsystem)
        validateFagsystem(tilgangskontrollsfagsystem, brukerRolleOgFagsystemstilgang, handling, saksbehandler)

        val rolleForFagsystem = brukerRolleOgFagsystemstilgang.tilganger.getValue(tilgangskontrollsfagsystem)
        // sjekk om saksbehandler har riktig rolle å aksessere denne ytelsestypen
        validateRolle(
            brukersrolleTilFagsystemet = rolleForFagsystem,
            minimumBehandlerrolle = minimumBehandlerrolle,
            handling = handling,
            logContext = logContext,
            saksbehandler = saksbehandler,
        )

        validateEgenAnsattKode6Kode7(
            personIBehandlingen = ident,
            fagsystem = fagsystem,
            handling = handling,
            saksbehandler = saksbehandler,
            logContext = logContext,
        )

        if (ident != null) {
            logAccess(auditLoggerEvent, ident, eksternFagsakId!!)
        }
        return rolleForFagsystem
    }

    fun logAccess(
        auditLoggerEvent: AuditLoggerEvent,
        ident: String,
        eksternFagsakId: String,
        behandling: Behandling? = null,
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
        fagsystem: FagsystemDTO,
        handling: String,
        saksbehandler: String,
        logContext: SecureLog.Context,
    ) {
        if (personIBehandlingen == null) return
        val tilgangTilPerson = try {
            validerMedTilgangsmaskinen(personIBehandlingen, logContext.behandlingId, logContext.fagsystemId)
        } catch (e: UnexpectedResponseException) {
            SecureLog.medContext(logContext) {
                warn("Feilet validering med tilgangsmaskinen. Status={}, Svar={}", e.statusCode.value, e.response, e)
            }
            log.medContext(logContext) { warn("Feilet validering med tilgangsmaskinen.", e) }
            null
        } catch (e: Exception) {
            log.medContext(logContext) { warn("Feilet validering med tilgangsmaskinen.", e) }
            null
        }

        if (applicationProperties.toggles.tilgangsmaskinenEnabled) {
            if (tilgangTilPerson == null) {
                log.medContext(logContext) { info("Faller tilbake til validering med familie-integrasjoner") }
            } else if (tilgangTilPerson) {
                return
            } else {
                throw ForbiddenError(
                    message = "$saksbehandler har ikke tilgang til person i $handling",
                    frontendFeilmelding = "$saksbehandler  har ikke tilgang til person i $handling",
                    logContext = SecureLog.Context.tom(),
                )
            }
        }

        val resultatMedFamilieIntegrasjoner = validerMedFamilieIntegrasjoner(personIBehandlingen, fagsystem)
        if (resultatMedFamilieIntegrasjoner == tilgangTilPerson) {
            log.medContext(SecureLog.Context.tom()) { info("Validering av tilgang kom frem til samme utfall med familie-integrasjoner og tilgangsmaskinen") }
        } else {
            log.medContext(SecureLog.Context.tom()) { warn("Validering av tilgang var ulik med familie-integrasjoner og tilgangsmaskinen") }
        }
        if (!resultatMedFamilieIntegrasjoner) {
            throw ForbiddenError(
                message = "$saksbehandler har ikke tilgang til person i $handling",
                frontendFeilmelding = "$saksbehandler  har ikke tilgang til person i $handling",
                logContext = SecureLog.Context.tom(),
            )
        }
    }

    private fun validerMedFamilieIntegrasjoner(
        personIBehandlingen: String,
        fagsystem: FagsystemDTO,
    ): Boolean {
        val tilganger = integrasjonerClient.sjekkTilgangTilPersoner(listOf(personIBehandlingen), fagsystem.tilTema())
        return tilganger.all { it.harTilgang }
    }

    private fun validerMedTilgangsmaskinen(
        personIdent: String,
        behandlingId: String?,
        fagsystemId: String?,
    ): Boolean {
        val token = tokenValidationContextHolder.getTokenValidationContext().firstValidToken
        if (token != null) {
            val tilgang = runBlocking {
                persontilgangService.sjekkPersontilgang(
                    CallContext.Saksbehandler(
                        behandlingId,
                        fagsystemId,
                        userToken = token.encodedToken,
                    ),
                    personIdent = personIdent,
                )
            }
            if (tilgang is Persontilgang.Ok) {
                return true
            }
        }
        return false
    }

    private fun FagsystemDTO.tilTema() =
        when (this) {
            FagsystemDTO.BA -> Tema.BAR
            FagsystemDTO.KONT -> Tema.KON
            FagsystemDTO.EF -> Tema.ENF
            FagsystemDTO.TS -> Tema.TSO
            FagsystemDTO.IT01 -> throw Feil(
                message = "Fagsystem $this støttes ikke",
                logContext = SecureLog.Context.tom(),
            )
        }

    private fun validateFagsystem(
        fagsystem: Tilgangskontrollsfagsystem,
        brukerRolleOgFagsystemstilgang: InnloggetBrukertilgang,
        handling: String,
        saksbehandler: String,
    ) {
        if (!brukerRolleOgFagsystemstilgang.tilganger.contains(fagsystem)) {
            throw ForbiddenError(
                message = "$saksbehandler har ikke tilgang til $handling",
                frontendFeilmelding = "$saksbehandler  har ikke tilgang til $handling",
                logContext = SecureLog.Context.tom(),
            )
        }
    }

    private fun validateRolle(
        brukersrolleTilFagsystemet: Behandlerrolle,
        minimumBehandlerrolle: Behandlerrolle,
        handling: String,
        logContext: SecureLog.Context,
        saksbehandler: String,
    ) {
        if (minimumBehandlerrolle.nivå > brukersrolleTilFagsystemet.nivå) {
            throw ForbiddenError(
                message = "$saksbehandler med rolle $brukersrolleTilFagsystemet har ikke tilgang til å $handling. Krever $minimumBehandlerrolle.",
                frontendFeilmelding = "Du har rollen $brukersrolleTilFagsystemet og trenger rollen $minimumBehandlerrolle når du ${handling.toLowerCasePreservingASCIIRules()}.",
                logContext = logContext,
            )
        }
    }

    private fun validateForvaltingsrolle(
        brukerRolleOgFagsystemstilgang: InnloggetBrukertilgang,
        minimumBehandlerrolle: Behandlerrolle,
        handling: String,
        saksbehandler: String,
    ) {
        val tilganger = brukerRolleOgFagsystemstilgang.tilganger
        // Forvalter kan kun kalle forvaltningstjenestene og tjenestene som kan kalles av Veileder
        if (minimumBehandlerrolle == Behandlerrolle.FORVALTER && tilganger[Tilgangskontrollsfagsystem.FORVALTER_TILGANG] != Behandlerrolle.FORVALTER) {
            throw ForbiddenError(
                message = "$saksbehandler uten rolle ${Behandlerrolle.FORVALTER} har ikke tilgang til å kalle forvaltningstjeneste $handling.",
                frontendFeilmelding = "Du har rollen FORVALTER og trenger rollen $minimumBehandlerrolle når du ${handling.toLowerCasePreservingASCIIRules()}.",
                logContext = SecureLog.Context.tom(),
            )
        }
    }
}
