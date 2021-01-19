package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("kravgrunnlagsbelop433")
data class Kravgrunnlagsbeløp433(@Id
                                 val id: UUID = UUID.randomUUID(),
                                 val kravgrunnlagsperiode432Id: UUID,
                                 val klassekode: String,
                                 val klassetype: String,
                                 @Column("opprinnelig_utbetalingsbelop")
                                 val opprinneligUtbetalingsbeløp: Double?,
                                 @Column("nytt_belop")
                                 val nyttBeløp: Double,
                                 @Column("tilbakekreves_belop")
                                 val tilbakekrevesBeløp: Double?,
                                 @Column("uinnkrevd_belop")
                                 val uinnkrevdBeløp: Double?,
                                 val resultatkode: String?,
                                 @Column("arsakskode")
                                 val årsakskode: String?,
                                 val skyldkode: String?,
                                 val skatteprosent: Double,
                                 @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                 val sporbar: Sporbar = Sporbar())