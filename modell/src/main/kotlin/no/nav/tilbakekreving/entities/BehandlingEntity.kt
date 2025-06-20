package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
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
)
