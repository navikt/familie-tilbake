package no.nav.tilbakekreving

import no.nav.tilbakekreving.ModellTestdata.forårsaketAvBruker
import no.nav.tilbakekreving.behandling.BegrunnelseForUnntak
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.OpprettTilbakekrevingHendelse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.util.UUID

fun opprettTilbakekreving(
    opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse,
    context: SideeffektContext = systemContext(),
) = Tilbakekreving
    .opprett(
        id = UUID.randomUUID().toString(),
        opprettTilbakekrevingEvent = opprettTilbakekrevingHendelse,
        sideeffektContext = context,
    )

fun tilbakekrevingTilBehandling(
    opprettTilbakekrevingHendelse: OpprettTilbakekrevingHendelse = opprettTilbakekrevingHendelse(),
    kravgrunnlag: KravgrunnlagHendelse = kravgrunnlag(),
    context: SideeffektContext = systemContext(),
): Tilbakekreving {
    val tilbakekreving = opprettTilbakekreving(opprettTilbakekrevingHendelse, context)
    tilbakekreving.apply {
        håndter(kravgrunnlag, context)
        håndter(fagsysteminfoHendelse(), context)
        håndter(brukerinfoHendelse(), context)
    }
    return tilbakekreving
}

fun Tilbakekreving.vurderAlleSteg(context: SideeffektContext, vararg perioder: Datoperiode) {
    gjørSaksbehandling(nåværendeBehandlingId(), context) {
        vurderFakta(faktastegVurdering(perioder = perioder.toList()))
        lagreForhåndsvarselUnntak(BegrunnelseForUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING, "")
        perioder.forEach { periode ->
            vurderForeldelse(periode, foreldelseVurdering())
        }
        perioder.forEach { periode ->
            vurderVilkår(periode, forårsaketAvBruker().uaktsomt())
        }
    }
}

fun Tilbakekreving.fatteVedtak() {
    gjørSaksbehandling(
        behandlingId = nåværendeBehandlingId(),
        sideeffektContext = beslutterContext(),
    ) {
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
