package no.tilbakekreving.integrasjoner.persontilgang

sealed interface Persontilgang {
    object Ok : Persontilgang

    data class IkkeTilgang(val begrunnelse: String, val begrunnelseType: AvvisningskodeType) : Persontilgang {
        enum class AvvisningskodeType {
            AVVIST_STRENGT_FORTROLIG_ADRESSE,
            AVVIST_STRENGT_FORTROLIG_UTLAND,
            AVVIST_PERSON_UTLAND,
            AVVIST_SKJERMING,
            AVVIST_FORTROLIG_ADRESSE,
            AVVIST_HABILITET,
            UKJENT,
        }
    }
}
