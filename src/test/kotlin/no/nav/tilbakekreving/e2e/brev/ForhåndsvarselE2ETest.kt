package no.nav.tilbakekreving.e2e.brev

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.api.DokumentController
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.api.v1.dto.BestillBrevDto
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.brev.Dokumentmalstype
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ForhåndsvarselE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Autowired
    private lateinit var documentController: DokumentController

    @Test
    fun `forhåndsvarsel sendes og lagres riktig i DB`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))
        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        val tilbakekrevingFørForhåndsvarsel = tilbakekreving(behandlingId)
        documentController.bestillBrev(
            BestillBrevDto(
                behandlingId = behandlingId,
                brevmalkode = Dokumentmalstype.VARSEL,
                fritekst = "Tekst fra saksbehandler",
            ),
        )
        val tilbakekrevingEtterForhåndsvarsel = tilbakekreving(behandlingId)

        tilbakekrevingFørForhåndsvarsel.brevHistorikk.sisteVarselbrev().shouldBeNull()
        tilbakekrevingEtterForhåndsvarsel.brevHistorikk.sisteVarselbrev().shouldNotBeNull {
            tekstFraSaksbehandler shouldBe "Tekst fra saksbehandler"
        }
    }
}
