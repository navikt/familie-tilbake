package no.nav.tilbakekreving.e2e.ytelser

import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.familie.tilbake.api.FagsakController
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class DagpengerE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Autowired
    lateinit var fagsakController: FagsakController

    @Test
    fun `kan hente ut fagsak basert på tilbakekrevingId i stedet for ekstern fagsakId`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            kravgrunnlag = KravgrunnlagGenerator.forDP(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Dagpenger, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))
        val tilbakekreving = tilbakekreving(FagsystemDTO.DP, fagsystemId).shouldNotBeNull()

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            fagsakController.hentFagsak(FagsystemDTO.DP, tilbakekreving.id)
        }
    }
}
