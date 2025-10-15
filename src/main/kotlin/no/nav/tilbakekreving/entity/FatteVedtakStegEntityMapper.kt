package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.entities.BehandlerEntity
import no.nav.tilbakekreving.entities.BehandlerType
import no.nav.tilbakekreving.entities.FatteVedtakStegEntity
import no.nav.tilbakekreving.entities.VurdertStegEntity
import no.nav.tilbakekreving.entities.VurdertStegType
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import java.sql.ResultSet
import java.util.UUID

object FatteVedtakStegEntityMapper : Entity<FatteVedtakStegEntity, UUID, UUID>(
    "tilbakekreving_totrinnsvurdering",
    FatteVedtakStegEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val behandlingRef = field(
        "behandling_ref",
        FatteVedtakStegEntity::behandlingRef,
        FieldConverter.UUIDConverter.required(),
    )

    val beslutter = field(
        "beslutter",
        { it.ansvarligBeslutter?.type },
        FieldConverter.EnumConverter.of<BehandlerType>(),
    )

    val behandlerIdent = field(
        "behandler_ident",
        { it.ansvarligBeslutter?.ident },
        FieldConverter.StringConverter,
    )

    fun map(
        resultSet: ResultSet,
        vurderinger: List<VurdertStegEntity>,
    ): FatteVedtakStegEntity {
        return FatteVedtakStegEntity(
            id = resultSet[id],
            behandlingRef = resultSet[behandlingRef],
            ansvarligBeslutter = resultSet[beslutter]?.let { type ->
                resultSet[behandlerIdent]?.let { ident ->
                    BehandlerEntity(type, ident)
                }
            },
            vurderteStegEntities = vurderinger,
        )
    }

    object Vurderinger : Entity<VurdertStegEntity, UUID, UUID>(
        "tilbakekreving_fattevedtak_vurdering",
        VurdertStegEntity::id,
        FieldConverter.UUIDConverter.required(),
    ) {
        val fattevedtakRef = field(
            "fattevedtak_ref",
            VurdertStegEntity::fattevedtakRef,
            FieldConverter.UUIDConverter.required(),
        )

        val behandlingssteg = field(
            "behandlingssteg",
            { it.steg },
            FieldConverter.EnumConverter.of<Behandlingssteg>().required(),
        )

        val vurdering = field(
            "vurdering",
            { it.vurdering },
            FieldConverter.EnumConverter.of<VurdertStegType>().required(),
        )

        val begrunnelse = field(
            "begrunnelse",
            { it.begrunnelse },
            FieldConverter.StringConverter,
        )

        fun map(resultSet: ResultSet): VurdertStegEntity {
            return VurdertStegEntity(
                id = resultSet[id],
                fattevedtakRef = resultSet[fattevedtakRef],
                steg = resultSet[behandlingssteg],
                vurdering = resultSet[vurdering],
                begrunnelse = resultSet[begrunnelse],
            )
        }
    }
}
