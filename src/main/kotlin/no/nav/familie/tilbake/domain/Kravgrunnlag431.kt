package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
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
                           @MappedCollection(idColumn = "kravgrunnlag431_id")
                           val perioder: Set<Kravgrunnlagsperiode432> = setOf(),
                           @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                           val sporbar: Sporbar = Sporbar())

data class Kravgrunnlagsperiode432(@Id
                                   val id: UUID = UUID.randomUUID(),
                                   val fom: LocalDate,
                                   val tom: LocalDate,
                                   @Column("manedlig_skattebelop")
                                   val månedligSkattebeløp: Double,
                                   @MappedCollection(idColumn = "kravgrunnlagsperiode432_id")
                                   val beløp: Set<Kravgrunnlagsbeløp433> = setOf(),
                                   @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                   val sporbar: Sporbar = Sporbar())

@Table("kravgrunnlagsbelop433")
data class Kravgrunnlagsbeløp433(@Id
                                 val id: UUID = UUID.randomUUID(),
                                 val klassekode: Klassekode,
                                 val klassetype: Klassetype,
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

enum class Klassekode {
    FPATAL,
    FPATFER,
    FPATFRI,
    FPATORD,
    FPREFAGFER_IOP,
    FPREFAG_IOP,
    FPSNDDM_OP,
    FPSNDFI,
    FPSNDJB_OP,
    FPSND_OP,
    FSKTSKAT,
    KL_KODE_FEIL_KORTTID,
    TBMOTOBS,
    SPSND100D1DAGPFI,
    SPSND100D1DTRPFI,
    FPADATORD,
    FPADATFRI,
    FPADSND_OP,
    FPADATAL,
    FPADATSJO,
    FPADSNDDM_OP,
    FPADSNDJB_OP,
    FPADSNDFI,
    FPATSJO,
    FPADREFAG_IOP,
    FPADREFAGFER_IOP,
    FPENAD_OP,
    FPENFOD_OP,
    KL_KODE_FEIL_REFUTG,
    FPSVATORD,
    FPSVATFRI,
    FPSVSND_OP,
    FPSVATAL,
    FPSVATSJO,
    FPSVSNDDM_OP,
    FPSVSNDJB_OP,
    FPSVSNDFI,
    FPSVREFAG_IOP,
    FPSVREFAGFER_IOP,
    KL_KODE_JUST_KORTTID,
    FRISINN_FRILANS,
    FRISINN_SELVST_OP,
    KL_KODE_FEIL_KORONA,
    OMATAL,
    OMATFRI,
    OMATORD,
    OMATSJO,
    OMREFAG_IOP,
    OMSND_OP,
    OMSNDDM_OP,
    OMSNDFI,
    OMSNDJB_OP,
    OPPATAL,
    OPPATFRI,
    OPPATORD,
    OPPATSJO,
    OPPREFAG_IOP,
    OPPSND_OP,
    OPPSNDDM_OP,
    OPPSNDFI,
    OPPSNDJB_OP,
    PNBSATAL,
    PNBSATFRI,
    PNBSATORD,
    PNBSATSJO,
    PNBSREFAG_IOP,
    PNBSSND_OP,
    PNBSSNDDM_OP,
    PNBSSNDFI,
    PNBSSNDJB_OP,
    PPNPATAL,
    PPNPATFRI,
    PPNPATORD,
    PPNPATSJO,
    PPNPREFAG_IOP,
    PPNPSND_OP,
    PPNPSNDDM_OP,
    PPNPSNDFI,
    PPNPSNDJB_OP,
    SPATFER,
    SPREFAGFERPP_IOP,
    UDEFINERT
}

enum class Klassetype(val navn: String) {
    FEIL("Feilkonto"),
    JUST("Justeringskonto"),
    SKAT("Skatt"),
    TREK("Trekk"),
    YTEL("Ytelseskonto")
}

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
