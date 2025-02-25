package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.kontrakter.Regelverk
import no.nav.familie.tilbake.log.SecureLog
import java.util.UUID

fun validerAtBehandlingIkkeErAvsluttet(
    behandling: Behandling,
    logContext: SecureLog.Context,
) {
    if (behandling.erSaksbehandlingAvsluttet) {
        throw Feil(
            message = "Behandling med id=${behandling.id} er allerede ferdig behandlet",
            logContext = logContext,
        )
    }
}

fun validerAtUtomatiskBehandlingIkkeErEøs(
    behandling: Behandling,
    logContext: SecureLog.Context,
) {
    if (behandling.regelverk == Regelverk.EØS && behandling.saksbehandlingstype != Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_UNDER_4X_RETTSGEBYR) {
        throw Feil(
            message = "Behandling med id=${behandling.id} behandles etter EØS-regelverket, og skal dermed ikke behandles automatisk.",
            logContext = logContext,
        )
    }
}

fun validerAtBehandlingIkkeErPåVent(
    behandlingId: UUID,
    erBehandlingPåVent: Boolean,
    behandledeSteg: String,
    logContext: SecureLog.Context,
) {
    if (erBehandlingPåVent) {
        throw Feil(
            message = "Behandling med id=$behandlingId er på vent, kan ikke behandle steg $behandledeSteg",
            logContext = logContext,
        )
    }
}

fun validerAtBehandlingErAutomatisk(
    behandling: Behandling,
    logContext: SecureLog.Context,
) {
    if (behandling.saksbehandlingstype == Saksbehandlingstype.ORDINÆR) {
        throw Feil(
            "Behandling med id=${behandling.id} er satt til ordinær saksbehandling. Kan ikke saksbehandle den automatisk",
            logContext = logContext,
        )
    }
}
