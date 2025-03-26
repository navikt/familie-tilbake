package no.nav.familie.tilbake

import no.nav.familie.tilbake.config.ApplicationProperties
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v2.EksternFagsakDto
import no.nav.tilbakekreving.api.v2.OpprettTilbakekrevingEvent
import no.nav.tilbakekreving.api.v2.Opprettelsevalg
import no.nav.tilbakekreving.behandling.BehandlingHistorikk
import no.nav.tilbakekreving.behov.BehovObservatør
import no.nav.tilbakekreving.behov.FagsysteminfoBehov
import no.nav.tilbakekreving.behov.VarselbrevBehov
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.VarselbrevSendtHendelse
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

    private val eksternFagsak =
        EksternFagsak(
            eksternId = "TEST-101010",
            ytelsestype = Ytelsestype.BARNETRYGD,
            fagsystem = Fagsystem.BA,
            behandlinger = EksternFagsakBehandlingHistorikk(mutableListOf()),
            behovObservatør = behovObservatør,
        )
    private val eksempelsaker =
        listOf(
            Tilbakekreving(
                eksternFagsak,
                opprettet = LocalDateTime.of(2025, Month.MARCH, 15, 12, 0),
                behandlingHistorikk =
                    BehandlingHistorikk(mutableListOf()),
                bruker =
                    Bruker(
                        ident = "20046912345",
                        språkkode = Språkkode.NB,
                        fødselsdato = LocalDate.of(1969, Month.APRIL, 20),
                    ),
                behovObservatør = behovObservatør,
            ).apply {
                håndter(
                    OpprettTilbakekrevingEvent(
                        EksternFagsakDto(
                            fagsystem = Fagsystem.BA,
                            ytelsestype = Ytelsestype.BARNETRYGD,
                            eksternId = "TEST-101010",
                        ),
                        opprettelsesvalg = Opprettelsevalg.OPPRETT_BEHANDLING_MED_VARSEL,
                    ),
                )
                håndter(KravgrunnlagHendelse())
                håndter(
                    FagsysteminfoHendelse(
                        eksternId = UUID.randomUUID().toString(),
                    ),
                )
                håndter(VarselbrevSendtHendelse)
            },
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
