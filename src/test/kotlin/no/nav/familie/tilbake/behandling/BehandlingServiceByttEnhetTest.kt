package no.nav.familie.tilbake.behandling

import io.kotest.inspectors.forOne
import io.kotest.matchers.shouldBe
import io.mockk.verify
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.Historikkinnslag
import no.nav.familie.tilbake.oppgave.OppgaveService
import no.nav.tilbakekreving.api.v1.dto.ByttEnhetDto
import no.nav.tilbakekreving.e2e.ContextServiceHelpers
import no.nav.tilbakekreving.kontrakter.Tilbakekrevingsvalg
import no.nav.tilbakekreving.kontrakter.historikk.Historikkinnslagstype
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import org.springframework.beans.factory.annotation.Autowired

@Isolated
internal class BehandlingServiceByttEnhetTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var historikkService: HistorikkService

    @Autowired
    private lateinit var oppgaveService: OppgaveService

    @Test
    fun `byttBehandlendeEnhet skal bytte og oppdatere oppgave`() {
        val opprettTilbakekrevingRequest =
            BehandlingServiceTest.lagOpprettTilbakekrevingRequest(
                tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
                fagsystem = FagsystemDTO.BA,
                ytelsestype = YtelsestypeDTO.BARNETRYGD,
            )
        var behandling = ContextServiceHelpers.somSaksbehandler(grupper = listOf(BehandlingServiceTest.BARNETRYGD_SAKSBEHANDLER_ROLLE)) {
            behandlingService.opprettBehandling(opprettTilbakekrevingRequest)
        }
        behandling = behandlingRepository.findByIdOrThrow(behandling.id)

        ContextServiceHelpers.somSaksbehandler(grupper = listOf(BehandlingServiceTest.BARNETRYGD_SAKSBEHANDLER_ROLLE)) {
            behandlingService.byttBehandlendeEnhet(
                behandling.id,
                ByttEnhetDto(
                    "4806",
                    "bytter i unittest" + "\n\nmed linjeskift" + "\n\nto til og med",
                ),
            )
        }

        behandling = behandlingRepository.findByIdOrThrow(behandling.id)
        behandling.behandlendeEnhet shouldBe "4806"
        behandling.behandlendeEnhetsNavn shouldBe "jnkmmk"

        verify(exactly = 1) {
            oppgaveService.patchOppgave(
                match {
                    it.id == 1L && it.tilordnetRessurs == SAKSBEHANDLER_IDENT && it.endretAvEnhetsnr == "0425"
                },
            )
        }

        verify(exactly = 1) {
            oppgaveService.tilordneOppgaveNyEnhet(1L, "4806", true, false)
        }

        historikkService.hentHistorikkinnslag(behandling.id).forOne {
            it.type shouldBe Historikkinnslagstype.HENDELSE
            it.tekst shouldBe "Ny enhet: 4806, Begrunnelse: bytter i unittest  med linjeskift  to til og med"
            it.aktør shouldBe Historikkinnslag.Aktør.SAKSBEHANDLER
            it.opprettetAv shouldBe SAKSBEHANDLER_IDENT
        }
    }
}
