package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("vilkar_aktsomhet")
data class VilkårAktsomhet(@Id
                           val id: UUID = UUID.randomUUID(),
                           @Column("vilkarsperiode_id")
                           val vilkårsperiodeId: UUID,
                           val aktsomhet: String,
                           val ileggRenter: Boolean?,
                           val andelTilbakekreves: Double?,
                           @Column("manuelt_satt_belop")
                           val manueltSattBeløp: Long?,
                           val begrunnelse: String,
                           @Column("serlige_grunner_til_reduksjon")
                           val særligeGrunnerTilReduksjon: Boolean?,
                           @Column("tilbakekrev_smabelop")
                           val tilbakekrevSmabeløp: Boolean?,
                           @Column("serlige_grunner_begrunnelse")
                           val særligeGrunnerBegrunnelse: String?,
                           @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                           val sporbar: Sporbar = Sporbar())