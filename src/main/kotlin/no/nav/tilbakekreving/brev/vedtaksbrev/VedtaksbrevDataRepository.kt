package no.nav.tilbakekreving.brev.vedtaksbrev

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.tilbakekreving.entity.FieldConverter
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevRedigerbareDataUpdateDto
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
class VedtaksbrevDataRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun oppdaterVedtaksbrevData(behandlingId: UUID, data: VedtaksbrevRedigerbareDataUpdateDto): Pair<LocalDateTime, VedtaksbrevRedigerbareDataUpdateDto> {
        val json = objectMapper.writeValueAsString(data)
        jdbcTemplate.update("INSERT INTO tilbakekreving_behandling_vedtaksbrev(behandling_ref, data, sist_oppdatert) VALUES(?, ?::json, ?) ON CONFLICT(behandling_ref) DO UPDATE SET data=EXCLUDED.data;", behandlingId, json, LocalDateTime.now())

        return hentVedtaksbrevData(behandlingId)!!
    }

    fun hentVedtaksbrevData(behandlingId: UUID): Pair<LocalDateTime, VedtaksbrevRedigerbareDataUpdateDto>? {
        return jdbcTemplate.query("SELECT * FROM tilbakekreving_behandling_vedtaksbrev WHERE behandling_ref=?;", behandlingId) { rs, _ ->
            FieldConverter.LocalDateTimeConverter.required().convert(rs, "sist_oppdatert") to objectMapper.readValue<VedtaksbrevRedigerbareDataUpdateDto>(rs.getString("data"))
        }.singleOrNull()
    }
}
