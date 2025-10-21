package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.GodTroEntity
import no.nav.tilbakekreving.entities.SærligeGrunnerEntity
import no.nav.tilbakekreving.entities.VilkårsvurderingsperiodeEntity
import no.nav.tilbakekreving.entities.VilkårsvurderingstegEntity
import no.nav.tilbakekreving.entities.VurdertAktsomhetEntity
import no.nav.tilbakekreving.entity.Entity.Companion.get
import no.nav.tilbakekreving.entity.VilkårsvurderingEntityMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class NyVilkårsvurderingRepository(private val jdbcTemplate: JdbcTemplate) {
    fun lagre(vilkårsvurdering: VilkårsvurderingstegEntity) {
        VilkårsvurderingEntityMapper.upsertQuery(jdbcTemplate, vilkårsvurdering)
        jdbcTemplate.update("DELETE FROM tilbakekreving_vilkårsvurdering_periode WHERE vurdering_ref=?;", vilkårsvurdering.id)
        lagreVurderinger(vilkårsvurdering.vurderinger)
    }

    fun hentVilkårsvurdering(behandlingId: UUID): VilkårsvurderingstegEntity? {
        return jdbcTemplate.query("SELECT * FROM tilbakekreving_vilkårsvurdering WHERE behandling_ref=?;", behandlingId) { resultSet, _ ->
            val vurderingId = resultSet[VilkårsvurderingEntityMapper.id]
            VilkårsvurderingEntityMapper.map(resultSet, hentVurdertePerioder(vurderingId))
        }.singleOrNull()
    }

    fun lagreVurderinger(perioder: List<VilkårsvurderingsperiodeEntity>) {
        for (periode in perioder) {
            VilkårsvurderingEntityMapper.VilkårsvurdertPeriodeEntityMapper.upsertQuery(jdbcTemplate, periode)
            if (periode.vurdering.aktsomhet != null) {
                lagreAktsomhet(periode.vurdering.aktsomhet!!)
            }
            if (periode.vurdering.beløpIBehold != null) {
                lagreGodTro(periode.vurdering.beløpIBehold!!)
            }
        }
    }

    private fun hentVurdertePerioder(vurderingId: UUID): List<VilkårsvurderingsperiodeEntity> {
        return jdbcTemplate.query("SELECT * FROM tilbakekreving_vilkårsvurdering_periode WHERE vurdering_ref=?", vurderingId) { resultSet, _ ->
            val periodeId = resultSet[VilkårsvurderingEntityMapper.VilkårsvurdertPeriodeEntityMapper.id]
            VilkårsvurderingEntityMapper.VilkårsvurdertPeriodeEntityMapper.map(
                resultSet,
                hentGodTroVurdering(periodeId),
                hentAktsomhetvurdering(periodeId),
            )
        }
    }

    private fun lagreGodTro(godTro: GodTroEntity) {
        VilkårsvurderingEntityMapper.GodTroEntityMapper.upsertQuery(jdbcTemplate, godTro)
    }

    private fun hentGodTroVurdering(periodeId: UUID): GodTroEntity? {
        return jdbcTemplate.query("SELECT * FROM tilbakekreving_vilkårsvurdering_periode_god_tro WHERE id=?;", periodeId) { resultSet, _ ->
            VilkårsvurderingEntityMapper.GodTroEntityMapper.map(resultSet)
        }.singleOrNull()
    }

    private fun hentAktsomhetvurdering(periodeId: UUID): VurdertAktsomhetEntity? {
        return jdbcTemplate.query("SELECT * FROM tilbakekreving_vilkårsvurdering_periode_aktsomhet WHERE id=?;", periodeId) { resultSet, _ ->
            VilkårsvurderingEntityMapper.AktsomhetMapper.map(
                resultSet,
                hentSærligeGrunnerVurdering(periodeId),
            )
        }.singleOrNull()
    }

    private fun lagreAktsomhet(aktsomhet: VurdertAktsomhetEntity) {
        VilkårsvurderingEntityMapper.AktsomhetMapper.upsertQuery(jdbcTemplate, aktsomhet)
        if (aktsomhet.særligGrunner != null) {
            lagreSærligeGrunner(aktsomhet.særligGrunner!!)
        }
    }

    private fun lagreSærligeGrunner(særligGrunner: SærligeGrunnerEntity) {
        VilkårsvurderingEntityMapper.SærligeGrunnerMapper.upsertQuery(jdbcTemplate, særligGrunner)
    }

    private fun hentSærligeGrunnerVurdering(periodeId: UUID): SærligeGrunnerEntity? {
        return jdbcTemplate.query("SELECT * FROM tilbakekreving_vilkårsvurdering_periode_særlige_grunner WHERE id=?;", periodeId) { resultSet, _ ->
            VilkårsvurderingEntityMapper.SærligeGrunnerMapper.map(resultSet)
        }.singleOrNull()
    }
}
