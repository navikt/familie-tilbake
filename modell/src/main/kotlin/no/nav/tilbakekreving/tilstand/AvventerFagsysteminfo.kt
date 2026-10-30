package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.Toggle
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.hendelse.AvsluttetStatusmeldingHendelse
import no.nav.tilbakekreving.hendelse.EndretStatusmeldingHendelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.hendelse.SperretStatusmeldingHendelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object AvventerFagsysteminfo : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofHours(3)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.AVVENTER_FAGSYSTEMINFO

    override fun behandlingsstatus(behandling: Behandling, klokke: Klokke): BehandlingsstatusModell = BehandlingsstatusModell.OPPRETTET

    override fun entering(tilbakekreving: Tilbakekreving, sideeffektContext: SideeffektContext) {
        tilbakekreving.trengerFagsysteminfo(sideeffektContext)
        if (!tilbakekreving.eksternFagsak.ytelse.integrererMotFagsystem()) {
            tilbakekreving.opprettBehandlingUtenIntegrasjon(sideeffektContext)
        }
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse, sideeffektContext: SideeffektContext) {
        tilbakekreving.trengerFagsysteminfo(sideeffektContext)
    }

    override fun håndter(tilbakekreving: Tilbakekreving, kravgrunnlag: KravgrunnlagHendelse, sideeffektContext: SideeffektContext) {
        tilbakekreving.kravgrunnlagHistorikk.lagre(kravgrunnlag)
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        fagsysteminfo: FagsysteminfoHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        val eksternBehandling = tilbakekreving.eksternFagsak.lagre(fagsysteminfo, sideeffektContext.klokke)
        tilbakekreving.opprettBehandling(eksternBehandling, sideeffektContext, fagsysteminfo.behandlendeEnhet, Behandlingstype.TILBAKEKREVING)
        if (fagsysteminfo.aktør != null) {
            tilbakekreving.opprettBruker(fagsysteminfo.aktør)
        }
        tilbakekreving.byttTilstand(AvventerBrukerinfo, sideeffektContext)
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        statusmeldingHendelse: AvsluttetStatusmeldingHendelse,
        sideeffektContext: SideeffektContext,
    ) {
        if (sideeffektContext.features[Toggle.KravBortfalt]) {
            tilbakekreving.byttTilstand(JournalførInfobrevForBortfaltKrav, sideeffektContext)
            return
        }
        tilbakekreving.byttTilstand(KravgrunnlagBortfalt, sideeffektContext)
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        statusmeldingHendelse: SperretStatusmeldingHendelse,
        sideeffektContext: SideeffektContext,
    ) {
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        statusmeldingHendelse: EndretStatusmeldingHendelse,
        sideeffektContext: SideeffektContext,
    ) {
    }
}
