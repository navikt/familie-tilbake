package no.nav.tilbakekreving.e2e
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forOne
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.KlokkeStub
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvBruker
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.BegrunnelseForUnntak
import no.nav.tilbakekreving.behandlingslogg.Behandlingslogg
import no.nav.tilbakekreving.beslutterContext
import no.nav.tilbakekreving.brukerinfoHendelse
import no.nav.tilbakekreving.fagsysteminfoHendelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.godkjenning
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlagPeriode
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.systemContext
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class AutomatiskForeldelseE2ETest {
    @Test
    fun `foreldelse vurderes automatisk når perioden er innenfor 30 måneder`() {
        val fom = 1.januar(2025)
        val periode = fom til 31.januar(2025)
        val klokke = KlokkeStub(fom.plusMonths(10))

        val tilbakekreving = opprettTilbakekrevingMedKravgrunnlag(periode, klokke)
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            vurderFakta(faktastegVurdering(periode))
            lagreForhåndsvarselUnntak(
                begrunnelseForUnntak = BegrunnelseForUnntak.IKKE_PRAKTISK_MULIG,
                beskrivelse = "Ikke praktisk mulig",
            )
        }

        foreldelsePerioderFor(tilbakekreving)
            .forAll { it.foreldelsesvurderingstype shouldBe Foreldelsesvurderingstype.AUTOMATISK_VURDERT_IKKE_FORELDET }
    }

    @Test
    fun `foreldelse vurderes ikke automatisk når perioden er eldre enn 30 måneder`() {
        val fom = 1.januar(2025)
        val periode = fom til 31.januar(2025)
        val klokke = KlokkeStub(fom.plusMonths(31))

        val tilbakekreving = opprettTilbakekrevingMedKravgrunnlag(periode, klokke)
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(klokke = klokke)) {
            vurderFakta(faktastegVurdering(periode))
            lagreForhåndsvarselUnntak(
                begrunnelseForUnntak = BegrunnelseForUnntak.IKKE_PRAKTISK_MULIG,
                beskrivelse = "Ikke praktisk mulig",
            )
        }

        foreldelsePerioderFor(tilbakekreving)
            .forAll { it.foreldelsesvurderingstype shouldBe Foreldelsesvurderingstype.IKKE_VURDERT }
    }

    @Test
    fun `automatisk foreldelse endres ikke etter ForeslåVedtakSteg er fullstendig`() {
        val fom = 1.januar(2025)
        val periode = fom til 31.januar(2025)
        val klokke = KlokkeStub(fom.plusMonths(10))

        val tilbakekreving = opprettTilbakekrevingMedKravgrunnlag(periode, klokke)
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            vurderFakta(faktastegVurdering(periode))
            lagreForhåndsvarselUnntak(
                begrunnelseForUnntak = BegrunnelseForUnntak.IKKE_PRAKTISK_MULIG,
                beskrivelse = "Ikke praktisk mulig",
            )
        }
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            vurderVilkår(periode, forårsaketAvBruker().uaktsomt())
            foreslåVedtak()

            klokke.settTid(fom.plusMonths(31))
        }
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), beslutterContext()) {
            fatteVedtak(godkjenning())
        }

        foreldelsePerioderFor(tilbakekreving)
            .forAll { it.foreldelsesvurderingstype shouldBe Foreldelsesvurderingstype.AUTOMATISK_VURDERT_IKKE_FORELDET }
    }

    @Test
    fun `påminnelse tilbakestiller til IkkeVurdert når automatiseringsregler ikke lenger gjelder`() {
        val fom = 1.januar(2025)
        val periode = fom til 31.januar(2025)
        val klokke = KlokkeStub(fom.plusMonths(10))

        val tilbakekreving = opprettTilbakekrevingMedKravgrunnlag(periode, klokke)
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            vurderFakta(faktastegVurdering(periode))
            lagreForhåndsvarselUnntak(
                begrunnelseForUnntak = BegrunnelseForUnntak.IKKE_PRAKTISK_MULIG,
                beskrivelse = "Ikke praktisk mulig",
            )
        }

        foreldelsePerioderFor(tilbakekreving)
            .forAll { it.foreldelsesvurderingstype shouldBe Foreldelsesvurderingstype.AUTOMATISK_VURDERT_IKKE_FORELDET }

        klokke.settTid(fom.plusMonths(31))
        tilbakekreving.håndter(Påminnelse(klokke.nå()), systemContext(klokke = klokke))

        foreldelsePerioderFor(tilbakekreving)
            .forAll { it.foreldelsesvurderingstype shouldBe Foreldelsesvurderingstype.IKKE_VURDERT }
    }

    @Test
    fun `påminnelse tilbakestiller ikke til IkkeVurdert når automatiseringsregler ikke lenger gjelder`() {
        val fom = 1.januar(2025)
        val periode = fom til 31.januar(2025)
        val klokke = KlokkeStub(fom.plusMonths(10))

        val tilbakekreving = opprettTilbakekrevingMedKravgrunnlag(periode, klokke)
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            vurderFakta(faktastegVurdering(periode))
            lagreForhåndsvarselUnntak(
                begrunnelseForUnntak = BegrunnelseForUnntak.IKKE_PRAKTISK_MULIG,
                beskrivelse = "Ikke praktisk mulig",
            )
        }
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            vurderVilkår(periode, forårsaketAvBruker().uaktsomt())
            foreslåVedtak()
        }

        klokke.settTid(fom.plusMonths(31))
        tilbakekreving.håndter(Påminnelse(klokke.nå()), systemContext())

        foreldelsePerioderFor(tilbakekreving)
            .forAll { it.foreldelsesvurderingstype shouldBe Foreldelsesvurderingstype.AUTOMATISK_VURDERT_IKKE_FORELDET }
    }

    @Test
    fun `godkjenning tilbakestiller ikke til IkkeVurdert når automatiseringsregler ikke lenger gjelder`() {
        val fom = 1.januar(2025)
        val periode = fom til 31.januar(2025)
        val klokke = KlokkeStub(fom.plusMonths(10))

        val tilbakekreving = opprettTilbakekrevingMedKravgrunnlag(periode, klokke)
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            vurderFakta(faktastegVurdering(periode))
            lagreForhåndsvarselUnntak(
                begrunnelseForUnntak = BegrunnelseForUnntak.IKKE_PRAKTISK_MULIG,
                beskrivelse = "Ikke praktisk mulig",
            )
        }
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            vurderVilkår(periode, forårsaketAvBruker().uaktsomt())
            foreslåVedtak()

            klokke.settTid(fom.plusMonths(31))
        }
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), beslutterContext()) {
            fatteVedtak(godkjenning())
        }

        foreldelsePerioderFor(tilbakekreving)
            .forAll { it.foreldelsesvurderingstype shouldBe Foreldelsesvurderingstype.AUTOMATISK_VURDERT_IKKE_FORELDET }
    }

    @Test
    fun `behandlingshistorikk får logginnslag når foreldelse vurderes automatisk`() {
        val fom = 1.januar(2025)
        val periode = fom til 31.januar(2025)
        val klokke = KlokkeStub(fom.plusMonths(10))
        val behandlingslogg = Behandlingslogg(mutableListOf())

        val tilbakekreving = opprettTilbakekrevingMedKravgrunnlag(periode, klokke)
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext(behandlingslogg = behandlingslogg)) {
            vurderFakta(faktastegVurdering(periode))
            lagreForhåndsvarselUnntak(
                begrunnelseForUnntak = BegrunnelseForUnntak.IKKE_PRAKTISK_MULIG,
                beskrivelse = "Ikke praktisk mulig",
            )
        }

        behandlingslogg.tilFrontend()
            .forOne { it.tittel shouldBe "Foreldelse automatisk vurdert" }
    }

    @Test
    fun `behandlingshistorikk får bare et logginnslag når foreldelse vurderes automatisk og blir påminnet`() {
        val fom = 1.januar(2025)
        val periode = fom til 31.januar(2025)
        val klokke = KlokkeStub(fom.plusMonths(10))
        val behandlingslogg = Behandlingslogg(mutableListOf())

        val tilbakekreving = opprettTilbakekrevingMedKravgrunnlag(periode, klokke)
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            vurderFakta(faktastegVurdering(periode))
            lagreForhåndsvarselUnntak(
                begrunnelseForUnntak = BegrunnelseForUnntak.IKKE_PRAKTISK_MULIG,
                beskrivelse = "Ikke praktisk mulig",
            )
        }
        tilbakekreving.håndter(Påminnelse(LocalDateTime.now()), systemContext(behandlingslogg = behandlingslogg))

        behandlingslogg.tilFrontend()
            .forOne { it.tittel shouldBe "Foreldelse automatisk vurdert" }
    }

    @Test
    fun `behandlingshistorikk får logginnslag når automatisk foreldelse tilbakestilles`() {
        val fom = 1.januar(2025)
        val periode = fom til 31.januar(2025)
        val klokke = KlokkeStub(fom.plusMonths(10))
        val behandlingslogg = Behandlingslogg(mutableListOf())

        val tilbakekreving = opprettTilbakekrevingMedKravgrunnlag(periode, klokke)
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            vurderFakta(faktastegVurdering(periode))
            lagreForhåndsvarselUnntak(
                begrunnelseForUnntak = BegrunnelseForUnntak.IKKE_PRAKTISK_MULIG,
                beskrivelse = "Ikke praktisk mulig",
            )
        }

        klokke.settTid(fom.plusMonths(31))
        tilbakekreving.håndter(Påminnelse(klokke.nå()), systemContext(klokke = klokke, behandlingslogg = behandlingslogg))

        behandlingslogg.tilFrontend()
            .forOne { it.tittel shouldBe "Automatisk vurdering av foreldelse er fjernet" }
    }

    @Test
    fun `behandlingshistorikk får bare et logginnslag når automatisk foreldelse tilbakestilles og blir påminnet`() {
        val fom = 1.januar(2025)
        val periode = fom til 31.januar(2025)
        val klokke = KlokkeStub(fom.plusMonths(10))
        val behandlingslogg = Behandlingslogg(mutableListOf())

        val tilbakekreving = opprettTilbakekrevingMedKravgrunnlag(periode, klokke)
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), saksbehandlerContext()) {
            vurderFakta(faktastegVurdering(periode))
            lagreForhåndsvarselUnntak(
                begrunnelseForUnntak = BegrunnelseForUnntak.IKKE_PRAKTISK_MULIG,
                beskrivelse = "Ikke praktisk mulig",
            )
        }

        klokke.settTid(fom.plusMonths(31))
        tilbakekreving.håndter(Påminnelse(klokke.nå()), systemContext(klokke = klokke, behandlingslogg = behandlingslogg))
        tilbakekreving.håndter(Påminnelse(klokke.nå()), systemContext(klokke = klokke, behandlingslogg = behandlingslogg))

        behandlingslogg.tilFrontend()
            .forOne { it.tittel shouldBe "Automatisk vurdering av foreldelse er fjernet" }
    }

    private fun opprettTilbakekrevingMedKravgrunnlag(periode: Datoperiode, klokke: KlokkeStub): Tilbakekreving {
        val tilbakekreving = Tilbakekreving.opprett(
            id = UUID.randomUUID().toString(),
            opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse(),
            sideeffektContext = systemContext(klokke = klokke),
        )
        tilbakekreving.håndter(kravgrunnlag(perioder = listOf(kravgrunnlagPeriode(periode))), systemContext(klokke = klokke))
        tilbakekreving.håndter(fagsysteminfoHendelse(), systemContext(klokke = klokke))
        tilbakekreving.håndter(brukerinfoHendelse(), systemContext(klokke = klokke))
        return tilbakekreving
    }

    private fun foreldelsePerioderFor(tilbakekreving: Tilbakekreving) =
        tilbakekreving.hentBehandling(tilbakekreving.nåværendeBehandlingId())
            .foreldelsestegDto.tilFrontendDto(saksbehandlerContext()).foreldetPerioder
}
