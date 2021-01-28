package no.nav.familie.tilbake.common.exceptionhandler

import java.net.URI

class IntegrasjonException(msg: String,
                           throwable: Throwable? = null,
                           val uri: URI? = null,
                           val data: Any? = null) : RuntimeException(msg, throwable)
