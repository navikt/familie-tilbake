package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.behandling.UttalelseVurdering
import no.nav.tilbakekreving.entities.BrukeruttalelseEntity
import no.nav.tilbakekreving.entities.UtsettFristInfoEntity
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

    val kommentar = field(
        "kommentar",
        BrukeruttalelseEntity::kommentar,
        FieldConverter.StringConverter,
    )

    fun map(
        resultSet: ResultSet,
        uttalelseInfoEntity: List<UttalelseInfoEntity>,
        utsettFristInfoEntity: List<UtsettFristInfoEntity>,
    ): BrukeruttalelseEntity {
        return BrukeruttalelseEntity(
            id = resultSet[id],
            behandlingRef = resultSet[behandlingRef],
            uttalelseVurdering = resultSet[uttalelseVurdering],
            uttalelseInfoEntity = uttalelseInfoEntity,
            utsettFristEntity = utsettFristInfoEntity,
            kommentar = resultSet[kommentar],
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

    object UtsettFrist : Entity<UtsettFristInfoEntity, UUID, UUID>(
        "tilbakekreving_utsett_uttalelse",
        UtsettFristInfoEntity::id,
        FieldConverter.UUIDConverter.required(),
    ) {
        val brukeruttalelseRef = field(
            "brukeruttalelse_ref",
            UtsettFristInfoEntity::brukeruttalelseRef,
            FieldConverter.UUIDConverter.required(),
        )

        val nyFrist = field(
            "ny_frist",
            UtsettFristInfoEntity::nyFrist,
            FieldConverter.LocalDateConverter.required(),
        )

        val begrunnelse = field(
            "begrunnelse",
            UtsettFristInfoEntity::begrunnelse,
            FieldConverter.StringConverter.required(),
        )

        fun map(resultSet: ResultSet): UtsettFristInfoEntity {
            return UtsettFristInfoEntity(
                id = resultSet[id],
                brukeruttalelseRef = resultSet[brukeruttalelseRef],
                nyFrist = resultSet[nyFrist],
                begrunnelse = resultSet[begrunnelse],
            )
        }
    }
}
