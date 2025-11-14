package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.entities.BrukeruttalelseEntity
import no.nav.tilbakekreving.entities.UttalelseInfoEntity
import java.sql.ResultSet
import java.util.UUID

object BrukerUttalelseEntityMapper : Entity<BrukeruttalelseEntity, UUID, UUID>(
    "tilbakekreving_brukeruttalelse",
    BrukeruttalelseEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val behandlingRef = field(
        "behandling_ref",
        BrukeruttalelseEntity::behandlingRef,
        FieldConverter.UUIDConverter.required(),
    )

    val uttalelseVurdering = field(
        "uttalelse_vurdering",
        BrukeruttalelseEntity::uttalelseVurdering,
        FieldConverter.EnumConverter.of<UttalelseVurdering>().required(),
    )

    val utsettFrist = field(
        "utsett_frist",
        BrukeruttalelseEntity::utsettFrist,
        FieldConverter.LocalDateConverter,
    )

    val beskrivelseVedNeiEllerUtsettFrist = field(
        "beskrivelse_ved_nei_eller_utsett_frist",
        BrukeruttalelseEntity::beskrivelseVedNeiEllerUtsettFrist,
        FieldConverter.StringConverter,
    )

    fun map(
        resultSet: ResultSet,
        uttalelseInfoEntity: List<UttalelseInfoEntity>?,
    ): BrukeruttalelseEntity {
        return BrukeruttalelseEntity(
            id = resultSet[id],
            behandlingRef = resultSet[behandlingRef],
            uttalelseVurdering = resultSet[uttalelseVurdering],
            uttalelseInfoEntity = uttalelseInfoEntity,
            utsettFrist = resultSet[utsettFrist],
            beskrivelseVedNeiEllerUtsettFrist = resultSet[beskrivelseVedNeiEllerUtsettFrist],
        )
    }

    object UttalelseInfo : Entity<UttalelseInfoEntity, UUID, UUID>(
        "tilbakekreving_uttalelse_informasjon",
        UttalelseInfoEntity::id,
        FieldConverter.UUIDConverter.required(),
    ) {
        val brukeruttalelseRef = field(
            "brukeruttalelse_ref",
            UttalelseInfoEntity::brukeruttalelseRef,
            FieldConverter.UUIDConverter.required(),
        )
        val uttalelsesdato = field(
            "uttalelsesdato",
            UttalelseInfoEntity::uttalelsesdato,
            FieldConverter.LocalDateConverter,
        )

        val hvorBrukerenUttaletSeg = field(
            "hvor_brukeren_uttalet_seg",
            UttalelseInfoEntity::hvorBrukerenUttalteSeg,
            FieldConverter.StringConverter,
        )

        val uttalelseBeskrivelse = field(
            "uttalelse_beskrivelse",
            UttalelseInfoEntity::uttalelseBeskrivelse,
            FieldConverter.StringConverter,
        )

        fun map(resultSet: ResultSet): UttalelseInfoEntity {
            return UttalelseInfoEntity(
                id = resultSet[id],
                brukeruttalelseRef = resultSet[brukeruttalelseRef],
                uttalelsesdato = resultSet[uttalelsesdato]!!,
                hvorBrukerenUttalteSeg = resultSet[hvorBrukerenUttaletSeg]!!,
                uttalelseBeskrivelse = resultSet[uttalelseBeskrivelse]!!,
            )
        }
    }
}
