package no.nav.familie.tilbake.kontrakter

import java.io.PrintWriter
import java.io.StringWriter

/**
 * Objekt som brukes for utveksling av data mellom familietjenester.
 * Brukes både mellom systemer og til frontend.
 *
 * @param T typen til data i objektet.
 * @param status status på request. Kan være 200 OK med feilet ressurs
 * @param melding teknisk melding som ikke skal inneholde sensitive data
 * @param frontendFeilmelding feilmelding forbehold frontend og kan inneholde sensitive data
 * @param stacktrace stacktrace fra feil som kan være nyttig til debugging i familie-prosessering
 */
data class Ressurs<T>(
    val data: T?,
    val status: Status,
    val melding: String,
    val frontendFeilmelding: String? = null,
    val stacktrace: String?,
) {
    enum class Status {
        SUKSESS,
        FEILET,
        IKKE_HENTET,
        IKKE_TILGANG,
        FUNKSJONELL_FEIL,
    }

    companion object {
        fun <T> success(data: T): Ressurs<T> =
            Ressurs(
                data = data,
                status = Status.SUKSESS,
                melding = "Innhenting av data var vellykket",
                stacktrace = null,
            )

        fun <T> success(
            data: T,
            melding: String?,
        ): Ressurs<T> =
            Ressurs(
                data = data,
                status = Status.SUKSESS,
                melding = melding ?: "Innhenting av data var vellykket",
                stacktrace = null,
            )

        fun <T> failure(
            errorMessage: String? = null,
            frontendFeilmelding: String? = null,
            error: Throwable? = null,
        ): Ressurs<T> =
            Ressurs(
                data = null,
                status = Status.FEILET,
                melding = errorMessage ?: "En feil har oppstått: ${error?.message}",
                frontendFeilmelding = frontendFeilmelding,
                stacktrace = error?.textValue(),
            )

        fun <T> ikkeTilgang(
            melding: String,
            frontendFeilMelding: String,
        ): Ressurs<T> =
            Ressurs(
                data = null,
                status = Status.IKKE_TILGANG,
                melding = melding,
                frontendFeilmelding = frontendFeilMelding,
                stacktrace = null,
            )

        fun <T> funksjonellFeil(
            melding: String,
            frontendFeilmelding: String? = null,
        ): Ressurs<T> =
            Ressurs(
                data = null,
                status = Status.FUNKSJONELL_FEIL,
                melding = melding,
                frontendFeilmelding = frontendFeilmelding,
                stacktrace = null,
            )

        private fun Throwable.textValue(): String {
            val sw = StringWriter()
            this.printStackTrace(PrintWriter(sw))
            return sw.toString()
        }
    }

    fun toJson(): String = objectMapper.writeValueAsString(this)

    override fun toString(): String = "Ressurs(status=$status, melding='$melding')"

    fun toSecureString(): String = "Ressurs(status=$status, melding='$melding', frontendFeilmelding='$frontendFeilmelding')"
}

fun <T> Ressurs<T>.getDataOrThrow(): T =
    when (this.status) {
        Ressurs.Status.SUKSESS -> data ?: error("Data er null i Ressurs")
        else -> error(melding)
    }
