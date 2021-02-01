package no.nav.familie.tilbake.integration.pdl.internal

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
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

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlPersonData(@JsonProperty("foedsel") val fødsel: List<PdlFødselsDato>,
                         val navn: List<PdlNavn>,
                         @JsonProperty("kjoenn") val kjønn: List<PdlKjønn>)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlFødselsDato(@JsonProperty("foedselsdato") val fødselsdato: String?)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlError(val message: String)

@JsonIgnoreProperties(ignoreUnknown = true)
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

@JsonIgnoreProperties(ignoreUnknown = true)
data class PdlKjønn(@JsonProperty("kjoenn") val kjønn: Kjønn)

enum class Kjønn {
    MANN, KVINNE, UKJENT
}
