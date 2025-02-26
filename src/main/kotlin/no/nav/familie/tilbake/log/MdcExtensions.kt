package no.nav.familie.tilbake.log

import org.slf4j.MDC

fun callId(): String? = MDC.get("callId")

fun requestId(): String? = MDC.get("requestId")

fun putCallId(callId: String) = MDC.put("callId", callId)

fun putConsumerId(consumerId: String) = MDC.put("consumerId", consumerId)

fun putRequestId(requestId: String) = MDC.put("requestId", requestId)

fun removeCallId() = MDC.remove("callId")

fun removeConsumerId() = MDC.remove("consumerId")

fun removeRequestId() = MDC.remove("requestId")
