package no.nav.familie.tilbake.data

import no.nav.familie.tilbake.domain.*
import java.time.LocalDate
import java.util.*

object Testdata {

    private val bruker = Bruker(ident = "321321321")

    val fagsak = Fagsak(ytelsestype = Ytelsestype.BA,
                        eksternFagsakId = "testverdi",
                        bruker = bruker)

    val eksternBehandling = EksternBehandling(henvisning = "testverdi",
                                              eksternId = UUID.randomUUID())

    val varsel = Varsel(varseltekst = "testverdi",
                        varselbeløp = 123)

    val verge = Verge(ident = "testverdi",
                      gyldigFom = LocalDate.now(),
                      gyldigTom = LocalDate.now(),
                      type = Vergetype.BARN,
                      orgNr = "testverdi",
                      navn = "testverdi",
                      kilde = "testverdi",
                      begrunnelse = "testverdi")

    val behandling = Behandling(fagsakId = fagsak.id,
                                type = Behandlingstype.TILBAKEKREVING,
                                opprettetDato = LocalDate.now(),
                                avsluttetDato = LocalDate.now(),
                                ansvarligSaksbehandler = "testverdi",
                                ansvarligBeslutter = "testverdi",
                                behandlendeEnhet = "testverdi",
                                behandlendeEnhetsNavn = "testverdi",
                                manueltOpprettet = true,
                                eksternBehandling = setOf(eksternBehandling),
                                verger = setOf(verge),
                                varsler = setOf(varsel),
                                eksternId = UUID.randomUUID())
}
