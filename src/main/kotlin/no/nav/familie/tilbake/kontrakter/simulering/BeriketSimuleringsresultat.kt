package no.nav.familie.tilbake.kontrakter.simulering

data class BeriketSimuleringsresultat(
    val detaljer: DetaljertSimuleringResultat,
    val oppsummering: Simuleringsoppsummering,
)
