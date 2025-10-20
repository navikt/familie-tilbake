package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.PåVentEntity
import no.nav.tilbakekreving.entity.PåventEntityMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class NyPåventRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun hentPåventetBehandling(behandlingId: UUID): PåVentEntity? {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_påvent WHERE behandling_ref=?",
            behandlingId,
        ) { resultSet, _ ->
            PåventEntityMapper.map(resultSet = resultSet)
        }.singleOrNull()
    }

    fun lagre(påVentEntity: PåVentEntity) {
        PåventEntityMapper.upsertQuery(jdbcTemplate, påVentEntity)
    }
}
