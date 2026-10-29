package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.behandling.TilbakekrevingRevurderingsårsakType
import no.nav.tilbakekreving.behandling.VedtakOmgjortAv
import no.nav.tilbakekreving.entities.TilbakekrevingRevurderingEntity
import java.sql.ResultSet
import java.util.UUID

object TilbakekrevingRevurderingEntityMapper : Entity<TilbakekrevingRevurderingEntity, UUID, UUID>(
    "tilbakekreving_revurdering",
    TilbakekrevingRevurderingEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val behandlingRef = field(
        column = "behandling_ref",
        getter = TilbakekrevingRevurderingEntity::behandlingRef,
        converter = FieldConverter.UUIDConverter.required(),
    )

    val revurderingsårsakType = field(
        column = "revurderingsårsak_type",
        getter = TilbakekrevingRevurderingEntity::revurderingsårsakType,
        converter = FieldConverter.EnumConverter.of<TilbakekrevingRevurderingsårsakType>().required(),
    )
    val instans = field(
        column = "instans",
        getter = TilbakekrevingRevurderingEntity::instans,
        converter = FieldConverter.EnumConverter.of<VedtakOmgjortAv>(),
    )
    val beskrivelse = field(
        column = "beskrivelse",
        getter = TilbakekrevingRevurderingEntity::beskrivelse,
        converter = FieldConverter.StringConverter.required(),
    )

    fun map(result: ResultSet): TilbakekrevingRevurderingEntity =
        TilbakekrevingRevurderingEntity(
            id = result[id],
            behandlingRef = result[behandlingRef],
            revurderingsårsakType = result[revurderingsårsakType],
            instans = result[instans],
            beskrivelse = result[beskrivelse],
        )
}
