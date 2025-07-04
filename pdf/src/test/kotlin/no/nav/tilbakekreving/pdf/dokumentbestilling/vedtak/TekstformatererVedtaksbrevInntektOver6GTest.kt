package no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunn
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Adresseinfo
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.HbGrunnbeløpUtil.lagHbGrunnbeløp
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.TekstformatererVedtaksbrev.lagVedtaksbrevsfritekst
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbBehandling
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbHjemmel
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbKonfigurasjon
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbPerson
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbTotalresultat
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbVarsel
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevFelles
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.HbVedtaksbrevsdata
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.Vedtaksbrevstype
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbFakta
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbGrunnbeløp
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbKravgrunnlag
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbResultatTestBuilder
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbSærligeGrunner
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbVedtaksbrevsperiode
import no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode.HbVurderinger
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.Scanner

class TekstformatererVedtaksbrevInntektOver6GTest {
    @Nested
    inner class GenererHeltVedtaksbrev {
        @Test
        internal fun `en periode, en beløpsperiode`() {
            val data = HbVedtaksbrevsdata(felles, listOf(periodeMedEnBeløpsperiode))
            val generertBrev = lagVedtaksbrevsfritekst(data)
            val fasit = les("/vedtaksbrev/barnetilsyn/BT_beløp_over_6G_helt_brev_en_periode.txt")

            generertBrev shouldBe fasit
        }

        @Test
        internal fun `flere perioder, to beløpsperiode og tre beløpsperioder`() {
            val data = HbVedtaksbrevsdata(felles, listOf(periodeMedToBeløpsperioder, periodeMedTreBeløpsperioder))
            val generertBrev = lagVedtaksbrevsfritekst(data)
            val fasit = les("/vedtaksbrev/barnetilsyn/BT_beløp_over_6G_helt_brev_flere_perioder_flere_beløp.txt")

            generertBrev shouldBe fasit
        }

        @Test
        internal fun `nynorsk - flere perioder, en beløpsperiode og tre beløpsperioder`() {
            val data =
                HbVedtaksbrevsdata(
                    felles.copy(brevmetadata.copy(språkkode = Språkkode.NN)),
                    listOf(periodeMedEnBeløpsperiode, periodeMedTreBeløpsperioder),
                )
            val generertBrev = lagVedtaksbrevsfritekst(data)
            val fasit = les("/vedtaksbrev/barnetilsyn/BT_beløp_over_6G_helt_brev_flere_perioder_flere_beløp_nn.txt")

            generertBrev shouldBe fasit
        }
    }

    private val januar =
        Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 31))

    private val brevmetadata =
        Brevmetadata(
            sakspartId = "123456",
            sakspartsnavn = "Test",
            mottageradresse = Adresseinfo("ident", "bob"),
            behandlendeEnhetsNavn = "Nav Familie- og pensjonsytelser Skien",
            ansvarligSaksbehandler = "Ansvarlig Saksbehandler",
            saksnummer = "1232456",
            språkkode = Språkkode.NB,
            ytelsestype = YtelsestypeDTO.OVERGANGSSTØNAD,
            gjelderDødsfall = false,
        )

    private val felles =
        HbVedtaksbrevFelles(
            brevmetadata = brevmetadata,
            hjemmel = HbHjemmel("Folketrygdloven"),
            totalresultat =
                HbTotalresultat(
                    Vedtaksresultat.FULL_TILBAKEBETALING,
                    BigDecimal.valueOf(10000),
                    BigDecimal.valueOf(11000),
                    BigDecimal.valueOf(11000),
                    BigDecimal.valueOf(1000),
                ),
            varsel =
                HbVarsel(
                    varsletBeløp = BigDecimal.valueOf(10000),
                    varsletDato = LocalDate.of(2022, 6, 21),
                ),
            konfigurasjon = HbKonfigurasjon(klagefristIUker = 6),
            søker =
                HbPerson(
                    navn = "Søker Søkersen",
                ),
            fagsaksvedtaksdato = LocalDate.now(),
            behandling = HbBehandling(),
            totaltFeilutbetaltBeløp = BigDecimal.valueOf(10000),
            vedtaksbrevstype = Vedtaksbrevstype.ORDINÆR,
            ansvarligBeslutter = "Ansvarlig Beslutter",
            harBrukerUttaltSeg = HarBrukerUttaltSeg.JA,
        )

    private val fakta =
        HbFakta(Hendelsestype.STØNAD_TIL_BARNETILSYN, Hendelsesundertype.INNTEKT_OVER_6G)

    private fun lagPeriode(grunnbeløp: HbGrunnbeløp): HbVedtaksbrevsperiode =
        HbVedtaksbrevsperiode(
            periode = januar,
            kravgrunnlag = HbKravgrunnlag.forFeilutbetaltBeløp(BigDecimal(30001)),
            fakta = fakta,
            vurderinger =
                HbVurderinger(
                    foreldelsevurdering = Foreldelsesvurderingstype.IKKE_VURDERT,
                    vilkårsvurderingsresultat =
                        Vilkårsvurderingsresultat
                            .MANGELFULLE_OPPLYSNINGER_FRA_BRUKER,
                    aktsomhetsresultat = Aktsomhet.SIMPEL_UAKTSOMHET,
                    særligeGrunner =
                        HbSærligeGrunner(
                            listOf(
                                SærligGrunn.TID_FRA_UTBETALING,
                                SærligGrunn.STØRRELSE_BELØP,
                            ),
                        ),
                ),
            resultat = HbResultatTestBuilder.forTilbakekrevesBeløp(20002),
            grunnbeløp = grunnbeløp,
            førstePeriode = true,
        )

    private val periodeMedEnBeløpsperiode =
        lagPeriode(
            lagHbGrunnbeløp(
                Månedsperiode(
                    LocalDate.of(2021, 1, 1),
                    LocalDate.of(2021, 3, 31),
                ),
            ),
        )

    private val periodeMedToBeløpsperioder =
        lagPeriode(
            lagHbGrunnbeløp(
                Månedsperiode(
                    LocalDate.of(2020, 1, 1),
                    LocalDate.of(2021, 4, 30),
                ),
            ),
        )

    private val periodeMedTreBeløpsperioder =
        lagPeriode(
            lagHbGrunnbeløp(
                Månedsperiode(
                    LocalDate.of(2020, 1, 1),
                    LocalDate.of(2021, 5, 31),
                ),
            ),
        )

    private fun les(filnavn: String): String? {
        javaClass.getResourceAsStream(filnavn).use { resource ->
            Scanner(resource, StandardCharsets.UTF_8).use { scanner ->
                scanner.useDelimiter("\\A")
                return if (scanner.hasNext()) scanner.next() else null
            }
        }
    }
}
