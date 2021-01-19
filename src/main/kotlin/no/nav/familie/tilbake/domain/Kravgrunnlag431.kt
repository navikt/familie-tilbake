package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.util.*

data class Kravgrunnlag431(@Id
                           val id: UUID = UUID.randomUUID(),
                           val vedtakId: String,
                           val omgjortVedtakId: String?,
                           val kravstatuskode: String,
                           @Column("fagomradekode")
                           val fagområdekode: String,
                           val fagsystem: String,
                           val fagsystemVedtaksdato: LocalDate?,
                           val gjelderVedtakId: String,
                           val gjelderType: String,
                           val utbetalesTilId: String,
                           val hjemmelkode: String?,
                           val beregnesRenter: Boolean?,
                           val ansvarligEnhet: String,
                           val bostedsenhet: String,
                           val behandlingsenhet: String,
                           val kontrollfelt: String,
                           val saksbehandlerId: String,
                           val referanse: String?,
                           val eksternKravgrunnlagId: String?,
                           @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                           val sporbar: Sporbar = Sporbar())