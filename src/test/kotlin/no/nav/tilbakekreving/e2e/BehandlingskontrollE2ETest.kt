package no.nav.tilbakekreving.e2e

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.tilbakekreving.api.v1.dto.BehandlingPåVentDto
import no.nav.tilbakekreving.e2e.ytelser.TilleggstønaderE2ETest.Companion.TILLEGGSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.junit.jupiter.api.Test

class BehandlingskontrollE2ETest : TilbakekrevingE2EBase() {
    @Test
    fun `sett behandling på vent`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTillegstønader(
                fagsystemId = fagsystemId,
            ),
        )


        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()

        behandlingController.settBehandlingPåVent(
            behandlingId = behandlingId,
            behandlingPåVentDto = BehandlingPåVentDto(
                venteårsak = Venteårsak.MANGLER_STØTTE,
                tidsfrist = LocalDate.MAX,
                begrunnelse = "Ikke implementert!"
            )
        ) shouldBe Ressurs.success("OK")

        val frontendDtoPåVent = tilbakekrevingService.hentTilbakekreving(behandlingId)
            .shouldNotBeNull()
            .behandlingHistorikk
            .nåværende()
            .entry
            .tilFrontendDto()

        frontendDtoPåVent.erBehandlingPåVent shouldBe true

        behandlingController.taBehandlingAvVent(behandlingId) shouldBe Ressurs.success("OK")
        val frontendDtoAvVent = tilbakekrevingService.hentTilbakekreving(behandlingId)
            .shouldNotBeNull()
            .behandlingHistorikk
            .nåværende()
            .entry
            .tilFrontendDto()

        frontendDtoAvVent.erBehandlingPåVent shouldBe false
    }
}
