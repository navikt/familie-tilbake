package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.BehandlerEntity
import no.nav.tilbakekreving.entities.BehandlingEntity
import no.nav.tilbakekreving.entities.BrevmottakerStegEntity
import no.nav.tilbakekreving.entities.EnhetEntity
import no.nav.tilbakekreving.entities.FaktastegEntity
import no.nav.tilbakekreving.entities.FatteVedtakStegEntity
import no.nav.tilbakekreving.entities.ForeldelsesstegEntity
import no.nav.tilbakekreving.entities.ForeslåVedtakStegEntity
import no.nav.tilbakekreving.entities.HistorikkReferanseEntity
import no.nav.tilbakekreving.entities.PåVentEntity
import no.nav.tilbakekreving.entities.VilkårsvurderingstegEntity
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import java.sql.ResultSet
import java.util.UUID

object BehandlingEntityMapper : Entity<BehandlingEntity, UUID, UUID>(
    "tilbakekreving_behandling",
    BehandlingEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val tilbakekrevingId = field(
        "tilbakekreving_id",
        BehandlingEntity::tilbakekrevingId,
        FieldConverter.NumericId,
    )
    val behandlingstype = field(
        column = "behandlingstype",
        getter = BehandlingEntity::behandlingstype,
        converter = FieldConverter.EnumConverter.of<Behandlingstype>().required(),
    )
    val opprettet = field(
        column = "opprettet",
        getter = BehandlingEntity::opprettet,
        converter = FieldConverter.LocalDateTimeConverter.required(),
    )
    val sistEndret = field(
        column = "sist_endret",
        getter = BehandlingEntity::sistEndret,
        converter = FieldConverter.LocalDateTimeConverter.required(),
    )
    val årsak = field(
        column = "årsak",
        getter = BehandlingEntity::årsak,
        converter = FieldConverter.EnumConverter.of<Behandlingsårsakstype>().required(),
    )

    val eksternFagsakBehandlingId = field(
        column = "ekstern_fagsak_behandling_id",
        getter = { it.eksternFagsakBehandlingRef.id },
        converter = FieldConverter.UUIDConverter.required(),
    )

    val kravgrunnlagId = field(
        column = "kravgrunnlag_id",
        getter = { it.kravgrunnlagRef.id },
        converter = FieldConverter.UUIDConverter.required(),
    )

    fun map(
        resultSet: ResultSet,
        enhet: EnhetEntity?,
        ansvarligSaksbehandler: BehandlerEntity,
        foreldelsessteg: ForeldelsesstegEntity,
        faktasteg: FaktastegEntity,
        vilkårsvurdering: VilkårsvurderingstegEntity,
        foreslåVedtak: ForeslåVedtakStegEntity,
        fatteVedtak: FatteVedtakStegEntity,
        påVent: PåVentEntity?,
        brevmottakerSteg: BrevmottakerStegEntity?,
    ): BehandlingEntity {
        return BehandlingEntity(
            id = resultSet[id],
            tilbakekrevingId = resultSet[tilbakekrevingId],
            behandlingstype = resultSet[behandlingstype],
            opprettet = resultSet[opprettet],
            sistEndret = resultSet[sistEndret],
            enhet = enhet,
            årsak = resultSet[årsak],
            ansvarligSaksbehandler = ansvarligSaksbehandler,
            eksternFagsakBehandlingRef = HistorikkReferanseEntity(resultSet[eksternFagsakBehandlingId]),
            kravgrunnlagRef = HistorikkReferanseEntity(resultSet[kravgrunnlagId]),
            foreldelsestegEntity = foreldelsessteg,
            faktastegEntity = faktasteg,
            vilkårsvurderingstegEntity = vilkårsvurdering,
            foreslåVedtakStegEntity = foreslåVedtak,
            fatteVedtakStegEntity = fatteVedtak,
            påVentEntity = påVent,
            brevmottakerStegEntity = brevmottakerSteg,
        )
    }
}
