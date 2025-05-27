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
    var ansvarligSaksbehandler: String,
    val eksternFagsakBehandlingRef: UUID,
    val kravgrunnlagRef: UUID,
    val foreldelsesteg: ForeldelsestegEntity,
    val faktasteg: FaktastegEntity,
    val vilkårsvurderingsteg: VilkårsvurderingstegEntity,
    val foreslåVedtakSteg: ForeslåVedtakStegEntity,
)
