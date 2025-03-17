package no.nav.tilbakekreving.kontrakter.behandling

enum class Saksbehandlingstype {
    ORDINÆR,
    AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP,
    AUTOMATISK_IKKE_INNKREVING_UNDER_4X_RETTSGEBYR,
    ;

    fun erAutomatisk() = this == AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP || this == AUTOMATISK_IKKE_INNKREVING_UNDER_4X_RETTSGEBYR
}
