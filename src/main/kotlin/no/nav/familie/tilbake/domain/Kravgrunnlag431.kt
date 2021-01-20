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
                           val fagområdekode: Fagområdekode,
                           val fagsystem: Fagsystem,
                           val fagsystemVedtaksdato: LocalDate?,
                           val gjelderVedtakId: String,
                           val gjelderType: GjelderType,
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

enum class Fagsystem(val offisiellKode: String) {
    SAK("FS36"),
    KSSASkK9SAK("K9"),
    TPS("FS03"),
    JOARK("AS36"),
    INFOTRYGD("IT01"),
    ARENA("AO01"),
    INNTEKT("FS28"),
    MEDL("FS18"),
    GOSYS("FS22"),
    ENHETSREGISTERET("ER01"),
    AAREGISTERET("AR01"),
    FPTILBAKE(""),
    K9TILBAKE("")
}
