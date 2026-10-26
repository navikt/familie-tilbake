package no.nav.tilbakekreving.e2e

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource

@TestPropertySource(
    properties = ["tilbakekreving.toggles.nyModell.SperretStatusmelding=false"],
)
class SperretStatusmeldingUtenforScopeE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Test
    fun `sak får statusmelding med sperret`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)

        sendKravgrunnlagOgAvventLesing(KravgrunnlagGenerator.forTilleggsstønader(fagsystemId = fagsystemId))
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))
        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()

        sendStatusmelding(statusmelding = StatusmeldingGenerator.forFagområdet(fagsystemId = fagsystemId, kodeStatusKrav = "SPER", fagområde = "TILLST"))

        val exception = shouldThrow<ModellFeil.UtenforScopeException> {
            somSaksbehandler(SAKSBEHANDLER_IDENT) {
                behandlingController.hentBehandling(behandlingId)
            }
        }
        exception.utenforScope shouldBe UtenforScope.KravgrunnlagSperret
    }
}
