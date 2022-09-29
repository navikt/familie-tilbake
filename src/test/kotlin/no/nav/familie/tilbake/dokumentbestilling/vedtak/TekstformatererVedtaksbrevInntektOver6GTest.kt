package no.nav.familie.tilbake.dokumentbestilling.vedtak

import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.dokumentbestilling.vedtak.HbUtils.hbGrunnbeløpsperiode
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbBehandling
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbHjemmel
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbKonfigurasjon
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbPerson
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbTotalresultat
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVarsel
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevFelles
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevPeriodeOgFelles
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbFakta
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbGrunnbeløpsperiode
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbKravgrunnlag
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbResultatTestBuilder
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbSærligeGrunner
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbVedtaksbrevsperiode
import no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode.HbVurderinger
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.Scanner

class TekstformatererVedtaksbrevInntektOver6GTest {

    private val januar = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

    private val brevmetadata = Brevmetadata(
        sakspartId = "123456",
        sakspartsnavn = "Test",
        mottageradresse = Adresseinfo("ident", "bob"),
        behandlendeEnhetsNavn = "NAV Familie- og pensjonsytelser Skien",
        ansvarligSaksbehandler = "Ansvarlig Saksbehandler",
        saksnummer = "1232456",
        språkkode = Språkkode.NB,
        ytelsestype = Ytelsestype.OVERGANGSSTØNAD,
        gjelderDødsfall = false
    )

    private val felles =
        HbVedtaksbrevFelles(
            brevmetadata = brevmetadata,
            hjemmel = HbHjemmel("Folketrygdloven"),
            totalresultat = HbTotalresultat(
                Vedtaksresultat.FULL_TILBAKEBETALING,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(11000),
                BigDecimal.valueOf(11000),
                BigDecimal.valueOf(1000)
            ),
            varsel = HbVarsel(
                varsletBeløp = BigDecimal.valueOf(10000),
                varsletDato = LocalDate.now().minusDays(100)
            ),
            konfigurasjon = HbKonfigurasjon(klagefristIUker = 6),
            søker = HbPerson(
                navn = "Søker Søkersen"
            ),
            fagsaksvedtaksdato = LocalDate.now(),
            behandling = HbBehandling(),
            totaltFeilutbetaltBeløp = BigDecimal.valueOf(10000),
            vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
            ansvarligBeslutter = "Ansvarlig Beslutter"
        )

    private val fakta = HbFakta(Hendelsestype.STØNAD_TIL_BARNETILSYN, Hendelsesundertype.INNTEKT_OVER_6G, null, hbGrunnbeløpsperiode)

    private val periode =
        HbVedtaksbrevsperiode(
            periode = januar,
            kravgrunnlag = HbKravgrunnlag.forFeilutbetaltBeløp(BigDecimal(30001)),
            fakta = fakta,
            vurderinger = HbVurderinger(
                foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat
                    .MANGELFULLE_OPPLYSNINGER_FRA_BRUKER,
                aktsomhetsresultat = Aktsomhet.SIMPEL_UAKTSOMHET,
                særligeGrunner =
                HbSærligeGrunner(
                    listOf(
                        SærligGrunn.TID_FRA_UTBETALING,
                        SærligGrunn.STØRRELSE_BELØP
                    )
                )
            ),
            resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(20002),
            førstePeriode = true
        )

    @Test
    fun `skal generere tekst når perioden overlapper en grunnbeløpsperiode`() {
        val data = HbVedtaksbrevPeriodeOgFelles(felles, periode)

        val generertTekst = FellesTekstformaterer.lagDeltekst(data, AvsnittUtil.PARTIAL_PERIODE_FAKTA)

        generertTekst shouldBe les("/vedtaksbrev/barnetilsyn/BT_beløp_over_6g.txt")
    }

    @Test
    fun `skal generere tekst når perioden overlapper flere grunnbeløpsperioder`() {
        val data = HbVedtaksbrevPeriodeOgFelles(felles, periode.copy(fakta = fakta.copy(grunnbeløpsperioder = listOf(
            HbGrunnbeløpsperiode(
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 4, 30),
                BigDecimal(99_858)
            ),
            HbGrunnbeløpsperiode(
                LocalDate.of(2020, 5, 1),
                LocalDate.of(2021, 4, 30),
                BigDecimal(101_351)
            ),
            HbGrunnbeløpsperiode(
                LocalDate.of(2021, 5, 1),
                LocalDate.of(2021, 5, 31),
                BigDecimal(104_716)
            )
        ))))

        val generertTekst = FellesTekstformaterer.lagDeltekst(data, AvsnittUtil.PARTIAL_PERIODE_FAKTA)

        generertTekst shouldBe les("/vedtaksbrev/barnetilsyn/BT_beløp_over_6g_flere_grunnbeløpsperioder.txt")
    }

    private fun les(filnavn: String): String? {
        javaClass.getResourceAsStream(filnavn).use { resource ->
            Scanner(resource, StandardCharsets.UTF_8).use { scanner ->
                scanner.useDelimiter("\\A")
                return if (scanner.hasNext()) scanner.next() else null
            }
        }
    }
}
