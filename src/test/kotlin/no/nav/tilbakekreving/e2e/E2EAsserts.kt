package no.nav.tilbakekreving.e2e

import io.kotest.inspectors.forNone
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.saksbehandler.Behandler

infix fun Tilbakekreving.kanBehandle(behandlingssteg: Behandlingssteg) {
    val steg = frontendDtoForBehandling(Behandler.Saksbehandler("A123456"), true).behandlingsstegsinfo.singleOrNull {
        it.behandlingssteg == behandlingssteg
    }.shouldNotBeNull()

    steg.behandlingsstegstatus shouldBe Behandlingsstegstatus.KLAR
}

infix fun Tilbakekreving.avventerBehandling(behandlingssteg: Behandlingssteg) {
    frontendDtoForBehandling(Behandler.Saksbehandler("A123456"), true).behandlingsstegsinfo.forNone {
        it.behandlingssteg shouldBe behandlingssteg
    }
}
