package no.nav.tilbakekreving.entities

import com.fasterxml.jackson.annotation.JsonAlias
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.brev.BrevHistorikk
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakBehandlingHistorikk
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagHistorikk
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingEntity(
    val id: UUID,
    val tilbakekrevingId: String,
    @field:JsonAlias("behandlingstype", "type")
    val type: Behandlingstype,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val enhet: EnhetEntity?,
    @field:JsonAlias("årsak", "revurderingsårsak")
    val revurderingsårsak: Behandlingsårsakstype?,
    var ansvarligSaksbehandler: BehandlerEntity,
    val eksternFagsakBehandlingRef: HistorikkReferanseEntity<UUID>,
    val kravgrunnlagRef: HistorikkReferanseEntity<UUID>,
    val foreldelsestegEntity: ForeldelsesstegEntity,
    val faktastegEntity: FaktastegEntity,
    val vilkårsvurderingstegEntity: VilkårsvurderingstegEntity,
    val foreslåVedtakStegEntity: ForeslåVedtakStegEntity,
    val fatteVedtakStegEntity: FatteVedtakStegEntity,
    val påVentEntity: PåVentEntity?,
    val brevmottakerStegEntity: BrevmottakerStegEntity?,
    val forhåndsvarselEntity: ForhåndsvarselEntity = ForhåndsvarselEntity(null, null, listOf()), // todo denne opprettelsen skal fjernes etter prodsetting
) {
    fun fraEntity(
        eksternFagsakBehandlingHistorikk: EksternFagsakBehandlingHistorikk,
        kravgrunnlagHistorikk: KravgrunnlagHistorikk,
        brevHistorikk: BrevHistorikk,
    ): Behandling {
        val sporing = Sporing("Ukjent", id.toString())
        val eksternFagsak = eksternFagsakBehandlingHistorikk.finn(eksternFagsakBehandlingRef.id, sporing)
        val kravgrunnlag = kravgrunnlagHistorikk.finn(kravgrunnlagRef.id, sporing)
        val foreldelsessteg = foreldelsestegEntity.fraEntity()
        return Behandling(
            id = id,
            type = type,
            opprettet = opprettet,
            sistEndret = sistEndret,
            enhet = enhet?.fraEntity(),
            revurderingsårsak = revurderingsårsak,
            ansvarligSaksbehandler = ansvarligSaksbehandler.fraEntity(),
            eksternFagsakRevurdering = eksternFagsak,
            kravgrunnlag = kravgrunnlag,
            foreldelsesteg = foreldelsessteg,
            faktasteg = faktastegEntity.fraEntity(brevHistorikk),
            vilkårsvurderingsteg = vilkårsvurderingstegEntity.fraEntity(foreldelsessteg),
            foreslåVedtakSteg = foreslåVedtakStegEntity.fraEntity(),
            fatteVedtakSteg = fatteVedtakStegEntity.fraEntity(),
            påVent = påVentEntity?.fraEntity(),
            brevmottakerSteg = brevmottakerStegEntity?.fraEntity(),
            forhåndsvarsel = forhåndsvarselEntity.fraEntity(),
        )
    }
}
