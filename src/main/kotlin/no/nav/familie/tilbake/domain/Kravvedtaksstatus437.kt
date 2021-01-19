package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class Kravvedtaksstatus437(@Id
                                val id: UUID = UUID.randomUUID(),
                                val vedtakId: String,
                                val kravstatuskode: String,
                                @Column("fagomradekode")
                                val fagområdekode: String,
                                val fagsystemId: String,
                                val gjelderVedtakId: String,
                                val gjelderType: String,
                                val referanse: String?,
                                @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                val sporbar: Sporbar = Sporbar())