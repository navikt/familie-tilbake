package no.nav.familie.tilbake.data

import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.behandling.domain.EksternBehandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Fagsystem
import no.nav.familie.tilbake.behandling.domain.Ytelsestype
import no.nav.familie.tilbake.varsel.Varsel
import no.nav.familie.tilbake.varsel.Varselsperiode
import no.nav.familie.tilbake.verge.Verge
import no.nav.familie.tilbake.verge.Vergetype
import java.time.LocalDate
import java.util.UUID

object Testdata {

    private val bruker = Bruker(ident = "321321321")

    val fagsak = Fagsak(ytelsestype = Ytelsestype.BA,
                        fagsystem = Fagsystem.BA,
                        eksternFagsakId = "testverdi",
                        bruker = bruker)

    val eksternBehandling = EksternBehandling(eksternId = UUID.randomUUID().toString())
    val date = LocalDate.now()

    val varsel = Varsel(varseltekst = "testverdi",
                        varselbeløp = 123,
                        revurderingsvedtaksdato = date.minusDays(1),
                        perioder = setOf(Varselsperiode(fom = date.minusMonths(2), tom = date)))

    val verge = Verge(ident = "testverdi",
                      gyldigFom = LocalDate.now(),
                      gyldigTom = LocalDate.now(),
                      type = Vergetype.BARN,
                      orgNr = "testverdi",
                      navn = "testverdi",
                      kilde = "testverdi",
                      begrunnelse = "testverdi")

    val behandlingsresultat = Behandlingsresultat()

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
                                resultater = setOf(behandlingsresultat),
                                varsler = setOf(varsel),
                                verger = setOf(verge),
                                eksternBrukId = UUID.randomUUID())


}
