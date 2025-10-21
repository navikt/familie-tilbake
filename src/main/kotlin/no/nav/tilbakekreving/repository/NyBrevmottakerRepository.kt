package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.BrevmottakerStegEntity
import no.nav.tilbakekreving.entities.MottakerType
import no.nav.tilbakekreving.entities.RegistrertBrevmottakerEntity
import no.nav.tilbakekreving.entity.BrevmottakerEntityMapper
import no.nav.tilbakekreving.entity.BrevmottakerEntityMapper.RegistrertBrevmottaker.mottakerType
import no.nav.tilbakekreving.entity.BrevmottakerEntityMapper.id
import no.nav.tilbakekreving.entity.Entity.Companion.get
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class NyBrevmottakerRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun hentBrevmottaker(behandlingRef: UUID): BrevmottakerStegEntity? {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_brevmottaker WHERE behandling_ref = ?",
            behandlingRef,
        ) { resultSet, _ ->

            val registrertBrevmottakere = hentRegistrertBrevmottaker(resultSet[id])
            when (registrertBrevmottakere.size) {
                1 -> BrevmottakerEntityMapper.map(
                    resultSet = resultSet,
                    defaultMottakerEntity = registrertBrevmottakere[0],
                    registrertBrevmottakerEntity = registrertBrevmottakere[0],
                )
                else -> {
                    val defaultMottaker = registrertBrevmottakere.find { it.mottakerType == MottakerType.DEFAULT_MOTTAKER }
                    val registrertMottaker = registrertBrevmottakere.find { it.mottakerType != MottakerType.DEFAULT_MOTTAKER }
                    BrevmottakerEntityMapper.map(
                        resultSet = resultSet,
                        defaultMottakerEntity = defaultMottaker!!,
                        registrertBrevmottakerEntity = registrertMottaker!!,
                    )
                }
            }
        }.singleOrNull()
    }

    fun hentRegistrertBrevmottaker(brevmottakerRef: UUID): List<RegistrertBrevmottakerEntity> {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_registrert_brevmottaker WHERE brevmottaker_ref = ?",
            brevmottakerRef,
        ) { resultSet, _ ->
            when (resultSet[mottakerType]) {
                MottakerType.DEFAULT_MOTTAKER,
                MottakerType.UTENLANDSK_ADRESSE_MOTTAKER,
                MottakerType.FULLMEKTIG_MOTTAKER,
                MottakerType.VERGE_MOTTAKER,
                MottakerType.DODSBO_MOTTAKER,
                -> BrevmottakerEntityMapper.RegistrertBrevmottaker.map(
                    resultSet = resultSet,
                    utenlandskAdresse = null,
                    vergeMottaker = null,
                    fullmektigMottaker = null,
                )
                MottakerType.UTENLANDSK_ADRESSE_OG_VERGE_MOTTAKER -> {
                    BrevmottakerEntityMapper.RegistrertBrevmottaker.map(
                        resultSet = resultSet,
                        utenlandskAdresse = hentKomboAdresser(resultSet[id], MottakerType.UTENLANDSK_ADRESSE_MOTTAKER),
                        vergeMottaker = hentKomboAdresser(resultSet[id], MottakerType.VERGE_MOTTAKER),
                        fullmektigMottaker = null,
                    )
                }
                MottakerType.UTENLANDSK_ADRESSE_OG_FULLMEKTIG_MOTTAKER -> BrevmottakerEntityMapper.RegistrertBrevmottaker.map(
                    resultSet = resultSet,
                    utenlandskAdresse = hentKomboAdresser(resultSet[id], MottakerType.UTENLANDSK_ADRESSE_MOTTAKER),
                    vergeMottaker = null,
                    fullmektigMottaker = hentKomboAdresser(resultSet[id], MottakerType.FULLMEKTIG_MOTTAKER),
                )
            }
        }
    }

    fun hentKomboAdresser(andreAdresseId: UUID, mottakerType: MottakerType): RegistrertBrevmottakerEntity? {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_registrert_brevmottaker WHERE parent_ref = ?  AND mottaker_type = ?",
            andreAdresseId,
            mottakerType.name,
        ) { resultSet, _ ->
            BrevmottakerEntityMapper.RegistrertBrevmottaker.map(
                resultSet = resultSet,
                utenlandskAdresse = null,
                vergeMottaker = null,
                fullmektigMottaker = null,
            )
        }.singleOrNull()
    }

    fun lagre(brevmottakerStegEntity: BrevmottakerStegEntity) {
        jdbcTemplate.update("DELETE FROM tilbakekreving_brevmottaker WHERE id=?", brevmottakerStegEntity.id)
        BrevmottakerEntityMapper.upsertQuery(jdbcTemplate, brevmottakerStegEntity)
        BrevmottakerEntityMapper.RegistrertBrevmottaker.upsertQuery(jdbcTemplate, brevmottakerStegEntity.defaultMottakerEntity)

        when (brevmottakerStegEntity.registrertBrevmottakerEntity.mottakerType) {
            MottakerType.UTENLANDSK_ADRESSE_OG_VERGE_MOTTAKER -> {
                BrevmottakerEntityMapper.RegistrertBrevmottaker.upsertQuery(jdbcTemplate, brevmottakerStegEntity.registrertBrevmottakerEntity)
                BrevmottakerEntityMapper.RegistrertBrevmottaker.upsertQuery(jdbcTemplate, brevmottakerStegEntity.registrertBrevmottakerEntity.utenlandskAdresse!!)
                BrevmottakerEntityMapper.RegistrertBrevmottaker.upsertQuery(jdbcTemplate, brevmottakerStegEntity.registrertBrevmottakerEntity.verge!!)
            }
            MottakerType.UTENLANDSK_ADRESSE_OG_FULLMEKTIG_MOTTAKER -> {
                BrevmottakerEntityMapper.RegistrertBrevmottaker.upsertQuery(jdbcTemplate, brevmottakerStegEntity.registrertBrevmottakerEntity)
                BrevmottakerEntityMapper.RegistrertBrevmottaker.upsertQuery(jdbcTemplate, brevmottakerStegEntity.registrertBrevmottakerEntity.utenlandskAdresse!!)
                BrevmottakerEntityMapper.RegistrertBrevmottaker.upsertQuery(jdbcTemplate, brevmottakerStegEntity.registrertBrevmottakerEntity.fullmektig!!)
            }
            else -> BrevmottakerEntityMapper.RegistrertBrevmottaker.upsertQuery(jdbcTemplate, brevmottakerStegEntity.registrertBrevmottakerEntity)
        }
    }
}
