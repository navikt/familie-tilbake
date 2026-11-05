package no.nav.tilbakekreving.behandling.revurdering

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvNav
import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v1.dto.BehandlerRolle
import no.nav.tilbakekreving.behandling.BegrunnelseForUnntak
import no.nav.tilbakekreving.behandling.TilbakekrevingRevurderingsårsak
import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.behandling.VedtakOmgjortAv
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.beslutterContext
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.foreldelseVurdering
import no.nav.tilbakekreving.hendelse.DistribusjonHendelse
import no.nav.tilbakekreving.hendelse.IverksettelseHendelse
import no.nav.tilbakekreving.hendelse.JournalføringHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.frontend.models.TilbakekrevingRevurderingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UtenKlageDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.nåværendeBehandlingId
import no.nav.tilbakekreving.opprettTilbakekrevingHendelse
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.test.ingenReduksjon
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.skalIkkeUnnlates
import no.nav.tilbakekreving.test.uaktsomt
import no.nav.tilbakekreving.tilbakekrevingTilBehandling
import org.junit.jupiter.api.Test
import java.math.BigInteger
import java.util.UUID

class RevurderingTest {
    @Test
    fun `Revurdering opprettes riktig for avsluttet sak`() {
        val context = saksbehandlerContext()
        val tilbakekreving = tilbakekrevingTilAvsluttet(opprettTilbakekrevingHendelse(), context)
        val behandling = tilbakekreving.hentBehandling(tilbakekreving.nåværendeBehandlingId())
        val opprinneligFakta = tilbakekreving.tilFeilutbetalingFrontendDto(behandling.id, context.klokke)
        val opprinneligForhåndsvarsel = tilbakekreving.nyHentForhåndsvarselFrontendDto(behandling.id, context.klokke)
        val opprinneligForeldelse = behandling.foreldelsestegDto.tilFrontendDto(context)
        val opprinneligVilkår = behandling.vilkårsvurderingDto(context)

        tilbakekreving.tilstand.behandlingsstatus(behandling, context.klokke) shouldBe BehandlingsstatusModell.AVSLUTTET

        tilbakekreving.opprettRevurdering(
            tilbakekreving.nåværendeBehandlingId(),
            TilbakekrevingRevurderingsårsak.EtterKlage(
                vedtakOmgjortAv = VedtakOmgjortAv.Klageinstansen,
                beskrivelse = "Begrunnelse fra klageinstansen",
            ),
            saksbehandlerContext(),
        )

        val revurderingBehandling = tilbakekreving.hentBehandling(tilbakekreving.nåværendeBehandlingId())

        val revurderingFakta = tilbakekreving.tilFeilutbetalingFrontendDto(revurderingBehandling.id, context.klokke)
        val revurderingForhåndsvarsel = tilbakekreving.nyHentForhåndsvarselFrontendDto(revurderingBehandling.id, context.klokke)
        val revurderingForeldelse = revurderingBehandling.foreldelsestegDto.tilFrontendDto(context)
        val revurderingVilkår = revurderingBehandling.vilkårsvurderingDto(context)

        revurderingBehandling.id shouldNotBe behandling.id

        revurderingFakta shouldNotBeNull {
            feilutbetaling shouldBe opprinneligFakta.feilutbetaling
            tidligereVarsletBeløp shouldBe opprinneligFakta.tidligereVarsletBeløp
            muligeRettsligGrunnlag shouldBe opprinneligFakta.muligeRettsligGrunnlag
            perioder shouldNotBeNull {
                size shouldBe 1
                first().fom shouldBe opprinneligFakta.perioder.first().fom
                first().tom shouldBe opprinneligFakta.perioder.first().tom
                first().feilutbetaltBeløp shouldBe opprinneligFakta.perioder.first().feilutbetaltBeløp
                first().splittbarePerioder shouldBe opprinneligFakta.perioder.first().splittbarePerioder
                first().rettsligGrunnlag shouldBe opprinneligFakta.perioder.first().rettsligGrunnlag
                first().endringIKravgrunnlag shouldBe opprinneligFakta.perioder.first().endringIKravgrunnlag
            }
            vurdering shouldBe opprinneligFakta.vurdering
            rettsgebyrÅrFraSaksbehandler shouldBe opprinneligFakta.rettsgebyrÅrFraSaksbehandler
            status4xRettsgebyret shouldBe opprinneligFakta.status4xRettsgebyret
            tilbakeført shouldBe null
        }

        revurderingForhåndsvarsel shouldNotBeNull {
            forhaandsvarselSteg shouldBe opprinneligForhåndsvarsel.forhaandsvarselSteg
            brukeruttalelse shouldBe opprinneligForhåndsvarsel.brukeruttalelse
            sendtVarselbrev shouldBe opprinneligForhåndsvarsel.sendtVarselbrev
            uttalelsesfrist shouldBe opprinneligForhåndsvarsel.uttalelsesfrist
            tilbakeført shouldBe null
        }

        revurderingForeldelse shouldNotBeNull {
            foreldetPerioder shouldBe opprinneligForeldelse.foreldetPerioder
        }

        revurderingVilkår shouldNotBeNull {
            vilkårsperioder.size shouldBe opprinneligVilkår.vilkårsperioder.size
            vilkårsperioder.first().vilkårsvurdering.fom shouldBe opprinneligVilkår.vilkårsperioder.first().vilkårsvurdering.fom
            vilkårsperioder.first().vilkårsvurdering.tom shouldBe opprinneligVilkår.vilkårsperioder.first().vilkårsvurdering.tom
            vilkårsperioder.first().feilutbetaltBeløp shouldBe opprinneligVilkår.vilkårsperioder.first().feilutbetaltBeløp
            vilkårsperioder.first().vilkårsvurdering.valg shouldBe opprinneligVilkår.vilkårsperioder.first().vilkårsvurdering.valg
            tilbakeført shouldBe null
        }

        tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), context, true, BehandlerRolle.SAKSBEHANDLER) shouldNotBeNull {
            type shouldBe Behandlingstype.REVURDERING_TILBAKEKREVING
        }
    }

    @Test
    fun `opprettelse av revurdering for ikke avsluttet sak skal feile`() {
        val context = saksbehandlerContext()
        val tilbakekreving = tilbakekrevingTilBehandling(opprettTilbakekrevingHendelse())
        tilbakekreving.gjørSaksbehandling(tilbakekreving.nåværendeBehandlingId(), context) {
            vurderFakta(faktastegVurdering())
        }
        val behandling = tilbakekreving.hentBehandling(tilbakekreving.nåværendeBehandlingId())

        tilbakekreving.tilstand.behandlingsstatus(behandling, context.klokke) shouldBe BehandlingsstatusModell.TIL_FORHÅNDSVARSEL
        tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), context, true, BehandlerRolle.SAKSBEHANDLER).kanRevurderingOpprettes shouldBe false
        shouldThrow<ModellFeil.UgyldigOperasjonException> {
            tilbakekreving.opprettRevurdering(
                tilbakekreving.nåværendeBehandlingId(),
                TilbakekrevingRevurderingsårsak.EtterKlage(
                    vedtakOmgjortAv = VedtakOmgjortAv.Klageinstansen,
                    beskrivelse = "Begrunnelse fra klageinstansen",
                ),
                saksbehandlerContext(),
            )
        }.message shouldBe "Kan ikke opprette revurdering i TIL_BEHANDLING"
    }

    private fun tilbakekrevingTilAvsluttet(
        opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
        saksbehandlerContext: SideeffektContext,
    ): Tilbakekreving {
        val tilbakekreving = tilbakekrevingTilBehandling(opprettTilbakekrevingHendelse).apply {
            gjørSaksbehandling(nåværendeBehandlingId(), saksbehandlerContext) {
                lagreForhåndsvarselUnntak(BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG, "Forhåndsvarsel er ikke nødvendig i testen")
                lagreUttalelse(UttalelseVurdering.NEI, null, "")
                vurderFakta(faktastegVurdering())
                vurderForeldelse(1.januar(2021) til 31.januar(2021), foreldelseVurdering())
                vurderVilkår(
                    periode = 1.januar(2021) til 31.januar(2021),
                    vurdering = forårsaketAvNav().burdeForstått(aktsomhet = uaktsomt(skalIkkeUnnlates(), ingenReduksjon())),
                )
                foreslåVedtak()
            }
        }
        tilbakekreving.apply {
            gjørSaksbehandling(nåværendeBehandlingId(), beslutterContext()) {
                fatteVedtak(
                    listOf(
                        Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Godkjent,
                        Behandlingssteg.FORHÅNDSVARSEL to FatteVedtakSteg.Vurdering.Godkjent,
                        Behandlingssteg.FORELDELSE to FatteVedtakSteg.Vurdering.Godkjent,
                        Behandlingssteg.VILKÅRSVURDERING to FatteVedtakSteg.Vurdering.Godkjent,
                        Behandlingssteg.FORESLÅ_VEDTAK to FatteVedtakSteg.Vurdering.Godkjent,
                    ),
                )
            }
        }
        tilbakekreving.håndter(
            IverksettelseHendelse(
                iverksattVedtakId = UUID.randomUUID(),
                behandlingId = tilbakekreving.nåværendeBehandlingId(),
                vedtakId = BigInteger.valueOf(1L),
            ),
            saksbehandlerContext,
        )

        val brevId = tilbakekreving.brevHistorikk.sisteVedtaksbrev()!!.id
        tilbakekreving.håndter(
            JournalføringHendelse(
                brevId = brevId,
                behandlingId = tilbakekreving.nåværendeBehandlingId(),
                journalpostId = "",
                fagsakId = "",
                dokumentInfoId = "",
            ),
            saksbehandlerContext,
        )

        tilbakekreving.håndter(
            DistribusjonHendelse(
                brevId = brevId,
                behandlingId = tilbakekreving.nåværendeBehandlingId(),
                journalpostId = "",
                fagsakId = "",
                dokumentInfoId = "",
            ),
            saksbehandlerContext,
        )

        return tilbakekreving
    }

    @Test
    fun `oppdaterer revurderingsårsak for revurdering`() {
        val context = saksbehandlerContext()
        val tilbakekreving = tilbakekrevingTilAvsluttet(opprettTilbakekrevingHendelse(), context)
        tilbakekreving.opprettRevurdering(
            tilbakekreving.nåværendeBehandlingId(),
            TilbakekrevingRevurderingsårsak.EtterKlage(
                vedtakOmgjortAv = VedtakOmgjortAv.Klageinstansen,
                beskrivelse = "Begrunnelse fra klageinstansen",
            ),
            saksbehandlerContext(),
        )

        tilbakekreving.oppdaterRevurderingsårsak(
            tilbakekreving.nåværendeBehandlingId(),
            TilbakekrevingRevurderingsårsak.UtenKlage(
                vedtakOmgjortAv = VedtakOmgjortAv.Førsteinstansen,
                beskrivelse = "Begrunnelse fra Førsteinstansen",
            ),
            context,
        )

        tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), context, true, BehandlerRolle.SAKSBEHANDLER) shouldNotBeNull {
            type shouldBe Behandlingstype.REVURDERING_TILBAKEKREVING
            tilbakekrevingRevurdering shouldBe TilbakekrevingRevurderingDto(
                revurderingsarsak = UtenKlageDto(
                    instans = UtenKlageDto.Instans.Førsteinstansen,
                    begrunnelse = "Begrunnelse fra Førsteinstansen",
                ),
            )
        }
    }

    @Test
    fun `kan ikke oppdaterer revurderingsårsak når det ikke er revurdering`() {
        val context = saksbehandlerContext()
        val tilbakekreving = tilbakekrevingTilAvsluttet(opprettTilbakekrevingHendelse(), context)

        tilbakekreving.frontendDtoForBehandling(tilbakekreving.nåværendeBehandlingId(), context, true, BehandlerRolle.SAKSBEHANDLER) shouldNotBeNull {
            type shouldBe Behandlingstype.TILBAKEKREVING
        }
        shouldThrow<ModellFeil.UgyldigOperasjonException> {
            tilbakekreving.oppdaterRevurderingsårsak(
                tilbakekreving.nåværendeBehandlingId(),
                TilbakekrevingRevurderingsårsak.UtenKlage(
                    vedtakOmgjortAv = VedtakOmgjortAv.Førsteinstansen,
                    beskrivelse = "Begrunnelse fra Førsteinstansen",
                ),
                context,
            )
        }
    }
}
