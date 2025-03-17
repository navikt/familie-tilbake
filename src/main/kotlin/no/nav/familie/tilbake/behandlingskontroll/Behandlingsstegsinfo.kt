package no.nav.familie.tilbake.behandlingskontroll

import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import java.time.LocalDate

data class Behandlingsstegsinfo(
    val behandlingssteg: Behandlingssteg,
    val behandlingsstegstatus: Behandlingsstegstatus,
    val venteårsak: Venteårsak? = null,
    val tidsfrist: LocalDate? = null,
)
