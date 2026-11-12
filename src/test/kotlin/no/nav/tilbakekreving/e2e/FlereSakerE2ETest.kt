package no.nav.tilbakekreving.e2e

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.api.FagsakController
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FlereSakerE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsakController: FagsakController

    @Test
    fun `kan hente fagsak med bortfalt og ny tilbakekreving`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        val bortfaltVedtakId = KravgrunnlagGenerator.nextPaddedId(6)

        sendKravgrunnlagOgAvventLesing(KravgrunnlagGenerator.forTilleggsstønader(fagsystemId = fagsystemId, vedtakId = bortfaltVedtakId))
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))
        val bortfaltBehandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()

        sendStatusmelding(
            statusmelding = StatusmeldingGenerator.forFagområdet(
                vedtakId = bortfaltVedtakId,
                fagsystemId = fagsystemId,
                kodeStatusKrav = "AVSL",
                fagområde = "TILLST",
            ),
        )

        sendKravgrunnlagOgAvventLesing(KravgrunnlagGenerator.forTilleggsstønader(fagsystemId = fagsystemId))
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))
        val nyBehandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()

        val fagsak = somSaksbehandler(SAKSBEHANDLER_IDENT) {
            fagsakController.hentFagsak(FagsystemDTO.TS, fagsystemId)
        }.data.shouldNotBeNull()

        fagsak.eksternFagsakId shouldBe fagsystemId
        fagsak.behandlinger shouldHaveSize 2
        fagsak.behandlinger[0].behandlingId shouldBe bortfaltBehandlingId
        fagsak.behandlinger[0].status shouldBe Behandlingsstatus.UTREDES
        fagsak.behandlinger[1].behandlingId shouldBe nyBehandlingId
        fagsak.behandlinger[1].status shouldBe Behandlingsstatus.UTREDES
    }
}
