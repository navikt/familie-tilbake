package no.nav.familie.tilbake

import no.nav.familie.tilbake.config.ApplicationProperties
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.BehandlingHistorikk
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandling
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype
import no.nav.tilbakekreving.person.Bruker
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.util.UUID

@Service
class TilbakekrevingService(
    private val applicationProperties: ApplicationProperties,
) {
    private val behovObservatør =
        object : BehovObservatør {
            override fun håndter(behov: FagsysteminfoBehov) {}

            override fun håndter(behov: VarselbrevBehov) {}
        }

    private val eksternFagsakBehandlinger =
        mutableListOf(
            EksternFagsakBehandling(UUID.randomUUID().toString()),
        )

    private val eksternFagsak =
        EksternFagsak(
            eksternId = "TEST-101010",
            ytelsestype = Ytelsestype.BARNETRYGD,
            fagsystem = Fagsystem.BA,
            behandlinger = EksternFagsakBehandlingHistorikk(eksternFagsakBehandlinger),
            behovObservatør = behovObservatør,
        )
    private val eksempelsaker =
        listOf(
            Tilbakekreving(
                eksternFagsak,
                opprettet = LocalDateTime.of(2025, Month.MARCH, 15, 12, 0),
                behandlingHistorikk =
                    BehandlingHistorikk(
                        mutableListOf(
                            Behandling(
                                internId = UUID.fromString("abcdef12-1337-1338-1339-abcdef123456"),
                                eksternId = UUID.fromString("abcdef12-1337-1338-1339-abcdef123456"),
                                behandlingstype = Behandlingstype.TILBAKEKREVING,
                                opprettet = LocalDateTime.now(),
                                enhet = null,
                                fagsystembehandling = eksternFagsakBehandlinger.first(),
                                årsak = Behandlingsårsakstype.REVURDERING_OPPLYSNINGER_OM_VILKÅR,
                                begrunnelseForTilbakekreving = "WIP",
                                ansvarligSaksbehandler = "VL",
                                eksternFagsak = eksternFagsak,
                            ),
                        ),
                    ),
                bruker =
                    Bruker(
                        ident = "20046912345",
                        språkkode = Språkkode.NB,
                        fødselsdato = LocalDate.of(1969, Month.APRIL, 20),
                    ),
                behovObservatør = behovObservatør,
            ),
        )

    fun hentTilbakekreving(
        fagsystem: Fagsystem,
        eksternFagsakId: String,
    ): Tilbakekreving? {
        if (!applicationProperties.toggles.nyModellEnabled) return null

        return eksempelsaker.firstOrNull { it.tilFrontendDto().fagsystem == fagsystem && it.tilFrontendDto().eksternFagsakId == eksternFagsakId }
    }

    fun hentTilbakekreving(behandlingId: UUID): Tilbakekreving? {
        if (!applicationProperties.toggles.nyModellEnabled) return null

        return eksempelsaker.firstOrNull { sak -> sak.tilFrontendDto().behandlinger.any { it.eksternBrukId == behandlingId } }
    }
}
