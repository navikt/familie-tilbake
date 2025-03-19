package no.nav.familie.tilbake

import no.nav.familie.tilbake.config.ApplicationProperties
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.eksternfagsak.EksternFagsak
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.Month

@Service
class TilbakekrevingService(
    private val applicationProperties: ApplicationProperties,
) {
    private val eksempelsaker =
        listOf(
            Tilbakekreving(
                EksternFagsak(
                    eksternId = "TEST-101010",
                    ytelsestype = Ytelsestype.BARNETRYGD,
                    fagsystem = Fagsystem.BA,
                ),
                opprettet = LocalDateTime.of(2025, Month.MARCH, 15, 12, 0),
            ),
        )

    fun hentTilbakekreving(
        fagsystem: Fagsystem,
        eksternFagsakId: String,
    ): Tilbakekreving? {
        if (!applicationProperties.toggles.nyModellEnabled) return null

        return eksempelsaker.firstOrNull { it.tilFrontendDto().fagsystem == fagsystem && it.tilFrontendDto().eksternFagsakId == eksternFagsakId }
    }
}
