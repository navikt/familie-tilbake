package no.nav.tilbakekreving.e2e.brev

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.e2e.BehandlingsstegGenerator
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.entities.Brevtype
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class VedtaksbrevE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    private val ansvarligSaksbehandler = "Z999999"
    private val ansvarligBeslutter = "Z111111"

    @Test
    fun `vedtaksbrev sendes og lagres riktig i DB`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))

        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        lagreUttalelse(behandlingId)

        somSaksbehandler(ansvarligSaksbehandler) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }
        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(),
        )
        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingFullTilbakekreving(),
        )
        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagForeslåVedtakVurdering(),
        )
        utførSteg(
            ident = ansvarligBeslutter,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagGodkjennVedtakVurdering(),
        )

        tilbakekrevingService.hentTilbakekreving(behandlingId).shouldNotBeNull {
            brevHistorikk.nåværende().entry.brevtype shouldBe Brevtype.VEDTAKSBREV
        }
    }
}
