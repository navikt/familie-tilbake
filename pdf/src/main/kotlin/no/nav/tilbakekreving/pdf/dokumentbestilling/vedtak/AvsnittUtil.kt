package no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak

import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevFelles
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevPeriodeOgFelles
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevsdata
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbKravgrunnlag
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbResultat
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbVedtaksbrevsperiode
import no.nav.tilbakekreving.pdf.handlebars.FellesTekstformaterer
import java.math.BigDecimal

object AvsnittUtil {
    const val PARTIAL_PERIODE_FAKTA = "vedtak/periode_fakta"
    const val PARTIAL_PERIODE_FAKTA_SAMMENSLÅTT_PERIODER = "vedtak/periode-fakta/periode_fakta_ef_sammenslå_perioder"
    const val PARTIAL_PERIODE_FORELDELSE = "vedtak/periode_foreldelse"
    const val PARTIAL_PERIODE_VILKÅR = "vedtak/periode_vilkår"
    const val PARTIAL_PERIODE_VILKÅR_SAMMENSLÅTT = "vedtak/periode_vilkår_sammenslått"
    const val PARTIAL_PERIODE_SÆRLIGE_GRUNNER = "vedtak/periode_særlige_grunner"
    const val PARTIAL_PERIODE_SÆRLIGE_GRUNNER_SAMMENSLÅTT = "vedtak/periode_særlige_grunner_sammenslått"

    fun lagVedtaksbrevDeltIAvsnitt(
        vedtaksbrevsdata: HbVedtaksbrevsdata,
        hovedoverskrift: String,
        skalSammenslåPerioder: Boolean = false,
    ): List<Avsnitt> {
        val resultat: MutableList<Avsnitt> = ArrayList()
        val vedtaksbrevsdataMedFriteksmarkeringer = Vedtaksbrevsfritekst.settInnMarkeringForFritekst(vedtaksbrevsdata)
        resultat.add(lagOppsummeringsavsnitt(vedtaksbrevsdataMedFriteksmarkeringer, hovedoverskrift))
        if (vedtaksbrevsdata.felles.vedtaksbrevstype == Vedtaksbrevstype.ORDINÆR) {
            resultat.addAll(lagPerioderavsnitt(vedtaksbrevsdataMedFriteksmarkeringer, skalSammenslåPerioder))
        }
        resultat.add(lagAvsluttendeAvsnitt(vedtaksbrevsdataMedFriteksmarkeringer))
        return resultat
    }

    private fun lagOppsummeringsavsnitt(
        vedtaksbrevsdata: HbVedtaksbrevsdata,
        hovedoverskrift: String,
    ): Avsnitt {
        val tekst = lagVedtaksstart(vedtaksbrevsdata.felles)
        val avsnitt = Avsnitt(avsnittstype = Avsnittstype.OPPSUMMERING, overskrift = hovedoverskrift)
        return parseTekst(tekst, avsnitt, null)
    }

    private fun lagVedtaksstart(vedtaksbrevFelles: HbVedtaksbrevFelles): String {
        val filsti =
            when (vedtaksbrevFelles.vedtaksbrevstype) {
                Vedtaksbrevstype.FRITEKST_FEILUTBETALING_BORTFALT ->
                    "vedtak/fritekstFeilutbetalingBortfalt/fritekstFeilutbetalingBortfalt_start"
                Vedtaksbrevstype.ORDINÆR, Vedtaksbrevstype.AUTOMATISK_4X_RETTSGEBYR -> "vedtak/vedtak_start"
            }
        return FellesTekstformaterer.lagDeltekst(vedtaksbrevFelles, filsti)
    }

    private fun lagPerioderavsnitt(
        vedtaksbrevsdata: HbVedtaksbrevsdata,
        skalSammenslåPerioder: Boolean,
    ): List<Avsnitt> {
        if (skalSammenslåPerioder) {
            return listOf(lagSammenslåttPeriodeAvsnitt(vedtaksbrevsdata))
        } else {
            return vedtaksbrevsdata.perioder.map {
                lagPeriodeAvsnitt(HbVedtaksbrevPeriodeOgFelles(vedtaksbrevsdata.felles, it))
            }
        }
    }

    private fun lagAvsluttendeAvsnitt(vedtaksbrevsdata: HbVedtaksbrevsdata): Avsnitt {
        val tekst = FellesTekstformaterer.lagDeltekst(vedtaksbrevsdata, "vedtak/vedtak_slutt")
        val avsnitt = Avsnitt(avsnittstype = Avsnittstype.TILLEGGSINFORMASJON)
        return parseTekst(tekst, avsnitt, null)
    }

