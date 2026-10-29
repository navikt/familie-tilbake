package no.nav.tilbakekreving.e2e

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.api.VedtakDokumentController
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksdokumentDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

class VedtakDokumentE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var vedtakDokumentController: VedtakDokumentController

    @Test
    @Disabled("Midlertidig deaktivert etter at særhåndtering av foreslå/fatte vedtak ble fjernet fra TilbakekrevingE2EBase")
    fun `vedtaksdokumenter for en ferdigbehandlet tilbakekreving i ny modell`() {
        val vedtakId = KravgrunnlagGenerator.nextPaddedId(6)
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        val periode = 1.januar(2021) til 1.januar(2021)
        sendKravgrunnlagOgAvventLesing(
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                vedtakId = vedtakId,
                fagsystemId = fagsystemId,
                perioder = listOf(KravgrunnlagGenerator.standardPeriode(periode)),
            ),
        )
        fagsystemIntegrasjonService.håndter(
            Ytelse.Tilleggsstønad,
            Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()),
        )
        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        lagreUttalelse(behandlingId)

        tilbakekrevVedtak(behandlingId, listOf(periode))

        val response = ContextServiceHelpers.somSaksbehandler {
            vedtakDokumentController.dokumenterHentVedtaksdokumenter(vedtakId)
        }

        response.statusCode shouldBe HttpStatus.OK
        // DokarkivClientStub returnerer disse referansene ved normal journalføring.
        response.body.shouldNotBeNull() shouldContainExactly listOf(VedtaksdokumentDto("-1", "-2"))
    }
}
