package no.nav.tilbakekreving.e2e

import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotHaveSize
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvBruker
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behandling.saksbehandling.Foreldelsesteg
import no.nav.tilbakekreving.behov.BehovObservatørOppsamler
import no.nav.tilbakekreving.bigquery.BigQueryServiceStub
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.endring.EndringObservatørOppsamler
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.skalUnnlates
import org.junit.jupiter.api.Test
import java.util.UUID

class Behandlingslogg {
    @Test
    fun `lagring av behandlingslogg i historikken`() {
        val opprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse()
        val behandler = Behandler.Saksbehandler("Ansvarlig saksbehandler")

        val tilbakekreving = Tilbakekreving.opprett(
            UUID.randomUUID().toString(),
            BehovObservatørOppsamler(),
            opprettTilbakekrevingHendelse,
            BigQueryServiceStub(),
            EndringObservatørOppsamler(),
            features = defaultFeatures(),
        )

        tilbakekreving.håndter(
            kravgrunnlag(
                perioder = listOf(
                    kravgrunnlagPeriode(3.januar(2021) til 3.januar(2021)),
                    kravgrunnlagPeriode(1.februar(2021) til 1.februar(2021)),
                ),
            ),
        )
        tilbakekreving.håndter(
            fagsysteminfoHendelse(
                utvidPerioder = listOf(
                    FagsysteminfoHendelse.UtvidetPeriode(
                        kravgrunnlagPeriode = 3.januar(2021) til 3.januar(2021),
                        vedtaksperiode = 1.januar(2021) til 31.januar(2021),
                    ),
                    FagsysteminfoHendelse.UtvidetPeriode(
                        kravgrunnlagPeriode = 1.februar(2021) til 1.februar(2021),
                        vedtaksperiode = 1.februar(2021) til 28.februar(2021),
                    ),
                ),
            ),
        )
        tilbakekreving.håndter(brukerinfoHendelse())
        tilbakekreving.behandlingHistorikk.nåværende().entry.lagreUttalelse(UttalelseVurdering.JA, listOf(), "")

        tilbakekreving.håndter(behandler, faktastegVurdering(1.januar(2021) til 31.januar(2021)))
        tilbakekreving.håndter(behandler, faktastegVurdering(1.februar(2021) til 28.februar(2021)))
        tilbakekreving.håndter(behandler, 1.januar(2021) til 31.januar(2021), Foreldelsesteg.Vurdering.IkkeForeldet(""))
        tilbakekreving.håndter(behandler, 1.februar(2021) til 28.februar(2021), Foreldelsesteg.Vurdering.IkkeForeldet(""))
        tilbakekreving.håndter(
            behandler,
            1.januar(2021) til 31.januar(2021),
            forårsaketAvBruker().uaktsomt(unnlates = skalUnnlates()),
        )

        tilbakekreving.behandlingslogg.tilFrontend().shouldNotHaveSize(0)
        tilbakekreving.behandlingslogg.tilFrontend().map { it.tittel } shouldContainAll listOf(
            "Behandling opprettet",
            "Fagsysteminfo innhentet",
            "Brukerinfo innhentet",
            "Fakta vurdert",
            "Foreldelse vurdert",
            "Vilkår vurdert",
        )
    }
}
