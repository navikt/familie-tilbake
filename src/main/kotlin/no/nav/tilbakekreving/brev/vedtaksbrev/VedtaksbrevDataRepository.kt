package no.nav.tilbakekreving.brev.vedtaksbrev

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.kontrakter.frontend.models.VedtaksbrevRedigerbareDataDto
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class VedtaksbrevDataRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun oppdaterVedtaksbrevData(behandlingId: UUID, data: VedtaksbrevRedigerbareDataDto): VedtaksbrevRedigerbareDataDto {
        val json = objectMapper.writeValueAsString(data)
        jdbcTemplate.update("INSERT INTO tilbakekreving_behandling_vedtaksbrev(behandling_ref, data) VALUES(?, ?::json) ON CONFLICT(behandling_ref) DO UPDATE SET data=EXCLUDED.data;", behandlingId, json)

        return hentVedtaksbrevData(behandlingId)!!
    }

    fun hentVedtaksbrevData(behandlingId: UUID): VedtaksbrevRedigerbareDataDto? {
        return jdbcTemplate.query("SELECT * FROM tilbakekreving_behandling_vedtaksbrev WHERE behandling_ref=?;", behandlingId) { rs, _ ->
            objectMapper.readValue<VedtaksbrevRedigerbareDataDto>(rs.getString("data"))
        }.singleOrNull()
    }
}
