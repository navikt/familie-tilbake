package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingEntity(
    val internId: UUID,
    val eksternId: UUID,
    val behandlingstype: Behandlingstype,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val enhet: EnhetEntity?,
    val årsak: Behandlingsårsakstype,
    var ansvarligSaksbehandlerEntity: BehandlerEntity,
    val eksternFagsakBehandlingRefEntity: EksternFagsakBehandlingEntity?,
    val kravgrunnlagHendelseRefEntity: KravgrunnlagHendelseEntity,
    val foreldelsestegEntity: ForeldelsesstegEntity,
    val faktastegEntity: FaktastegEntity,
    val vilkårsvurderingstegEntity: VilkårsvurderingstegEntity,
    val foreslåVedtakStegEntity: ForeslåVedtakStegEntity,
    val fatteVedtakStegEntity: FatteVedtakStegEntity,
) {
    fun fraEntity(
        eksternFagsakBehandlingHistorikk: EksternFagsakBehandlingHistorikk,
        kravgrunnlagHistorikk: KravgrunnlagHistorikk,
        brevHistorikk: BrevHistorikk,
    ): Behandling {
        val eksternFagsak = eksternFagsakBehandlingHistorikk.nåværende()
        val kravgrunnlag = kravgrunnlagHistorikk.nåværende()
        return Behandling(
            internId = internId,
            eksternId = eksternId,
            behandlingstype = behandlingstype,
            opprettet = opprettet,
            sistEndret = sistEndret,
            enhet = enhet?.fraEntity(),
            årsak = årsak,
            ansvarligSaksbehandler = ansvarligSaksbehandlerEntity.fraEntity(),
            eksternFagsakBehandling = eksternFagsak,
            kravgrunnlag = kravgrunnlag,
            foreldelsesteg = foreldelsestegEntity.fraEntity(kravgrunnlag),
            faktasteg = faktastegEntity.fraEntity(eksternFagsak, kravgrunnlag, brevHistorikk),
            vilkårsvurderingsteg = vilkårsvurderingstegEntity.fraEntity(kravgrunnlag),
            foreslåVedtakSteg = foreslåVedtakStegEntity.fraEntity(),
            fatteVedtakSteg = fatteVedtakStegEntity.fraEntity(),
        )
    }
}
