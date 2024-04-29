package no.nav.familie.tilbake.behandling.steg

import java.util.*
import no.nav.familie.kontrakter.felles.Regelverk
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.common.exceptionhandler.Feil

fun validerAtBehandlingIkkeErAvsluttet(behandling: Behandling) {
    if (behandling.erSaksbehandlingAvsluttet) {
        throw Feil("Behandling med id=${behandling.id} er allerede ferdig behandlet")
    }
}

fun validerAtUtomatiskBehandlingIkkeErEøs(behandling: Behandling) {
    if (behandling.regelverk == Regelverk.EØS) {
        throw Feil("Behandling med id=${behandling.id} behandles etter EØS-regelverket, og skal dermed ikke behandles automatisk.")
    }
}

fun validerAtBehandlingIkkeErPåVent(
    behandlingId: UUID,
    erBehandlingPåVent: Boolean,
    behandledeSteg: String,
) {
    if (erBehandlingPåVent) {
        throw Feil(message = "Behandling med id=$behandlingId er på vent, kan ikke behandle steg $behandledeSteg")
    }
}

fun validerAtBehandlingErAutomatisk(behandling: Behandling) {
    if (behandling.saksbehandlingstype == Saksbehandlingstype.ORDINÆR) {
        throw Feil(
            message =
            "Behandling med id=${behandling.id} er satt til ordinær saksbehandling. " +
                    "Kan ikke saksbehandle den automatisk",
        )
    }
}