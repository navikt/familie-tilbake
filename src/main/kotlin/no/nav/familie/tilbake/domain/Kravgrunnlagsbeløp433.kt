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
