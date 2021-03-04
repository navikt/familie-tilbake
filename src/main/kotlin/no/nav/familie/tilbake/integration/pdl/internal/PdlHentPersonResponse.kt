package no.nav.familie.tilbake.integration.pdl.internal

import com.fasterxml.jackson.annotation.JsonProperty

data class PdlHentPersonResponse<T>(val data: T,
                                    val errors: List<PdlError>?) {

    fun harFeil(): Boolean {
        return errors != null && errors.isNotEmpty()
    }

    fun errorMessages(): String {
        return errors?.joinToString { it -> it.message } ?: ""
    }
}

data class PdlPerson(val person: PdlPersonData?)


data class PdlPersonData(@JsonProperty("foedsel") val fødsel: List<PdlFødselsDato>,
                         val navn: List<PdlNavn>,
                         @JsonProperty("kjoenn") val kjønn: List<PdlKjønn>)


data class PdlFødselsDato(@JsonProperty("foedselsdato") val fødselsdato: String?)


data class PdlError(val message: String,
                    val locations: List<PdlErrorLocation>,
                    val path: List<String>?,
                    val extensions: PdlErrorExtension
)


data class PdlErrorLocation(val line: Int?,
                            val column: Int?
)


data class PdlErrorExtension(val code: String?,
                             val details: PdlErrorDetails,
                             val classification: String
)


data class PdlErrorDetails(val type: String,
                           val cause: String,
                           val policy: String)


data class PdlNavn(val fornavn: String,
                   val mellomnavn: String? = null,
                   val etternavn: String) {

    fun fulltNavn(): String {
        return when (mellomnavn) {
            null -> "$fornavn $etternavn"
            else -> "$fornavn $mellomnavn $etternavn"
        }
    }
}


data class PdlKjønn(@JsonProperty("kjoenn") val kjønn: Kjønn)

enum class Kjønn {
    MANN,
    KVINNE,
    UKJENT
}
