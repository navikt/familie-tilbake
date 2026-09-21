package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.behandling.saksbehandling.ÅrsakTilTilbakeføring
import no.nav.tilbakekreving.entities.BrukeruttalelseEntity
import no.nav.tilbakekreving.entities.ForhåndsvarselEntity
import no.nav.tilbakekreving.entities.ForhåndsvarselUnntakEntity
import no.nav.tilbakekreving.entities.ForhåndsvarselVurderingstype
import no.nav.tilbakekreving.entities.UttalelsesfristEntity
import java.sql.ResultSet
import java.util.UUID

object ForhåndsvarselEntityMapper : Entity<ForhåndsvarselEntity, UUID, UUID>(
    "tilbakekreving_forhåndsvarsel",
    ForhåndsvarselEntity::id,
    FieldConverter.UUIDConverter.required(),
) {
    val vurderingstype = field(
        "vurderingstype",
        ForhåndsvarselEntity::vurderingstype,
        FieldConverter.EnumConverter.of<ForhåndsvarselVurderingstype>().required(),
    )

    val tilbakeført = field(
        "tilbakeført",
        ForhåndsvarselEntity::tilbakeført,
        FieldConverter.EnumConverter.of<ÅrsakTilTilbakeføring>(),
    )

    fun map(
        resultSet: ResultSet,
        brukeruttalelseEntity: BrukeruttalelseEntity?,
        forhåndsvarselUnntak: ForhåndsvarselUnntakEntity?,
        fristUtsettelse: UttalelsesfristEntity?,
    ): ForhåndsvarselEntity {
        return ForhåndsvarselEntity(
            id = resultSet[id],
            vurderingstype = resultSet[vurderingstype],
            brukeruttalelseEntity = brukeruttalelseEntity,
            forhåndsvarselUnntakEntity = forhåndsvarselUnntak,
            uttalelsesfristEntity = fristUtsettelse,
            tilbakeført = resultSet[tilbakeført],
        )
    }
}
