package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.entities.ForeslåVedtakStegEntity
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagSammenligning
import java.util.UUID

class ForeslåVedtakSteg(
    private val id: UUID,
    private var vurdert: Boolean,
    private var tilbakeført: ÅrsakTilTilbakeføring?,
) : Saksbehandlingsteg {
    override val type = Behandlingssteg.FORESLÅ_VEDTAK

    override fun erFullstendig(klokke: Klokke): Boolean = vurdert

    override fun erPåbegynt(): Boolean = vurdert

    override fun trengerNyVurdering(): ÅrsakTilTilbakeføring? {
        return tilbakeført
    }

    override fun underkjennSteget() {
        vurdert = false
        tilbakeført = ÅrsakTilTilbakeføring.Underkjent
    }

    internal fun håndter() {
        vurdert = true
        tilbakeført = null
    }

    fun tilEntity(behandlingRef: UUID): ForeslåVedtakStegEntity {
        return ForeslåVedtakStegEntity(
            id = id,
            behandlingRef = behandlingRef,
            vurdert = vurdert,
            tilbakeført = tilbakeført,
        )
    }

    override fun perideEndretBeløp(forskjell: KravgrunnlagSammenligning.Forskjell.JustertBeløp) {
        tilbakeført = ÅrsakTilTilbakeføring.NyttKravgrunnlag
    }

    override fun nullstill(
        kravgrunnlag: KravgrunnlagHendelse,
        eksternFagsakRevurdering: EksternFagsakRevurdering,
    ) {
        vurdert = false
    }

    companion object {
        fun opprett() = ForeslåVedtakSteg(
            id = UUID.randomUUID(),
            vurdert = false,
            tilbakeført = null,
        )
    }
}
