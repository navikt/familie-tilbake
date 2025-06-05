package no.nav.familie.tilbake.sikkerhet

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.config.RolleConfig
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.FagsystemUtil
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.kontrakter.ytelse.Tema
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import java.math.BigInteger
import java.util.UUID

@Configuration
class TilgangskontrollService(
    private val rolleConfig: RolleConfig,
    private val fagsakRepository: FagsakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val auditLogger: AuditLogger,
    private val økonomiXmlMottattRepository: ØkonomiXmlMottattRepository,
    private val integrasjonerClient: IntegrasjonerClient,
) {
    fun validerTilgangTilbakekreving(
        tilbakekreving: Tilbakekreving,
        behandlingId: UUID?,
        minimumBehandlerrolle: Behandlerrolle,
        auditLoggerEvent: AuditLoggerEvent,
        handling: String,
    ) {
        val saksbehandler = ContextService.hentSaksbehandler(SecureLog.Context.tom())
        val fagsystem = tilbakekreving.tilFrontendDto().fagsystem
        val logContext = SecureLog.Context.medBehandling(tilbakekreving.eksternFagsak.eksternId, behandlingId?.toString())
        val dto = tilbakekreving.tilFrontendDto()
        validate(
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
            fagsystem = fagsak.fagsystem,
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
            fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype),
            eksternFagsakId = eksternFagsakId,
            minimumBehandlerrolle = minimumBehandlerrolle,
            auditLoggerEvent = auditLoggerEvent,
            handling = handling,
        )
    }

    fun validerTilgangFagsystemOgFagsakId(
        fagsystem: Fagsystem,
        eksternFagsakId: String,
        minimumBehandlerrolle: Behandlerrolle,
        auditLoggerEvent: AuditLoggerEvent,
        handling: String,
    ) {
        val saksbehandler = ContextService.hentSaksbehandler(SecureLog.Context.tom())
        val fagsak = fagsakRepository.findByFagsystemAndEksternFagsakId(fagsystem, eksternFagsakId)
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
            fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(økonomiXmlMottatt.ytelsestype),
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
            fagsystem = FagsystemUtil.hentFagsystemFraYtelsestype(ytelsestype),
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
        fagsystem: Fagsystem,
        minimumBehandlerrolle: Behandlerrolle,
        ident: String?,
        eksternFagsakId: String?,
        handling: String,
        saksbehandler: String,
        auditLoggerEvent: AuditLoggerEvent,
        logContext: SecureLog.Context,
    ) {
        if (saksbehandler == Constants.BRUKER_ID_VEDTAKSLØSNINGEN) {
            // når behandler har system tilgang, trenges ikke det validering på fagsystem eller rolle
            return
        }
        val brukerRolleOgFagsystemstilgang =
            ContextService.hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(rolleConfig, handling, SecureLog.Context.tom())

        // når behandler har forvaltningstilgang, blir rollen bare validert
        if (brukerRolleOgFagsystemstilgang.tilganger.contains(Tilgangskontrollsfagsystem.FORVALTER_TILGANG)) {
            validateForvaltingsrolle(
                brukerRolleOgFagsystemstilgang = brukerRolleOgFagsystemstilgang,
                minimumBehandlerrolle = minimumBehandlerrolle,
                handling = handling,
                saksbehandler = saksbehandler,
            )
        } else {
            val tilgangskontrollsfagsystem = Tilgangskontrollsfagsystem.fraFagsystem(fagsystem)
            // sjekk om saksbehandler har riktig gruppe å aksessere denne ytelsestypen
            validateFagsystem(tilgangskontrollsfagsystem, brukerRolleOgFagsystemstilgang, handling, saksbehandler)

            // sjekk om saksbehandler har riktig rolle å aksessere denne ytelsestypen
            validateRolle(
                brukersrolleTilFagsystemet = brukerRolleOgFagsystemstilgang.tilganger.getValue(tilgangskontrollsfagsystem),
                minimumBehandlerrolle = minimumBehandlerrolle,
                handling = handling,
                logContext = logContext,
                saksbehandler = saksbehandler,
            )
        }

        validateEgenAnsattKode6Kode7(
            personIBehandlingen = ident,
            fagsystem = fagsystem,
            handling = handling,
            saksbehandler = saksbehandler,
        )

        if (ident != null) {
            logAccess(auditLoggerEvent, ident, eksternFagsakId!!)
        }
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
        fagsystem: Fagsystem,
        handling: String,
        saksbehandler: String,
    ) {
        if (personIBehandlingen == null) return

        val tilganger = integrasjonerClient.sjekkTilgangTilPersoner(listOf(personIBehandlingen), fagsystem.tilTema())
        if (tilganger.any { !it.harTilgang }) {
            throw Feil(
                message = "$saksbehandler har ikke tilgang til person i $handling",
                frontendFeilmelding = "$saksbehandler  har ikke tilgang til person i $handling",
                logContext = SecureLog.Context.tom(),
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }
    }

    private fun Fagsystem.tilTema() =
        when (this) {
            Fagsystem.BA -> Tema.BAR
            Fagsystem.KONT, Fagsystem.KS -> Tema.KON
            Fagsystem.EF -> Tema.ENF
            Fagsystem.IT01 -> throw Feil(
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
            throw Feil(
                message = "$saksbehandler har ikke tilgang til $handling",
                frontendFeilmelding = "$saksbehandler  har ikke tilgang til $handling",
                logContext = SecureLog.Context.tom(),
                httpStatus = HttpStatus.FORBIDDEN,
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
        if (minimumBehandlerrolle == Behandlerrolle.FORVALTER) {
            throw Feil(
                message =
                    "$saksbehandler med rolle $brukersrolleTilFagsystemet " +
                        "har ikke tilgang til å kalle forvaltningstjeneste $handling. Krever FORVALTER.",
                frontendFeilmelding = "Du har ikke tilgang til å $handling.",
                logContext = logContext,
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }
        if (minimumBehandlerrolle.nivå > brukersrolleTilFagsystemet.nivå) {
            throw Feil(
                message =
                    "$saksbehandler med rolle $brukersrolleTilFagsystemet " +
                        "har ikke tilgang til å $handling. Krever $minimumBehandlerrolle.",
                frontendFeilmelding = "Du har ikke tilgang til å $handling.",
                logContext = logContext,
                httpStatus = HttpStatus.FORBIDDEN,
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
        if (minimumBehandlerrolle.nivå > Behandlerrolle.FORVALTER.nivå &&
            tilganger.all { it.value == Behandlerrolle.FORVALTER }
        ) {
            throw Feil(
                message =
                    "$saksbehandler med rolle FORVALTER " +
                        "har ikke tilgang til å $handling. Krever $minimumBehandlerrolle.",
                frontendFeilmelding = "Du har ikke tilgang til å $handling.",
                logContext = SecureLog.Context.tom(),
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }
    }
}
