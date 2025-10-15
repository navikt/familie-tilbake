package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.FatteVedtakStegEntity
import no.nav.tilbakekreving.entities.VurdertStegEntity
import no.nav.tilbakekreving.entity.Entity.Companion.get
import no.nav.tilbakekreving.entity.FatteVedtakStegEntityMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class NyFatteVedtakRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun hentVedtaksvurdering(
        behandlingId: UUID,
    ): FatteVedtakStegEntity? {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_totrinnsvurdering WHERE behandling_ref=?",
            behandlingId,
        ) { resultSet, _ ->
            val fattVedtakRef = resultSet[FatteVedtakStegEntityMapper.id]
            FatteVedtakStegEntityMapper.map(
                resultSet = resultSet,
                vurderinger = hentVurderinger(fattVedtakRef),
            )
        }.singleOrNull()
    }

    private fun hentVurderinger(fattVedtakRef: UUID): List<VurdertStegEntity> {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_totrinnsvurdering_vurdertsteg WHERE fattevedtak_ref=?",
            fattVedtakRef,
        ) { resultSet, _ ->
            FatteVedtakStegEntityMapper.Vurderinger.map(resultSet)
        }
    }

    fun lagre(fatteVedtakStegEntity: FatteVedtakStegEntity) {
        FatteVedtakStegEntityMapper.insertQuery(jdbcTemplate, fatteVedtakStegEntity)
        for (vurdering in fatteVedtakStegEntity.vurderteStegEntities) {
            FatteVedtakStegEntityMapper.Vurderinger.upsertQuery(jdbcTemplate, vurdering)
        }
    }
}
