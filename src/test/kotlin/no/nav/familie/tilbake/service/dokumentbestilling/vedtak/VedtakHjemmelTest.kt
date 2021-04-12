package no.nav.familie.tilbake.service.dokumentbestilling.vedtak

import no.nav.familie.kontrakter.felles.tilbakekreving.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.domain.tbd.Aktsomhet
import no.nav.familie.tilbake.domain.tbd.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.domain.tbd.Vilkårsvurderingsperiode
import no.nav.familie.tilbake.domain.tbd.Vilkårsvurderingsresultat
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesperiode
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class VedtakHjemmelTest {

    var periode: Periode = Periode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

    @Test
    fun `lagHjemmelstekst skal gi riktig hjemmel når det ikke er foreldelse eller renter bokmål`() {
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) { it }

        val hjemmelstekst = VedtakHjemmel.lagHjemmelstekst(Vedtaksresultat.INGEN_TILBAKEBETALING,
                                                           null,
                                                           vurderingPerioder,
                                                           VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
                                                           Ytelsestype.OVERGANGSSTØNAD,
                                                           Språkkode.NB,
                                                           true)

        assertThat(hjemmelstekst).isEqualTo("folketrygdloven § 22-15")
    }

    @Test
    fun `lagHjemmelstekst skal gi riktig hjemmel når det ikke er foreldelse eller renter nynorsk`() {
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) { it }

        val hjemmelstekst = VedtakHjemmel.lagHjemmelstekst(Vedtaksresultat.INGEN_TILBAKEBETALING,
                                                           null,
                                                           vurderingPerioder,
                                                           VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
                                                           Ytelsestype.OVERGANGSSTØNAD,
                                                           Språkkode.NN,
                                                           true)

        assertThat(hjemmelstekst).isEqualTo("folketrygdlova § 22-15")
    }

    @Test
    fun `lagHjemmelstekst skal gi riktig hjemmel når det er forsto burde forstått og forsett`() {
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) {
            it.copy(aktsomhet = Aktsomhet.FORSETT, ileggRenter = false)
        }

        val hjemmelstekst = VedtakHjemmel.lagHjemmelstekst(Vedtaksresultat.INGEN_TILBAKEBETALING,
                                                           null,
                                                           vurderingPerioder,
                                                           VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
                                                           Ytelsestype.OVERGANGSSTØNAD,
                                                           Språkkode.NB,
                                                           true)

        assertThat(hjemmelstekst).isEqualTo("folketrygdloven § 22-15")
    }

    @Test
    fun `lagHjemmelstekst skal gi riktig hjemmel når det er feilaktig opplysninger og forsett`() {
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> =
                aktsomhet(Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER, periode) {
                    it.copy(aktsomhet = Aktsomhet.FORSETT)
                }

        val hjemmelstekst = VedtakHjemmel.lagHjemmelstekst(Vedtaksresultat.INGEN_TILBAKEBETALING,
                                                           null,
                                                           vurderingPerioder,
                                                           VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
                                                           Ytelsestype.OVERGANGSSTØNAD,
                                                           Språkkode.NB,
                                                           true)

        assertThat(hjemmelstekst).isEqualTo("folketrygdloven §§ 22-15 og 22-17 a")
    }

    @Test
    fun `lagHjemmelstekst skal gi riktig hjemmel når det er feilaktig opplysninger og forsett men frisinn og dermed ikke renter`() {
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> =
                aktsomhet(Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER, periode) {
                    it.copy(aktsomhet = Aktsomhet.FORSETT)
                }

        val hjemmelstekst = VedtakHjemmel.lagHjemmelstekst(Vedtaksresultat.INGEN_TILBAKEBETALING,
                                                           null,
                                                           vurderingPerioder,
                                                           VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
                                                           Ytelsestype.OVERGANGSSTØNAD,
                                                           Språkkode.NB,
                                                           false)

        assertThat(hjemmelstekst).isEqualTo("folketrygdloven § 22-15")
    }

    @Test
    fun `lagHjemmelstekst skal gi riktig hjemmel når det ikke kreves tilbake pga lavt beløp bokmål`() {
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) {
            it.copy(tilbakekrevSmåbeløp = false)
        }

        val hjemmelstekst = VedtakHjemmel.lagHjemmelstekst(Vedtaksresultat.INGEN_TILBAKEBETALING,
                                                           null,
                                                           vurderingPerioder,
                                                           VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
                                                           Ytelsestype.OVERGANGSSTØNAD,
                                                           Språkkode.NB,
                                                           true)

        assertThat(hjemmelstekst).isEqualTo("folketrygdloven § 22-15 sjette ledd")
    }

    @Test
    fun `lagHjemmelstekst skal gi riktig hjemmel når det ikke kreves tilbake pga lavt beløp nynorsk`() {
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) {
            it.copy(tilbakekrevSmåbeløp = false)
        }

        val hjemmelstekst = VedtakHjemmel.lagHjemmelstekst(Vedtaksresultat.INGEN_TILBAKEBETALING,
                                                           null,
                                                           vurderingPerioder,
                                                           VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
                                                           Ytelsestype.OVERGANGSSTØNAD,
                                                           Språkkode.NN,
                                                           true)

        assertThat(hjemmelstekst).isEqualTo("folketrygdlova § 22-15 sjette ledd")
    }

    @Test
    fun `lagHjemmelstekst skal gi riktig hjemmel når alt er foreldet`() {
        val vurdertForeldelse = lagForeldelseperiode(periode) {
            it.copy(foreldelsesvurderingstype = Foreldelsesvurderingstype.FORELDET,
                    foreldelsesfrist = periode.fom.plusMonths(11).atDay(1))

        }

        val hjemmelstekst = VedtakHjemmel.lagHjemmelstekst(Vedtaksresultat.INGEN_TILBAKEBETALING,
                                                           vurdertForeldelse,
                                                           emptySet(),
                                                           VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
                                                           Ytelsestype.OVERGANGSSTØNAD,
                                                           Språkkode.NB,
                                                           true)

        assertThat(hjemmelstekst).isEqualTo("foreldelsesloven §§ 2 og 3")
    }

    @Test
    fun `lagHjemmelstekst skal gi riktig hjemmel når noe er foreldet uten tilleggsfrist og ikke renter`() {
        val vurdertForeldelse: VurdertForeldelse = lagForeldelseperiode(periode) {
            it.copy(foreldelsesvurderingstype = Foreldelsesvurderingstype.FORELDET,
                    foreldelsesfrist = periode.fom.plusMonths(11).atDay(1))
        }
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) {
            it.copy(aktsomhet = Aktsomhet.GROV_UAKTSOMHET, ileggRenter = false)
        }

        val hjemmelstekst = VedtakHjemmel.lagHjemmelstekst(Vedtaksresultat.INGEN_TILBAKEBETALING,
                                                           vurdertForeldelse,
                                                           vurderingPerioder,
                                                           VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
                                                           Ytelsestype.OVERGANGSSTØNAD,
                                                           Språkkode.NB,
                                                           true)

        assertThat(hjemmelstekst).isEqualTo("folketrygdloven § 22-15 og foreldelsesloven §§ 2 og 3")
    }

    @Test
    fun `lagHjemmelstekst skal gi riktig hjemmel når foreldelse er vurdert men ikke ilagt uten tilleggsfrist og renter`() {
        val vurdertForeldelse: VurdertForeldelse = lagForeldelseperiode(periode) {
            it.copy(foreldelsesvurderingstype = Foreldelsesvurderingstype.IKKE_FORELDET)
        }
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) {
            it.copy(aktsomhet = Aktsomhet.GROV_UAKTSOMHET, ileggRenter = true)
        }

        val hjemmelstekst = VedtakHjemmel.lagHjemmelstekst(Vedtaksresultat.INGEN_TILBAKEBETALING,
                                                           vurdertForeldelse,
                                                           vurderingPerioder,
                                                           VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
                                                           Ytelsestype.OVERGANGSSTØNAD,
                                                           Språkkode.NB,
                                                           true)

        assertThat(hjemmelstekst).isEqualTo("folketrygdloven §§ 22-15 og 22-17 a")
    }

    @Test
    fun `lagHjemmelstekst skal gi riktig hjemmel når det er både foreldelse med tilleggsfrist og ikke renter`() {
        val vurdertForeldelse: VurdertForeldelse = lagForeldelseperiode(periode) {
            it.copy(foreldelsesvurderingstype = Foreldelsesvurderingstype.TILLEGGSFRIST,
                    foreldelsesfrist = periode.fom.plusMonths(11).atDay(1),
                    oppdagelsesdato = periode.fom.plusMonths(5).atDay(1))
        }
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) {
            it.copy(aktsomhet = Aktsomhet.GROV_UAKTSOMHET, ileggRenter = false)
        }

        val hjemmelstekst = VedtakHjemmel.lagHjemmelstekst(Vedtaksresultat.INGEN_TILBAKEBETALING,
                                                           vurdertForeldelse,
                                                           vurderingPerioder,
                                                           VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
                                                           Ytelsestype.OVERGANGSSTØNAD,
                                                           Språkkode.NB,
                                                           true)

        assertThat(hjemmelstekst).isEqualTo("folketrygdloven § 22-15 og foreldelsesloven §§ 2, 3 og 10")
    }

    @Test
    fun `lagHjemmelstekst skal gi riktig hjemmel når det er både foreldelse med tilleggsfrist og renter`() {
        val vurdertForeldelse: VurdertForeldelse = lagForeldelseperiode(periode) {
            it.copy(foreldelsesvurderingstype = Foreldelsesvurderingstype.TILLEGGSFRIST,
                    foreldelsesfrist = periode.fom.plusMonths(11).atDay(1),
                    oppdagelsesdato = periode.fom.plusMonths(5).atDay(1))

        }
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) {
            it.copy(aktsomhet = Aktsomhet.GROV_UAKTSOMHET, ileggRenter = true)
        }

        val hjemmelstekst = VedtakHjemmel.lagHjemmelstekst(Vedtaksresultat.INGEN_TILBAKEBETALING,
                                                           vurdertForeldelse,
                                                           vurderingPerioder,
                                                           VedtakHjemmel.EffektForBruker.FØRSTEGANGSVEDTAK,
                                                           Ytelsestype.OVERGANGSSTØNAD,
                                                           Språkkode.NB,
                                                           true)

        assertThat(hjemmelstekst).isEqualTo("folketrygdloven §§ 22-15 og 22-17 a og foreldelsesloven §§ 2, 3 og 10")
    }

    @Test
    fun `lagHjemmelstekst skal gi riktig hjemmel når det ikke er foreldelse eller renter er klage`() {
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) { it }

        val hjemmelstekst = VedtakHjemmel.lagHjemmelstekst(Vedtaksresultat.INGEN_TILBAKEBETALING,
                                                           null,
                                                           vurderingPerioder,
                                                           VedtakHjemmel.EffektForBruker.ENDRET_TIL_UGUNST_FOR_BRUKER,
                                                           Ytelsestype.OVERGANGSSTØNAD,
                                                           Språkkode.NB,
                                                           true)

        assertThat(hjemmelstekst).isEqualTo("folketrygdloven § 22-15 og forvaltningsloven § 35 c)")
    }

    @Test
    fun `lagHjemmelstekst skal gi riktig hjemmel når det er både foreldelse med tilleggsfrist og renter er klage`() {
        val vurdertForeldelse: VurdertForeldelse = lagForeldelseperiode(periode) {
            it.copy(foreldelsesvurderingstype = Foreldelsesvurderingstype.TILLEGGSFRIST,
                    foreldelsesfrist = periode.fom.plusMonths(11).atDay(1),
                    oppdagelsesdato = periode.fom.plusMonths(5).atDay(1))
        }
        val vurderingPerioder: Set<Vilkårsvurderingsperiode> = aktsomhet(periode) {
            it.copy(aktsomhet = Aktsomhet.GROV_UAKTSOMHET, ileggRenter = true)
        }

        val hjemmelstekst = VedtakHjemmel.lagHjemmelstekst(Vedtaksresultat.INGEN_TILBAKEBETALING,
                                                           vurdertForeldelse,
                                                           vurderingPerioder,
                                                           VedtakHjemmel.EffektForBruker.ENDRET_TIL_GUNST_FOR_BRUKER,
                                                           Ytelsestype.OVERGANGSSTØNAD,
                                                           Språkkode.NB,
                                                           true)

        assertThat(hjemmelstekst)
                .isEqualTo("folketrygdloven §§ 22-15 og 22-17 a, foreldelsesloven §§ 2, 3 og 10 og forvaltningsloven § 35 a)")
    }

    private fun lagForeldelseperiode(periode: Periode,
                                     oppsett: (Foreldelsesperiode) -> Foreldelsesperiode): VurdertForeldelse {
        val periodeBuilder = Foreldelsesperiode(periode = periode,
                                                foreldelsesvurderingstype = Foreldelsesvurderingstype.IKKE_VURDERT,
                                                begrunnelse = "bob")
        return VurdertForeldelse(behandlingId = UUID.randomUUID(),
                                 foreldelsesperioder = setOf(oppsett(periodeBuilder)))
    }

    private fun aktsomhet(periode: Periode,
                          oppsett: (VilkårsvurderingAktsomhet) -> VilkårsvurderingAktsomhet): Set<Vilkårsvurderingsperiode> {
        return aktsomhet(Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT, periode, oppsett)
    }

    private fun aktsomhet(resultat: Vilkårsvurderingsresultat,
                          periode: Periode,
                          oppsett: (VilkårsvurderingAktsomhet) -> VilkårsvurderingAktsomhet): Set<Vilkårsvurderingsperiode> {
        val aktsomhet: VilkårsvurderingAktsomhet =
                oppsett(VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET, begrunnelse = "foo"))
        val vurderingPeriode = Vilkårsvurderingsperiode(periode = periode,
                                                        vilkårsvurderingsresultat = resultat,
                                                        begrunnelse = "foo",
                                                        aktsomhet = aktsomhet)

        return setOf(vurderingPeriode.copy(aktsomhet = aktsomhet))
    }
}