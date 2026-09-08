package no.nav.tilbakekreving

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class Ryddejobb(private val jdbcTemplate: JdbcTemplate) {
    private val log = LoggerFactory.getLogger(Ryddejobb::class.java)

    @Scheduled(fixedRate = 500)
    fun rydd() {
        val updates = jdbcTemplate.update(
            """DO
$$
    DECLARE
        id int;
    BEGIN
        FOR id IN SELECT t.id
                  FROM tilbakekreving t
                           JOIN public.tilbakekreving_ekstern_fagsak tef on t.id = tef.tilbakekreving_ref
                  WHERE tef.ekstern_id = 'BFeec6149e'
                  ORDER BY RANDOM()
                  LIMIT 400
            LOOP
                CALL slett_tilbakekreving(id);
                COMMIT;
            END LOOP;
    END
$$;""",
        )

        val gjenstående = jdbcTemplate.query(
            """SELECT count(1) gjenstående
FROM tilbakekreving t
         JOIN public.tilbakekreving_ekstern_fagsak tef on t.id = tef.tilbakekreving_ref
WHERE tef.ekstern_id = 'BFeec6149e';""",
        ) { rs, _ ->
            rs.getInt("gjenstående")
        }.single()

        log.info("Fjerner ugyldige tilbakekrevinger, oppdatert={}, gjenstående={}", updates, gjenstående)
    }
}
