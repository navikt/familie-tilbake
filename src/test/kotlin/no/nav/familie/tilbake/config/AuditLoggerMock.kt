package no.nav.familie.tilbake.config

import no.nav.familie.tilbake.sikkerhet.AuditLogger
import no.nav.familie.tilbake.sikkerhet.Sporingsdata
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class AuditLoggerMock : AuditLogger {
    override fun log(data: Sporingsdata) {
    }
}
