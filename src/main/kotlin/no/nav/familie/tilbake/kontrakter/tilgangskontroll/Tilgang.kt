package no.nav.familie.tilbake.kontrakter.tilgangskontroll

/**
 * @param personIdent er identen på den personen man har sjekket tilgang på
 * I de tilfeller der man sender en ident for å sjekker tilgang på personMedRelasjoner,
 * så returnerer man fortsatt bare identen til den person som man etterspurt
 */
data class Tilgang(
    val personIdent: String,
    val harTilgang: Boolean,
    val begrunnelse: String? = null,
)
