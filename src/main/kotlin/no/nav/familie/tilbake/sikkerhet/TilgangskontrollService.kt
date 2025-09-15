package no.nav.familie.tilbake.sikkerhet

import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import java.math.BigInteger
import java.util.UUID

interface TilgangskontrollService {
    fun validerTilgangTilbakekreving(
        tilbakekreving: Tilbakekreving,
        behandlingId: UUID?,
        minimumBehandlerrolle: Behandlerrolle,
        auditLoggerEvent: AuditLoggerEvent,
        handling: String,
    ): Behandlerrolle

    fun validerTilgangBehandlingID(
        behandlingId: UUID,
        minimumBehandlerrolle: Behandlerrolle,
        auditLoggerEvent: AuditLoggerEvent,
        handling: String,
    )

    fun validerTilgangYtelsetypeOgFagsakId(
        ytelsestype: Ytelsestype,
        eksternFagsakId: String,
        minimumBehandlerrolle: Behandlerrolle,
        auditLoggerEvent: AuditLoggerEvent,
        handling: String,
    )

    fun validerTilgangFagsystemOgFagsakId(
        fagsystem: FagsystemDTO,
        eksternFagsakId: String,
        minimumBehandlerrolle: Behandlerrolle,
        auditLoggerEvent: AuditLoggerEvent,
        handling: String,
    )

    fun validerTilgangMottattXMLId(
        mottattXmlId: UUID,
        minimumBehandlerrolle: Behandlerrolle,
        auditLoggerEvent: AuditLoggerEvent,
        handling: String,
    )

    fun validerTilgangKravgrunnlagId(
        eksternKravgrunnlagId: BigInteger,
        minimumBehandlerrolle: Behandlerrolle,
        auditLoggerEvent: AuditLoggerEvent,
        handling: String,
    )

    fun logAccess(
        auditLoggerEvent: AuditLoggerEvent,
        ident: String,
        eksternFagsakId: String,
        behandling: Behandling? = null,
    )
}
