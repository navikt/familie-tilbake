package no.nav.familie.tilbake.data

import no.nav.familie.tilbake.domain.Behandling
import no.nav.familie.tilbake.domain.Bruker
import no.nav.familie.tilbake.domain.Fagsak
import java.time.LocalDate
import java.util.*

object Testdata {

    val bruker = Bruker(ident = "321321321")

    val fagsak = Fagsak(
            eksternFagsakId = "testverdi",
            fagsakstatus = "oujh",
            brukerId = bruker.id)

    val behandling = Behandling(
            fagsakId = fagsak.id,
            behandlingsstatus = "testverdi",
            behandlingstype = "testverdi",
            opprettetDato = LocalDate.now(),
            avsluttetDato = LocalDate.now(),
            ansvarligSaksbehandler = "testverdi",
            ansvarligBeslutter = "testverdi",
            behandlendeEnhet = "testverdi",
            behandlendeEnhetNavn = "testverdi",
            manueltOpprettet = true,
            eksternId = UUID.randomUUID(),
            saksbehandlingstype = "ORDINÆR",
    )


}