package no.nav.familie.tilbake.common.exceptionhandler

import no.nav.familie.tilbake.log.SecureLog
import java.net.URI

open class IntegrasjonException(
    msg: String,
    val logContext: SecureLog.Context,
    throwable: Throwable? = null,
    val uri: URI? = null,
    val data: Any? = null,
) : RuntimeException(msg, throwable)
