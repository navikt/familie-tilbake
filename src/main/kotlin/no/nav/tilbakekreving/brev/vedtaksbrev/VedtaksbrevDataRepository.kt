package no.nav.tilbakekreving.brev.vedtaksbrev

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.tilbakekreving.brev.vedtaksbrev.VedtaksbrevDataRepository.PeriodeavsnittEntity.Companion.overskriv
import no.nav.tilbakekreving.entity.FieldConverter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
class VedtaksbrevDataRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun oppdaterVedtaksbrevData(behandlingId: UUID, data: VedtaksbrevEntity): Pair<LocalDateTime, VedtaksbrevEntity> {
        val originalePerioder = hentVedtaksbrevData(behandlingId)?.let { (_, data) -> data.avsnitt } ?: emptyList()
        val json = objectMapper.writeValueAsString(data.merge(originalePerioder))
        jdbcTemplate.update("INSERT INTO tilbakekreving_behandling_vedtaksbrev(behandling_ref, data, sist_oppdatert) VALUES(?, ?::json, ?) ON CONFLICT(behandling_ref) DO UPDATE SET data=EXCLUDED.data;", behandlingId, json, LocalDateTime.now())

        return hentVedtaksbrevData(behandlingId)!!
    }

    fun hentVedtaksbrevData(behandlingId: UUID): Pair<LocalDateTime, VedtaksbrevEntity>? {
        return jdbcTemplate.query("SELECT * FROM tilbakekreving_behandling_vedtaksbrev WHERE behandling_ref=?;", behandlingId) { rs, _ ->
            FieldConverter.LocalDateTimeConverter.required().convert(rs, "sist_oppdatert") to objectMapper.readValue<VedtaksbrevEntity>(rs.getString("data"))
        }.singleOrNull()
    }

    data class VedtaksbrevEntity(
        val hovedavsnitt: HovedavsnittEntity,
        val avsnitt: List<PeriodeavsnittEntity>,
    ) {
        fun merge(existing: List<PeriodeavsnittEntity>): VedtaksbrevEntity {
            val originaleIder = existing.map { it.id }
            return VedtaksbrevEntity(
                hovedavsnitt = hovedavsnitt,
                avsnitt = existing.overskriv(avsnitt) + avsnitt.filter { it.id !in originaleIder },
            )
        }
    }

    data class HovedavsnittEntity(
        val underavsnitt: List<UnderavsnittEntity>,
    )

    data class PeriodeavsnittEntity(
        val id: UUID,
        val underavsnitt: List<UnderavsnittEntity>,
        val påkrevdBegrunnelser: List<PåkrevdBegrunnelse>,
    ) {
        fun slåSammen(eksisterendeBegrunnelser: List<PåkrevdBegrunnelse>): PeriodeavsnittEntity {
            val nyeBegrunnelseTyper = påkrevdBegrunnelser.map(PåkrevdBegrunnelse::type)
            return PeriodeavsnittEntity(
                id = id,
                underavsnitt = underavsnitt,
                påkrevdBegrunnelser = påkrevdBegrunnelser + eksisterendeBegrunnelser.filter { it.type !in nyeBegrunnelseTyper },
            )
        }

        companion object {
            fun Iterable<PeriodeavsnittEntity>.finn(id: UUID): PeriodeavsnittEntity? = singleOrNull { it.id == id }

            fun Iterable<PeriodeavsnittEntity>.overskriv(eksisterende: List<PeriodeavsnittEntity>): List<PeriodeavsnittEntity> =
                map { periode -> eksisterende.finn(periode.id)?.slåSammen(periode.påkrevdBegrunnelser) ?: periode }
        }
    }

    data class PåkrevdBegrunnelse(
        val type: String,
        val underavsnitt: List<String>,
    )

    data class UnderavsnittEntity(
        val type: Type,
        val tittel: String?,
        val tekst: String?,
        val underavsnitt: List<UnderavsnittEntity>?,
    ) {
        enum class Type {
            RENTEKST,
            UNDERAVSNITT,
        }
    }
}
