package no.nav.tilbakekreving.e2e.config

import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.util.UUID

@Profile("ny-modell")
@Primary
@Service
class TilgangskontrollServiceMock : TilgangskontrollService {
    override fun validerTilgangTilbakekreving(tilbakekreving: Tilbakekreving, behandlingId: UUID?, minimumBehandlerrolle: Behandlerrolle, auditLoggerEvent: AuditLoggerEvent, handling: String): Behandlerrolle {
        return Behandlerrolle.SYSTEM
    }

    override fun validerTilgangBehandlingID(behandlingId: UUID, minimumBehandlerrolle: Behandlerrolle, auditLoggerEvent: AuditLoggerEvent, handling: String) {}

    override fun validerTilgangYtelsetypeOgFagsakId(ytelsestype: Ytelsestype, eksternFagsakId: String, minimumBehandlerrolle: Behandlerrolle, auditLoggerEvent: AuditLoggerEvent, handling: String) {}

    override fun validerTilgangFagsystemOgFagsakId(fagsystem: FagsystemDTO, eksternFagsakId: String, minimumBehandlerrolle: Behandlerrolle, auditLoggerEvent: AuditLoggerEvent, handling: String) {}

    override fun validerTilgangMottattXMLId(mottattXmlId: UUID, minimumBehandlerrolle: Behandlerrolle, auditLoggerEvent: AuditLoggerEvent, handling: String) {}

    override fun validerTilgangKravgrunnlagId(eksternKravgrunnlagId: BigInteger, minimumBehandlerrolle: Behandlerrolle, auditLoggerEvent: AuditLoggerEvent, handling: String) {}

    override fun logAccess(auditLoggerEvent: AuditLoggerEvent, ident: String, eksternFagsakId: String, behandling: Behandling?) {}
}
