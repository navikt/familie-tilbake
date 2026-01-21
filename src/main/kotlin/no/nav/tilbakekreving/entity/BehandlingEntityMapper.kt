package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.BehandlerEntity
import no.nav.tilbakekreving.entities.BehandlerType
import no.nav.tilbakekreving.entities.BehandlingEntity
import no.nav.tilbakekreving.entities.BrevmottakerStegEntity
import no.nav.tilbakekreving.entities.BrukeruttalelseEntity
import no.nav.tilbakekreving.entities.EnhetEntity
import no.nav.tilbakekreving.entities.FaktastegEntity
import no.nav.tilbakekreving.entities.FatteVedtakStegEntity
import no.nav.tilbakekreving.entities.ForeldelsesstegEntity
import no.nav.tilbakekreving.entities.ForeslåVedtakStegEntity
import no.nav.tilbakekreving.entities.ForhåndsvarselEntity
import no.nav.tilbakekreving.entities.ForhåndsvarselUnntakEntity
import no.nav.tilbakekreving.entities.FristUtsettelseEntity
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
        column = "type",
        getter = BehandlingEntity::type,
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
        column = "revurderingsårsak",
        getter = BehandlingEntity::revurderingsårsak,
        converter = FieldConverter.EnumConverter.of<Behandlingsårsakstype>(),
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

    val ansvarligSaksbehandlerType = field(
        column = "ansvarlig_saksbehandler_type",
        getter = { it.ansvarligSaksbehandler.type },
        converter = FieldConverter.EnumConverter.of<BehandlerType>().required(),
    )

    val ansvarligSaksbehandlerIdent = field(
        column = "ansvarlig_saksbehandler_ident",
        getter = { it.ansvarligSaksbehandler.ident },
        converter = FieldConverter.StringConverter.required(),
    )

    val enhetId = field(
        column = "enhet_id",
        getter = { it.enhet?.kode },
        converter = FieldConverter.StringConverter,
    )

    val enhetNavn = field(
        column = "enhet_navn",
        getter = { it.enhet?.navn },
        converter = FieldConverter.StringConverter,
    )

    fun map(
        resultSet: ResultSet,
        foreldelsessteg: ForeldelsesstegEntity,
        faktasteg: FaktastegEntity,
        vilkårsvurdering: VilkårsvurderingstegEntity,
        foreslåVedtak: ForeslåVedtakStegEntity,
        fatteVedtak: FatteVedtakStegEntity,
        påVent: PåVentEntity?,
        brevmottakerSteg: BrevmottakerStegEntity?,
        brukeruttalelseEntity: BrukeruttalelseEntity?,
        forhåndsvarselUnntak: ForhåndsvarselUnntakEntity?,
        fristUtsettelse: List<FristUtsettelseEntity>,
    ): BehandlingEntity {
        return BehandlingEntity(
            id = resultSet[id],
            tilbakekrevingId = resultSet[tilbakekrevingId],
            type = resultSet[behandlingstype],
            opprettet = resultSet[opprettet],
            sistEndret = resultSet[sistEndret],
            enhet = resultSet[enhetId]?.let {
                EnhetEntity(kode = it, navn = resultSet[enhetNavn]!!)
            },
            revurderingsårsak = resultSet[årsak],
            ansvarligSaksbehandler = BehandlerEntity(type = resultSet[ansvarligSaksbehandlerType], ident = resultSet[ansvarligSaksbehandlerIdent]),
            eksternFagsakBehandlingRef = HistorikkReferanseEntity(resultSet[eksternFagsakBehandlingId]),
            kravgrunnlagRef = HistorikkReferanseEntity(resultSet[kravgrunnlagId]),
            foreldelsestegEntity = foreldelsessteg,
            faktastegEntity = faktasteg,
            vilkårsvurderingstegEntity = vilkårsvurdering,
            foreslåVedtakStegEntity = foreslåVedtak,
            fatteVedtakStegEntity = fatteVedtak,
            påVentEntity = påVent,
            brevmottakerStegEntity = brevmottakerSteg,
            forhåndsvarselEntity = ForhåndsvarselEntity(brukeruttalelseEntity, forhåndsvarselUnntak, fristUtsettelse),
        )
    }
}
