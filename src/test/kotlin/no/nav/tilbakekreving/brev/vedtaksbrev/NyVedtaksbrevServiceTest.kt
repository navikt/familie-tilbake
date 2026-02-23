package no.nav.tilbakekreving.brev.vedtaksbrev

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.breeeev.BegrunnetPeriode
import no.nav.tilbakekreving.breeeev.PåkrevdBegrunnelse
import no.nav.tilbakekreving.breeeev.Signatur
import no.nav.tilbakekreving.breeeev.Vedtaksbrev
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.frontend.models.AvsnittDto
import no.nav.tilbakekreving.kontrakter.frontend.models.BrevmottakerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedavsnittDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RentekstElementDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SignaturDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UnderavsnittElementDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevRedigerbareDataDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.OffsetDateTime
import java.util.UUID

class NyVedtaksbrevServiceTest : TilbakekrevingE2EBase() {
    @Autowired
    lateinit var nyVedtaksbrevService: NyVedtaksbrevService

    @Autowired
    lateinit var vedtaksbrevDataRepository: VedtaksbrevDataRepository

    @Autowired
    lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Test
    fun `fletter inn lagrede felter i vedtaksbrev-data`() {
        val behandlingId = lagBehandlingId()
        val vedtaksbrev = Vedtaksbrev(
            brukerdata = BrevmottakerDto(
                navn = "Toasty Testy",
                personIdent = "20046912345",
            ),
            ytelse = Ytelse.Arbeidsavklaringspenger.brevmeta(),
            signatur = Signatur(
                ansvarligSaksbehandlerIdent = "Z999999",
                ansvarligBeslutterIdent = null,
                ansvarligEnhet = "NAV Arbeid og Ytelser",
            ),
            perioder = listOf(
                BegrunnetPeriode(
                    periode = 1.januar(2021) til 31.januar(2021),
                    påkrevdeVurderinger = setOf(PåkrevdBegrunnelse.SÆRLIGE_GRUNNER),
                ),
            ),
        )
        val initielleData = nyVedtaksbrevService.hentVedtaksbrevData(behandlingId, vedtaksbrev)
        initielleData.should {
            it.hovedavsnitt shouldBe HovedavsnittDto(
                tittel = "Du må betale tilbake arbeidsavklaringspengene",
                underavsnitt = listOf(RentekstElementDto("")),
            )
            it.ytelse shouldBe Ytelse.Arbeidsavklaringspenger.brevmeta()
            it.signatur shouldBe SignaturDto(
                enhetNavn = "NAV Arbeid og Ytelser",
                ansvarligSaksbehandler = "Bob Burger",
                besluttendeSaksbehandler = null,
            )
            it.avsnitt[0].tittel shouldBe "Perioden fra og med 1. januar 2021 til og med 31. januar 2021"
            it.avsnitt[0].underavsnitt shouldBe listOf(
                RentekstElementDto(""),
                UnderavsnittElementDto(
                    tittel = "Er det særlige grunner til å redusere beløpet?",
                    underavsnitt = listOf(RentekstElementDto("")),
                ),
            )
        }

        vedtaksbrevDataRepository.oppdaterVedtaksbrevData(
            behandlingId,
            VedtaksbrevRedigerbareDataDto(
                hovedavsnitt = HovedavsnittDto(
                    tittel = "Du må betale tilbake arbeidsavklaringspengene",
                    underavsnitt = listOf(
                        RentekstElementDto("Vi oppdaget at du stjal penger"),
                    ),
                ),
                avsnitt = listOf(
                    AvsnittDto(
                        tittel = "Perioden fra og med 1. januar 2021 til og med 31. januar 2021",
                        id = initielleData.avsnitt.single().id,
                        underavsnitt = listOf(
                            RentekstElementDto("Det var jo litt uaktsomt..."),
                            RentekstElementDto("Derfor må du betale tilbake"),
                            UnderavsnittElementDto(
                                tittel = "Er det særlige grunner til å redusere beløpet?",
                                underavsnitt = listOf(RentekstElementDto("Ja, det syntes jeg.")),
                            ),
                        ),
                    ),
                ),
                sistOppdatert = OffsetDateTime.now(),
            ),
        )

        nyVedtaksbrevService.hentVedtaksbrevData(behandlingId, vedtaksbrev).should {
            it.hovedavsnitt shouldBe HovedavsnittDto(
                tittel = "Du må betale tilbake arbeidsavklaringspengene",
                underavsnitt = listOf(
                    RentekstElementDto("Vi oppdaget at du stjal penger"),
                ),
            )
            it.ytelse shouldBe Ytelse.Arbeidsavklaringspenger.brevmeta()
            it.signatur shouldBe SignaturDto(
                enhetNavn = "NAV Arbeid og Ytelser",
                ansvarligSaksbehandler = "Bob Burger",
                besluttendeSaksbehandler = null,
            )
            it.avsnitt[0].tittel shouldBe "Perioden fra og med 1. januar 2021 til og med 31. januar 2021"
            it.avsnitt[0].underavsnitt shouldBe listOf(
                RentekstElementDto("Det var jo litt uaktsomt..."),
                RentekstElementDto("Derfor må du betale tilbake"),
                UnderavsnittElementDto(
                    tittel = "Er det særlige grunner til å redusere beløpet?",
                    underavsnitt = listOf(RentekstElementDto("Ja, det syntes jeg.")),
                ),
            )
        }
    }

    private fun lagBehandlingId(): UUID {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))

        return tilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull().behandlingHistorikk.nåværende().entry.id
    }
}
