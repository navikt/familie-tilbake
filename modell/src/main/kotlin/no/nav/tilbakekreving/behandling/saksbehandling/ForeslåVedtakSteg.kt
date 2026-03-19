package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.entities.ForeslåVedtakStegEntity
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import java.util.UUID

class ForeslåVedtakSteg(
    private val id: UUID,
    private var vurdert: Boolean,
    private var underkjent: Boolean,
) : Saksbehandlingsteg {
    override val type = Behandlingssteg.FORESLÅ_VEDTAK

    override fun erFullstendig(): Boolean = vurdert

    override fun erUnderkjent(): Boolean {
        return underkjent
    }

    override fun underkjennSteget() {
        vurdert = false
        this.underkjent = true
    }

    internal fun håndter() {
        vurdert = true
        underkjent = false
    }

    fun tilEntity(behandlingRef: UUID): ForeslåVedtakStegEntity {
        return ForeslåVedtakStegEntity(
            id = id,
            behandlingRef = behandlingRef,
            vurdert = vurdert,
            trengerNyVurdering = underkjent,
        )
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
            underkjent = false,
        )
    }
}
