package no.nav.tilbakekreving.entities

import kotlinx.serialization.Serializable

@Serializable
data class BehandlingEntity(
    val internId: String,
    val eksternId: String,
    val behandlingstype: String,
    val opprettet: String,
    val sistEndret: String,
    val enhet: EnhetEntity?,
    val årsak: String,
    var ansvarligSaksbehandlerEntity: BehandlerEntity,
    val eksternFagsakBehandlingRefEntity: EksternFagsakBehandlingEntity,
    val kravgrunnlagHendelseRefEntity: KravgrunnlagHendelseEntity,
    val foreldelsestegEntity: ForeldelsestegEntity,
    val faktastegEntity: FaktastegEntity,
    val vilkårsvurderingstegEntity: VilkårsvurderingstegEntity,
    val foreslåVedtakStegEntity: ForeslåVedtakStegEntity,
    val fatteVedtakStegEntity: FatteVedtakStegEntity,
)
