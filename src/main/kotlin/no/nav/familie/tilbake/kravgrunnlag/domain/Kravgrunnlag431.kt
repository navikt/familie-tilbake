package no.nav.familie.tilbake.kravgrunnlag.domain

import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class Kravgrunnlag431(@Id
                           val id: UUID = UUID.randomUUID(),
                           val behandlingId: UUID,
                           val aktiv: Boolean = true,
                           val sperret: Boolean = false,
                           val vedtakId: String,
                           val omgjortVedtakId: String?,
                           val kravstatuskode: Kravstatuskode,
                           @Column("fagomradekode")
                           val fagområdekode: Fagområdekode,
                           val fagsystemId: String,
                           val fagsystemVedtaksdato: LocalDate?,
                           val gjelderVedtakId: String,
                           val gjelderType: GjelderType,
                           val utbetalesTilId: String,
                           val utbetIdType: GjelderType,
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
                           @Version
                           val versjon: Long = 0,
                           @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                           val sporbar: Sporbar = Sporbar())

data class Kravgrunnlagsperiode432(@Id
                                   val id: UUID = UUID.randomUUID(),
                                   @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                   val periode: Periode,
                                   @Column("manedlig_skattebelop")
                                   val månedligSkattebeløp: BigDecimal,
                                   @MappedCollection(idColumn = "kravgrunnlagsperiode432_id")
                                   val beløp: Set<Kravgrunnlagsbeløp433> = setOf(),
                                   @Version
                                   val versjon: Long = 0,
                                   @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                   val sporbar: Sporbar = Sporbar())


@Table("kravgrunnlagsbelop433")
data class Kravgrunnlagsbeløp433(@Id
                                 val id: UUID = UUID.randomUUID(),
                                 val klassekode: Klassekode,
                                 val klassetype: Klassetype,
                                 @Column("opprinnelig_utbetalingsbelop")
                                 val opprinneligUtbetalingsbeløp: BigDecimal,
                                 @Column("nytt_belop")
                                 val nyttBeløp: BigDecimal,
                                 @Column("tilbakekreves_belop")
                                 val tilbakekrevesBeløp: BigDecimal,
                                 @Column("uinnkrevd_belop")
                                 val uinnkrevdBeløp: BigDecimal?,
                                 val resultatkode: String?,
                                 @Column("arsakskode")
                                 val årsakskode: String?,
                                 val skyldkode: String?,
                                 val skatteprosent: BigDecimal,
                                 @Version
                                 val versjon: Long = 0,
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
