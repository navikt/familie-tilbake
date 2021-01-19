package no.nav.familie.tilbake.data

import no.nav.familie.tilbake.domain.*
import java.time.LocalDate
import java.util.*

object Testdata {

    private val bruker = Bruker(ident = "321321321")

    val fagsak = Fagsak(ytelsestype = Ytelsestype.BA,
                        eksternFagsakId = "testverdi",
                        bruker = bruker)

    val behandling = Behandling(fagsakId = fagsak.id,
                                type = Behandlingstype.TILBAKEKREVING,
                                opprettetDato = LocalDate.now(),
                                avsluttetDato = LocalDate.now(),
                                ansvarligSaksbehandler = "testverdi",
                                ansvarligBeslutter = "testverdi",
                                behandlendeEnhet = "testverdi",
                                behandlendeEnhetsNavn = "testverdi",
                                manueltOpprettet = true,
                                eksternId = UUID.randomUUID())


}