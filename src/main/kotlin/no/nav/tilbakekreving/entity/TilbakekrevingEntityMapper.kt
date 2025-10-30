package no.nav.tilbakekreving.entity

import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.entities.BehandlingEntity
import no.nav.tilbakekreving.entities.BrevEntity
import no.nav.tilbakekreving.entities.BrukerEntity
import no.nav.tilbakekreving.entities.EksternFagsakEntity
import no.nav.tilbakekreving.entities.KravgrunnlagHendelseEntity
import no.nav.tilbakekreving.entities.TilbakekrevingEntity
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.sql.ResultSet

object TilbakekrevingEntityMapper : Entity<TilbakekrevingEntity, String, Long>(
    tableName = "tilbakekreving",
    idGetter = TilbakekrevingEntity::id,
    idConverter = FieldConverter.NumericId,
) {
    val nåværendeTilstand = field(
        column = "nåværende_tilstand",
        getter = TilbakekrevingEntity::nåværendeTilstand,
        converter = FieldConverter.EnumConverter.of<TilbakekrevingTilstand>().required(),
    )
    val opprettet = field(
        column = "opprettet",
        getter = TilbakekrevingEntity::opprettet,
        converter = FieldConverter.LocalDateTimeConverter.required(),
    )
    val opprettelsesvalg = field(
        column = "opprettelsesvalg",
        getter = TilbakekrevingEntity::opprettelsesvalg,
        converter = FieldConverter.EnumConverter.of<Opprettelsesvalg>().required(),
    )
    val nestePåminnelse = field(
        column = "neste_påminnelse",
        getter = TilbakekrevingEntity::nestePåminnelse,
        converter = FieldConverter.LocalDateTimeConverter,
    )

    fun map(
        resultSet: ResultSet,
        eksternFagsak: EksternFagsakEntity,
        behandlingHistorikk: List<BehandlingEntity>,
        kravgrunnlagHistorikk: List<KravgrunnlagHendelseEntity>,
        brevHistorikk: List<BrevEntity>,
        bruker: BrukerEntity?,
    ): TilbakekrevingEntity {
        return TilbakekrevingEntity(
            id = resultSet[id],
            nåværendeTilstand = resultSet[nåværendeTilstand],
            eksternFagsak = eksternFagsak,
            behandlingHistorikkEntities = behandlingHistorikk,
            kravgrunnlagHistorikkEntities = kravgrunnlagHistorikk,
            brevHistorikkEntities = brevHistorikk,
            opprettet = resultSet[opprettet],
            opprettelsesvalg = resultSet[opprettelsesvalg],
            nestePåminnelse = resultSet[nestePåminnelse],
            bruker = bruker,
        )
    }
}
