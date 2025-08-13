package no.nav.tilbakekreving.e2e

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.tilbakekreving.api.v1.dto.BehandlingPåVentDto
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.saksbehandler.Behandler
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandlingskontrollE2ETest : TilbakekrevingE2EBase() {
    @Test
    fun `sett behandling på vent`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )

        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()

        behandlingController.settBehandlingPåVent(
            behandlingId = behandlingId,
            behandlingPåVentDto = BehandlingPåVentDto(
                venteårsak = Venteårsak.MANGLER_STØTTE,
                tidsfrist = LocalDate.MAX,
                begrunnelse = "Ikke implementert!",
            ),
        ) shouldBe Ressurs.success("OK")
        val behandler = Behandler.Saksbehandler("A123456")
        val frontendDtoPåVent = tilbakekrevingService.hentTilbakekreving(behandlingId)
            .shouldNotBeNull()
            .behandlingHistorikk
            .nåværende()
            .entry
            .tilFrontendDto(behandler, true)

        frontendDtoPåVent.erBehandlingPåVent shouldBe true

        behandlingController.taBehandlingAvVent(behandlingId) shouldBe Ressurs.success("OK")
        val frontendDtoAvVent = tilbakekrevingService.hentTilbakekreving(behandlingId)
            .shouldNotBeNull()
            .behandlingHistorikk
            .nåværende()
            .entry
            .tilFrontendDto(behandler, true)

        frontendDtoAvVent.erBehandlingPåVent shouldBe false
    }
}