    private fun lagPeriodeAvsnitt(data: HbVedtaksbrevPeriodeOgFelles): Avsnitt {
        val overskrift = FellesTekstformaterer.lagDeltekst(data, "vedtak/periode_overskrift")
        val faktatekst = FellesTekstformaterer.lagDeltekst(data, PARTIAL_PERIODE_FAKTA)
        val foreldelsestekst = FellesTekstformaterer.lagDeltekst(data, PARTIAL_PERIODE_FORELDELSE)
        val vilkårstekst = FellesTekstformaterer.lagDeltekst(data, PARTIAL_PERIODE_VILKÅR)
        val særligeGrunnerstekst = FellesTekstformaterer.lagDeltekst(data, PARTIAL_PERIODE_SÆRLIGE_GRUNNER)
        val avsluttendeTekst = FellesTekstformaterer.lagDeltekst(data, "vedtak/periode_slutt")
        var avsnitt =
            Avsnitt(
                avsnittstype = Avsnittstype.PERIODE,
                fom = data.periode.periode.fom,
                tom = data.periode.periode.tom,
                overskrift = fjernOverskriftFormattering(overskrift),
            )

        avsnitt = parseTekst(faktatekst, avsnitt, Underavsnittstype.FAKTA)
        avsnitt = parseTekst(foreldelsestekst, avsnitt, Underavsnittstype.FORELDELSE)
        avsnitt = parseTekst(vilkårstekst, avsnitt, Underavsnittstype.VILKÅR)
        avsnitt = parseTekst(særligeGrunnerstekst, avsnitt, Underavsnittstype.SÆRLIGEGRUNNER)
        avsnitt = parseTekst(avsluttendeTekst, avsnitt, null)
        return avsnitt
    }

    private fun lagSammenslåttPeriodeAvsnitt(vedtaksbrevsdata: HbVedtaksbrevsdata): Avsnitt {
        val førstePeriode = vedtaksbrevsdata.perioder.first()
        val sammenslåttDatoperiode =
            Datoperiode(
                førstePeriode.periode.fom,
                vedtaksbrevsdata.perioder
                    .last()
                    .periode.tom,
            )
        val totalTilbakekrevesBeløp = vedtaksbrevsdata.perioder.sumOf { it.resultat.tilbakekrevesBeløp }
        val totalrente = vedtaksbrevsdata.perioder.sumOf { it.resultat.rentebeløp }
        val totalForeldetBeløp = vedtaksbrevsdata.perioder.sumOf { it.resultat.foreldetBeløp ?: BigDecimal.ZERO }
        val totalTilbakekrevesBeløpUtenSkattMedRenter = vedtaksbrevsdata.perioder.sumOf { it.resultat.tilbakekrevesBeløpUtenSkattMedRenter }
        val totalFeilutbetaltBeløp = vedtaksbrevsdata.perioder.sumOf { it.kravgrunnlag.feilutbetaltBeløp }
        val sammenslåttPeriodeKravgrunnlag =
            HbKravgrunnlag(
                riktigBeløp = førstePeriode.kravgrunnlag.riktigBeløp,
                utbetaltBeløp = førstePeriode.kravgrunnlag.utbetaltBeløp,
                feilutbetaltBeløp = totalFeilutbetaltBeløp,
            )
        val sammenslåttResultat = HbResultat(totalTilbakekrevesBeløp, totalrente, totalForeldetBeløp, totalTilbakekrevesBeløpUtenSkattMedRenter)
        val hbVedtaksbrevsperiode = HbVedtaksbrevsperiode(sammenslåttDatoperiode, sammenslåttPeriodeKravgrunnlag, førstePeriode.fakta, førstePeriode.vurderinger, sammenslåttResultat, true, førstePeriode.grunnbeløp)
        val hbVedtaksbrevPeriodeOgFelles = HbVedtaksbrevPeriodeOgFelles(vedtaksbrevsdata.felles, hbVedtaksbrevsperiode)
        var avsnitt =
            Avsnitt(
                avsnittstype = Avsnittstype.SAMMENSLÅTT_PERIODE,
                overskrift = "Perioder som er feilutbetalt",
                fom = førstePeriode.periode.fom,
                tom = førstePeriode.periode.tom,
            )

        val faktatekst = FellesTekstformaterer.lagDeltekst(vedtaksbrevsdata, PARTIAL_PERIODE_FAKTA_SAMMENSLÅTT_PERIODER)
        val foreldelsestekst = FellesTekstformaterer.lagDeltekst(hbVedtaksbrevPeriodeOgFelles, PARTIAL_PERIODE_FORELDELSE)
        val vilkårstekst = FellesTekstformaterer.lagDeltekst(hbVedtaksbrevPeriodeOgFelles, PARTIAL_PERIODE_VILKÅR_SAMMENSLÅTT)
        val særligeGrunnerstekst = FellesTekstformaterer.lagDeltekst(hbVedtaksbrevPeriodeOgFelles, PARTIAL_PERIODE_SÆRLIGE_GRUNNER_SAMMENSLÅTT)
        val avsluttendeTekst = FellesTekstformaterer.lagDeltekst(hbVedtaksbrevPeriodeOgFelles, "vedtak/periode_slutt_sammenslått")

        avsnitt = parseTekst(faktatekst, avsnitt, Underavsnittstype.FAKTA)
        avsnitt = parseTekst(foreldelsestekst, avsnitt, Underavsnittstype.FORELDELSE)
        avsnitt = parseTekst(vilkårstekst, avsnitt, Underavsnittstype.VILKÅR)
        avsnitt = parseTekst(særligeGrunnerstekst, avsnitt, Underavsnittstype.SÆRLIGEGRUNNER)
        avsnitt = parseTekst(avsluttendeTekst, avsnitt, null)

        return avsnitt
    }

