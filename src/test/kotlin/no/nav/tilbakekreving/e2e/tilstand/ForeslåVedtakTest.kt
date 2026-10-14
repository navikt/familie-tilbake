package no.nav.tilbakekreving.e2e.tilstand

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.e2e.BehandlingsstegGenerator
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.e2e.kanBehandle
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ForeslåVedtakTest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Test
    fun `foreslå vedtak er stanset midlertidig`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))

        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        lagreUttalelse(behandlingId)

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }

        utførSteg(behandlingId, BehandlingsstegGenerator.lagIkkeForeldetVurdering())
        utførSteg(behandlingId, BehandlingsstegGenerator.lagVilkårsvurderingFullTilbakekreving())

        tilbakekreving(behandlingId) kanBehandle Behandlingssteg.FORESLÅ_VEDTAK

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            val exception = shouldThrow<ModellFeil.TjenesteUtilgjengeligException> {
                behandlingApiController.behandlingForeslaaVedtak(behandlingId)
            }

            exception.tittel shouldBe "Fryseperiode 9. oktober kl. 16:00–19. oktober kl. 08:00"
            exception.melding shouldBe "Skatteetaten avvikler PAK og migrerer til Innfri. I denne perioden er det ikke mulig å sende vedtak til beslutter i Tilbakeløsningen."
            exception.sporing shouldBe Sporing("Ukjent", behandlingId.toString())

            val stegException = shouldThrow<ModellFeil.TjenesteUtilgjengeligException> {
                behandlingController.utførBehandlingssteg(
                    behandlingId,
                    BehandlingsstegGenerator.lagForeslåVedtakVurdering(),
                )
            }

            stegException.tittel shouldBe "Fryseperiode 9. oktober kl. 16:00–19. oktober kl. 08:00"
            stegException.melding shouldBe "Skatteetaten avvikler PAK og migrerer til Innfri. I denne perioden er det ikke mulig å sende vedtak til beslutter i Tilbakeløsningen."
        }
    }
}
