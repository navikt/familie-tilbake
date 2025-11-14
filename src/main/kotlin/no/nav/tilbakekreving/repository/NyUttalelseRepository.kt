package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.BrukeruttalelseEntity
import no.nav.tilbakekreving.entities.UttalelseInfoEntity
import no.nav.tilbakekreving.entity.BrukerUttalelseEntityMapper
import no.nav.tilbakekreving.entity.BrukerUttalelseEntityMapper.UttalelseInfo.brukeruttalelseRef
import no.nav.tilbakekreving.entity.Entity.Companion.get
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class NyUttalelseRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun hentBrukerUttalelsen(behandlingId: UUID): BrukeruttalelseEntity? {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_brukeruttalelse WHERE behandling_ref = ?",
            behandlingId,
        ) { resultSet, _ ->
            val uttalelseInfoEntity = hentUttalelseInfo(resultSet[BrukerUttalelseEntityMapper.id]).takeIf { it.isNotEmpty() }
            BrukerUttalelseEntityMapper.map(resultSet, uttalelseInfoEntity)
        }.singleOrNull()
    }

    private fun hentUttalelseInfo(brukeruttalelseRef: UUID): List<UttalelseInfoEntity> {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_uttalelse_informasjon WHERE brukeruttalelse_ref=?",
            brukeruttalelseRef,
        ) { resultSet, _ ->
            BrukerUttalelseEntityMapper.UttalelseInfo.map(resultSet)
        }
    }

    fun lagre(brukerUttaleserEntity: BrukeruttalelseEntity?) {
        BrukerUttalelseEntityMapper.insertQuery(jdbcTemplate, brukerUttaleserEntity!!)
        jdbcTemplate.update("DELETE FROM tilbakekreving_uttalelse_informasjon WHERE brukeruttalelse_ref=?;", brukerUttaleserEntity.id)
        brukerUttaleserEntity.uttalelseInfoEntity?.forEach {
            BrukerUttalelseEntityMapper.UttalelseInfo.upsertQuery(jdbcTemplate, it)
        }
    }
}