    fun parseTekst(
        generertTekst: String,
        avsnitt: Avsnitt,
        underavsnittstype: Underavsnittstype?,
    ): Avsnitt {
        var lokaltAvsnitt = avsnitt
        var lokalUnderavsnittstype = underavsnittstype
        val splittet = generertTekst.split("\r?\n".toRegex()).toMutableList()
        if (avsnitt.overskrift.isNullOrBlank() && erOverskrift(splittet.first())) {
            val linje: String = splittet.removeAt(0)
            lokaltAvsnitt = avsnitt.copy(overskrift = fjernOverskriftFormattering(linje))
        }
        var leserFritekst = false
        var fritekstPåkrevet = false
        var fritekstTillatt = false
        var overskrift: String? = null
        var fritekst: MutableList<String> = ArrayList()
        var brødtekst: String? = null
        val underavsnitt = mutableListOf<Underavsnitt>()

        for (linje in splittet) {
            fun nyOverskriftOgAvsnittetHarAlleredeOverskrift(linje: String) = erOverskrift(linje) && overskrift != null

            fun avsnittHarBrødtekstSomIkkeEtterfølgesAvFritekst(linje: String) = brødtekst != null && !Vedtaksbrevsfritekst.erFritekstStart(linje)

            if (!leserFritekst &&
                (
                    fritekstTillatt ||
                        nyOverskriftOgAvsnittetHarAlleredeOverskrift(linje) ||
                        avsnittHarBrødtekstSomIkkeEtterfølgesAvFritekst(linje)
                )
            ) {
                underavsnitt.add(
                    Underavsnitt(
                        overskrift,
                        brødtekst,
                        fritekst.joinToString("\n"),
                        fritekstTillatt,
                        fritekstPåkrevet,
                        lokalUnderavsnittstype,
                    ),
                )
                overskrift = null
                brødtekst = null
                fritekstTillatt = false
                fritekstPåkrevet = false
                fritekst = ArrayList()
            }

            when {
                Vedtaksbrevsfritekst.erFritekstStart(linje) -> {
                    check(!leserFritekst) { "Feil med vedtaksbrev, har markering for 2 fritekst-start etter hverandre" }
                    fritekstPåkrevet = Vedtaksbrevsfritekst.erFritekstPåkrevetStart(linje)
                    lokalUnderavsnittstype = parseUnderavsnittstype(linje)
                    fritekstTillatt = true
                    leserFritekst = true
                }
                Vedtaksbrevsfritekst.erFritekstSlutt(linje) -> {
                    check(leserFritekst) { "Feil med vedtaksbrev, fikk markering for fritekst-slutt før fritekst-start" }
                    leserFritekst = false
                }
                leserFritekst -> {
                    fritekst.add(linje)
                }
                erOverskrift(linje) -> {
                    overskrift = fjernOverskriftFormattering(linje)
                }
                linje.isNotBlank() -> {
                    brødtekst = fjernFormattering(linje)
                }
            }
        }

        if (overskrift != null || brødtekst != null || fritekstTillatt) {
            underavsnitt.add(
                Underavsnitt(
                    overskrift,
                    brødtekst,
                    fritekst.joinToString("\n"),
                    fritekstTillatt,
                    fritekstPåkrevet,
                    lokalUnderavsnittstype,
                ),
            )
        }

        return lokaltAvsnitt.copy(underavsnittsliste = lokaltAvsnitt.underavsnittsliste + underavsnitt)
    }

    private fun fjernFormattering(linje: String): String = linje.removePrefix("{venstrejustert}").replace("{høyrejustert}", "\t\t\t")

    private fun parseUnderavsnittstype(tekst: String): Underavsnittstype? {
        val rest = Vedtaksbrevsfritekst.fjernFritekstmarkering(tekst)
        return Underavsnittstype.values().firstOrNull { it.name == rest }
    }

    private fun erOverskrift(tekst: String): Boolean = tekst.startsWith("_")

    private fun fjernOverskriftFormattering(tekst: String): String = tekst.removePrefix("__").removePrefix("_")
}
