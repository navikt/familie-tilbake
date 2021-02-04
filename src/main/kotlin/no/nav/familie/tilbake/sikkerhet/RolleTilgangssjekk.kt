package no.nav.familie.tilbake.sikkerhet


@Target(AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RolleTilgangssjekk(val minimumBehandlerRolle: BehandlerRolle,
                                    val handling: String,
                                    val henteParam: String = "") //brukes kun i GET request for å hente fagsystem
