package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.ForhåndsvarselEntity
import no.nav.tilbakekreving.entity.ForhåndsvarselEntityMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class NyForhåndsvarselRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val uttalelseRepository: NyUttalelseRepository,
    private val forhåndsvarselUnntakRepository: NyForhåndsvarselUnntakRepository,
    private val utsettUttalelseRepository: NyUtsettUttalelseRepository,
) {
    fun hentForhåndsvarsel(behandlingId: UUID): ForhåndsvarselEntity {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_forhåndsvarsel WHERE id = ?",
            behandlingId,
        ) { resultSet, _ ->
            ForhåndsvarselEntityMapper.map(
                resultSet,
                brukeruttalelseEntity = uttalelseRepository.hentBrukerUttalelsen(behandlingId),
                forhåndsvarselUnntak = forhåndsvarselUnntakRepository.hentForhåndsvarselUnntak(behandlingId),
                fristUtsettelse = utsettUttalelseRepository.hentUtsettUttalelseFrist(behandlingId),
            )
        }.single()
    }

    fun lagre(forhåndsvarsel: ForhåndsvarselEntity, behandlingId: UUID) {
        ForhåndsvarselEntityMapper.upsertQuery(jdbcTemplate, forhåndsvarsel)
        forhåndsvarselUnntakRepository.lagre(forhåndsvarsel.forhåndsvarselUnntakEntity, behandlingId)
        uttalelseRepository.lagre(forhåndsvarsel.brukeruttalelseEntity, behandlingId)
        utsettUttalelseRepository.lagre(forhåndsvarsel.uttalelsesfristEntity, behandlingId)
    }
}
