package no.nav.tilbakekreving.e2e

import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsbeløp.Companion.medFeilutbetaling
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.util.kroner
import no.nav.tilbakekreving.util.prosent
import org.intellij.lang.annotations.Language
import java.math.BigDecimal

object KravgrunnlagGenerator {
    private val idIndexes = Array(10) { 0 }

    fun nextPaddedId(width: Int) = (idIndexes[width - 1]++)
        .toString()
        .padStart(width, '0')

    fun forTilleggsstønader(
        vedtakId: String = nextPaddedId(6),
        fagsystemId: String = nextPaddedId(6),
        kravgrunnlagId: String = nextPaddedId(6),
        referanse: String = nextPaddedId(4),
        ansvarligEnhet: String = nextPaddedId(4),
        fødselsnummer: String = "40026912345",
        perioder: List<Tilbakekrevingsperiode> = listOf(
            Tilbakekrevingsperiode(
                1.januar(2021) til 1.januar(2021),
                tilbakekrevingsbeløp = listOf(
                    Tilbakekrevingsbeløp.forKlassekode(
                        klassekode = NyKlassekode.TSTBASISP4_OP,
                        beløpTilbakekreves = 2000.kroner,
                        beløpOpprinneligUtbetalt = 20000.kroner,
                    ),
                ).medFeilutbetaling(NyKlassekode.KL_KODE_FEIL_ARBYT),
            ),
        ),
    ): String {
        val perioderXML = perioder.joinToString("\n") { it.toXML() }

        @Language("XML")
        val xml = """<?xml version="1.0" encoding="utf-8"?>
        <urn:detaljertKravgrunnlagMelding xmlns:mmel="urn:no:nav:tilbakekreving:typer:v1" xmlns:urn="urn:no:nav:tilbakekreving:kravgrunnlag:detalj:v1">
            <urn:detaljertKravgrunnlag>
                <urn:kravgrunnlagId>$kravgrunnlagId</urn:kravgrunnlagId>
                <urn:vedtakId>$vedtakId</urn:vedtakId>
                <urn:kodeStatusKrav>NY</urn:kodeStatusKrav>
                <urn:kodeFagomraade>TILLST</urn:kodeFagomraade>
                <urn:fagsystemId>$fagsystemId</urn:fagsystemId>
                <urn:vedtakIdOmgjort>0</urn:vedtakIdOmgjort>
                <urn:vedtakGjelderId>$fødselsnummer</urn:vedtakGjelderId>
                <urn:typeGjelderId>PERSON</urn:typeGjelderId>
                <urn:utbetalesTilId>$fødselsnummer</urn:utbetalesTilId>
                <urn:typeUtbetId>PERSON</urn:typeUtbetId>
                <urn:enhetAnsvarlig>$ansvarligEnhet</urn:enhetAnsvarlig>
                <urn:enhetBosted>8020</urn:enhetBosted>
                <urn:enhetBehandl>8020</urn:enhetBehandl>
                <urn:kontrollfelt>2025-12-24-11.12.13.123456</urn:kontrollfelt>
                <urn:saksbehId>Z999999</urn:saksbehId>
                <urn:referanse>$referanse</urn:referanse>
                $perioderXML
            </urn:detaljertKravgrunnlag>
        </urn:detaljertKravgrunnlagMelding>
            """
        return xml.replace(Regex("\n *"), "")
    }

    class Tilbakekrevingsperiode(
        val periode: Datoperiode,
        val beløpSkattMnd: BigDecimal = 0.kroner,
        val tilbakekrevingsbeløp: List<Tilbakekrevingsbeløp>,
    ) {
        @Language("XML")
        fun toXML(): String {
            return """
            <urn:tilbakekrevingsPeriode>
                <urn:periode>
                    <mmel:fom>${periode.fom}</mmel:fom>
                    <mmel:tom>${periode.tom}</mmel:tom>
                </urn:periode>
                <urn:belopSkattMnd>${beløpSkattMnd.setScale(2)}</urn:belopSkattMnd>
                ${tilbakekrevingsbeløp.joinToString("\n") { it.toXML() }}
            </urn:tilbakekrevingsPeriode>
            """
        }
    }

    class Tilbakekrevingsbeløp(
        val beløpOpprinneligUtbetalt: BigDecimal,
        val beløpNy: BigDecimal,
        val beløpTilbakekreves: BigDecimal,
        val beløpUinnkrevd: BigDecimal,
        val skattProsent: BigDecimal,
        val typeKlasse: Klassetype,
        val klassekode: NyKlassekode,
    ) {
        @Language("XML")
        fun toXML(): String {
            return """
            <urn:tilbakekrevingsBelop>
                <urn:kodeKlasse>${klassekode.osNavn()}</urn:kodeKlasse>
                <urn:typeKlasse>${typeKlasse.name}</urn:typeKlasse>
                <urn:belopOpprUtbet>${beløpOpprinneligUtbetalt.setScale(2)}</urn:belopOpprUtbet>
                <urn:belopNy>${beløpNy.setScale(2)}</urn:belopNy>
                <urn:belopTilbakekreves>${beløpTilbakekreves.setScale(2)}</urn:belopTilbakekreves>
                <urn:belopUinnkrevd>${beløpUinnkrevd.setScale(2)}</urn:belopUinnkrevd>
                <urn:skattProsent>${skattProsent.setScale(4)}</urn:skattProsent>
            </urn:tilbakekrevingsBelop>
            """
        }

        companion object {
            fun forKlassekode(
                klassekode: NyKlassekode,
                beløpTilbakekreves: BigDecimal,
                beløpOpprinneligUtbetalt: BigDecimal,
                beløpNy: BigDecimal = beløpOpprinneligUtbetalt - beløpTilbakekreves,
                skattProsent: BigDecimal = BigDecimal.ZERO,
                beløpUinnkrevd: BigDecimal = BigDecimal.ZERO,
            ): Tilbakekrevingsbeløp {
                return Tilbakekrevingsbeløp(
                    beløpOpprinneligUtbetalt = beløpOpprinneligUtbetalt,
                    beløpNy = beløpNy,
                    beløpTilbakekreves = beløpTilbakekreves,
                    beløpUinnkrevd = beløpUinnkrevd,
                    skattProsent = skattProsent,
                    typeKlasse = klassekode.tilhørendeTypeKlase,
                    klassekode = klassekode,
                )
            }

            fun List<Tilbakekrevingsbeløp>.medFeilutbetaling(klassekode: NyKlassekode) = this + forKlassekode(
                klassekode = klassekode,
                beløpTilbakekreves = 0.kroner,
                beløpOpprinneligUtbetalt = 0.kroner,
                beløpNy = sumOf { it.beløpTilbakekreves },
                skattProsent = 0.prosent,
                beløpUinnkrevd = 0.kroner,
            )
        }
    }

    enum class NyKlassekode(val tilhørendeTypeKlase: Klassetype, private val osNavn: String? = null) {
        KL_KODE_FEIL_ARBYT(Klassetype.FEIL),
        TSTBASISP4_OP(Klassetype.YTEL, osNavn = "TSTBASISP4-OP"),
        ;

        fun osNavn() = osNavn ?: name
    }
}
