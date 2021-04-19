package no.nav.familie.tilbake.service.dokumentbestilling.vedtak

import no.nav.familie.kontrakter.felles.tilbakekreving.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import java.util.ArrayList

object VedtakHjemmel {

    private val Vilkårsvurderingsresultat_MED_FORSETT_ALLTID_RENTER: List<Vilkårsvurderingsresultat> =
            listOf(Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER,
                   Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER)

    fun lagHjemmelstekst(vedtaksresultatstype: Vedtaksresultat,
                         foreldelse: VurdertForeldelse?,
                         vilkårsperioder: Set<Vilkårsvurderingsperiode>,
                         effektForBruker: EffektForBruker,
                         ytelsestype: Ytelsestype,
                         språkkode: Språkkode,
                         visHjemmelForRenter: Boolean): String {
        val foreldetVanlig = erNoeSattTilVanligForeldet(foreldelse)
        val foreldetMedTilleggsfrist = erTilleggsfristBenyttet(foreldelse)
        val ignorerteSmåbeløp = heleVurderingPgaSmåbeløp(vedtaksresultatstype, vilkårsperioder)
        val renter = visHjemmelForRenter && erRenterBenyttet(vilkårsperioder)
        val barnetrygd = Ytelsestype.BARNETRYGD == ytelsestype
        val kontantstøtte = Ytelsestype.KONTANTSTØTTE == ytelsestype
        val hjemler: MutableList<Hjemler> = ArrayList()
        if (vilkårsperioder.isNotEmpty()) {
            when {
                ignorerteSmåbeløp -> hjemler.add(Hjemler.FOLKETRYGD_22_15_SJETTE)
                barnetrygd -> hjemler.add(Hjemler.BARNETRYGD_13_OG_FOLKETRYGD_22_15)
                kontantstøtte -> hjemler.add(Hjemler.KONTANTSTØTTE_11)
                renter -> hjemler.add(Hjemler.FOLKETRYGD_22_15_OG_22_17_A)
                else -> hjemler.add(Hjemler.FOLKETRYGD_22_15)
            }
        }
        if (foreldetMedTilleggsfrist) {
            hjemler.add(Hjemler.FORELDELSE_2_3_OG_10)
        } else if (foreldetVanlig) {
            hjemler.add(Hjemler.FORELDELSE_2_3)
        }
        if (EffektForBruker.ENDRET_TIL_GUNST_FOR_BRUKER == effektForBruker) {
            hjemler.add(Hjemler.FORVALTNING_35_A)
        }
        if (EffektForBruker.ENDRET_TIL_UGUNST_FOR_BRUKER == effektForBruker) {
            hjemler.add(Hjemler.FORVALTNING_35_C)
        }
        return join(hjemler, " og ", språkkode)
    }

    private fun erRenterBenyttet(vilkårPerioder: Set<Vilkårsvurderingsperiode>): Boolean {
        return vilkårPerioder.any {
            it.aktsomhet?.ileggRenter == true || erForsettOgAlltidRenter(it)
        }
    }

    private fun erForsettOgAlltidRenter(v: Vilkårsvurderingsperiode): Boolean {
        return Vilkårsvurderingsresultat_MED_FORSETT_ALLTID_RENTER.contains(v.vilkårsvurderingsresultat)
               && Aktsomhet.FORSETT == v.aktsomhet?.aktsomhet
    }

    private fun heleVurderingPgaSmåbeløp(vedtakResultatType: Vedtaksresultat,
                                         vilkårPerioder: Set<Vilkårsvurderingsperiode>): Boolean {
        return Vedtaksresultat.INGEN_TILBAKEBETALING == vedtakResultatType
               && vilkårPerioder.any { false == it.aktsomhet?.tilbakekrevSmåbeløp }
    }

    private fun erTilleggsfristBenyttet(foreldelse: VurdertForeldelse?): Boolean {
        return foreldelse?.foreldelsesperioder?.any { it.foreldelsesvurderingstype == Foreldelsesvurderingstype.TILLEGGSFRIST }
               ?: false
    }

    private fun erNoeSattTilVanligForeldet(foreldelse: VurdertForeldelse?): Boolean {
        return foreldelse?.foreldelsesperioder?.any { it.foreldelsesvurderingstype == Foreldelsesvurderingstype.FORELDET }
               ?: false
    }

    private fun join(elementer: List<Hjemler>,
                     sisteSkille: String,
                     lokale: Språkkode): String {
        val lokalListe = elementer.map { it.hjemmelTekst(lokale) }
        if (lokalListe.size == 1) {
            return lokalListe.first()!!
        }
        return lokalListe.subList(0, elementer.size - 1).joinToString(", ") + sisteSkille + lokalListe.last()
    }

    enum class EffektForBruker {
        FØRSTEGANGSVEDTAK,
        ENDRET_TIL_GUNST_FOR_BRUKER,
        ENDRET_TIL_UGUNST_FOR_BRUKER
    }

    private enum class Hjemler(bokmål: String, nynorsk: String) {
        FOLKETRYGD_22_15("folketrygdloven § 22-15", "folketrygdlova § 22-15"),
        FOLKETRYGD_22_15_SJETTE("folketrygdloven § 22-15 sjette ledd", "folketrygdlova § 22-15 sjette ledd"),
        FOLKETRYGD_22_15_OG_22_17_A("folketrygdloven §§ 22-15 og 22-17 a", "folketrygdlova §§ 22-15 og 22-17 a"),
        FORELDELSE_2_3_OG_10("foreldelsesloven §§ 2, 3 og 10", "foreldingslova §§ 2, 3 og 10"),
        FORELDELSE_2_3("foreldelsesloven §§ 2 og 3", "foreldingslova §§ 2 og 3"),
        FORVALTNING_35_A("forvaltningsloven § 35 a)", "forvaltningslova § 35 a)"),
        FORVALTNING_35_C("forvaltningsloven § 35 c)", "forvaltningslova § 35 c)"),
        KONTANTSTØTTE_11("kontantstøtteloven § 11", "kontantstøttelova § 11"),
        BARNETRYGD_13_OG_FOLKETRYGD_22_15("barnetrygdloven § 13 og folketrygdloven § 22-15",
                                          "barnetrygdlova § 13 og folketrygdlova § 22-15");

        private val hjemmelTekster = mapOf(Språkkode.NB to bokmål,
                                           Språkkode.NN to nynorsk)

        fun hjemmelTekst(språkkode: Språkkode): String? {
            return hjemmelTekster.getOrDefault(språkkode, hjemmelTekster[Språkkode.NB])
        }

    }
}
