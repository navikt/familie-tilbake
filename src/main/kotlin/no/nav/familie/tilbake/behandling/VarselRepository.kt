package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.behandling.domain.Varsel
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface VarselRepository {
    fun erstattAktivtVarsel(
        behandlingId: UUID,
        nyttVarsel: Varsel,
    )
}

@Repository
class VarselRepositoryImpl(
    private val jdbcOperations: NamedParameterJdbcOperations,
) : VarselRepository {
    @Transactional
    override fun erstattAktivtVarsel(
        behandlingId: UUID,
        nyttVarsel: Varsel,
    ) {
        jdbcOperations.queryForObject(
            "SELECT id FROM behandling WHERE id = :behandlingId FOR UPDATE",
            mapOf("behandlingId" to behandlingId),
            UUID::class.java,
        )

        val endret = nyttVarsel.sporbar.endret
        jdbcOperations.update(
            "UPDATE varsel SET aktiv = FALSE WHERE behandling_id = :behandlingId AND aktiv = TRUE",
            mapOf("behandlingId" to behandlingId),
        )

        jdbcOperations.update(
            """
INSERT INTO varsel (id,versjon,behandling_id,aktiv,varseltekst,varselbelop,opprettet_av,opprettet_tid,endret_av,endret_tid) 
VALUES (:id, :versjon, :behandlingId, :aktiv, :varseltekst, :varselbelop, :opprettetAv, :opprettetTid, :endretAv, :endretTid)
            """.trimIndent(),
            mapOf(
                "id" to nyttVarsel.id,
                "versjon" to nyttVarsel.versjon,
                "behandlingId" to behandlingId,
                "aktiv" to nyttVarsel.aktiv,
                "varseltekst" to nyttVarsel.varseltekst,
                "varselbelop" to nyttVarsel.varselbeløp,
                "opprettetAv" to nyttVarsel.sporbar.opprettetAv,
                "opprettetTid" to nyttVarsel.sporbar.opprettetTid,
                "endretAv" to endret.endretAv,
                "endretTid" to endret.endretTid,
            ),
        )

        jdbcOperations.batchUpdate(
            """
INSERT INTO varselsperiode (id, versjon, varsel_id, fom, tom, opprettet_av, opprettet_tid, endret_av, endret_tid) 
VALUES (:id, :versjon, :varselId, :fom, :tom, :opprettetAv, :opprettetTid, :endretAv, :endretTid)
            """.trimIndent(),
            nyttVarsel.perioder
                .map { periode ->
                    mapOf(
                        "id" to periode.id,
                        "versjon" to periode.versjon,
                        "varselId" to nyttVarsel.id,
                        "fom" to periode.fom,
                        "tom" to periode.tom,
                        "opprettetAv" to periode.sporbar.opprettetAv,
                        "opprettetTid" to periode.sporbar.opprettetTid,
                        "endretAv" to periode.sporbar.endret.endretAv,
                        "endretTid" to periode.sporbar.endret.endretTid,
                    )
                }.toTypedArray(),
        )
    }
}
