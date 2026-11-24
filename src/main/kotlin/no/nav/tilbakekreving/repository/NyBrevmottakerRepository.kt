package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.BrevmottakereEntity
import no.nav.tilbakekreving.entity.BrevmottakerEntityMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class NyBrevmottakerRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun hentBrevmottakere(tilbakekrevingId: String): BrevmottakereEntity {
        val mottakere = jdbcTemplate.timedQuery(
            "tilbakekreving_brevmottaker",
            "SELECT * FROM tilbakekreving_brevmottaker WHERE tilbakekreving_ref = ?",
            tilbakekrevingId,
        ) { resultSet, _ ->
            BrevmottakerEntityMapper.map(resultSet)
        }
        return BrevmottakereEntity(
            tilbakekrevingRef = tilbakekrevingId,
            hovedmottaker = mottakere.singleOrNull { it.tilleggsmottakerType == null },
            tilleggsmottaker = mottakere.singleOrNull { it.tilleggsmottakerType != null },
        )
    }

    fun lagre(brevmottakere: BrevmottakereEntity) {
        jdbcTemplate.update("DELETE FROM tilbakekreving_brevmottaker WHERE tilbakekreving_ref = ?", brevmottakere.tilbakekrevingRef)
        listOfNotNull(brevmottakere.hovedmottaker, brevmottakere.tilleggsmottaker).forEach {
            BrevmottakerEntityMapper.insertQuery(jdbcTemplate, it)
        }
    }
}
