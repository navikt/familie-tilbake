package no.nav.tilbakekreving.entities

import java.time.LocalDateTime
import java.util.UUID

data class BehandlingEntity(
    val internId: UUID,
    val eksternId: UUID,
    val behandlingstype: String,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
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
