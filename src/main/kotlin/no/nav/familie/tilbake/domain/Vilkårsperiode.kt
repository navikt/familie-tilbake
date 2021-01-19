package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.util.*

@Table("vilkarsperiode")
data class Vilkårsperiode(@Id
                          val id: UUID = UUID.randomUUID(),
                          @Column("vilkar_id")
                          val vilkårId: UUID,
                          val fom: LocalDate,
                          val tom: LocalDate,
                          val fulgtOppNav: String,
                          @Column("vilkarsresultat")
                          val vilkårsresultat: String,
                          val begrunnelse: String,
                          @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                          val sporbar: Sporbar = Sporbar())