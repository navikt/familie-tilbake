package no.nav.tilbakekreving.brev.vedtaksbrev

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.breeeev.BegrunnetPeriode
import no.nav.tilbakekreving.breeeev.Signatur
import no.nav.tilbakekreving.breeeev.VedtaksbrevInfo
import no.nav.tilbakekreving.breeeev.begrunnelse.Forklaringstekster
import no.nav.tilbakekreving.breeeev.begrunnelse.VilkårsvurderingBegrunnelse
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.frontend.models.AvsnittUpdateItemDto
import no.nav.tilbakekreving.kontrakter.frontend.models.BrevmottakerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedavsnittDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedavsnittUpdateDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PakrevdBegrunnelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PakrevdBegrunnelseUpdateItemDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RentekstElementDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SignaturDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevRedigerbareDataUpdateDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class NyVedtaksbrevServiceTest : TilbakekrevingE2EBase() {
    @Autowired
    lateinit var nyVedtaksbrevService: NyVedtaksbrevService

    @Autowired
    lateinit var vedtaksbrevDataRepository: VedtaksbrevDataRepository

    @Autowired
    lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    private val expectedSignatur = SignaturDto(
        enhetNavn = "NAV Arbeid og Ytelser",
        ansvarligSaksbehandler = "Bob Burger",
        besluttendeSaksbehandler = null,
    )

    private val brevmottaker = BrevmottakerDto(
        navn = "Toasty Testy",
        personIdent = "20046912345",
    )

    @Test
    fun `det finnes ingen lagrede felter i vedtaksbrev`() {
        val vedtaksbrevInfo = vedtaksbrevInfo(UUID.randomUUID())
        nyVedtaksbrevService.hentVedtaksbrevData(lagBehandlingId(), vedtaksbrevInfo).should {
            it.hovedavsnitt shouldBe HovedavsnittDto(
                tittel = "Du må betale tilbake arbeidsavklaringspengene",
                forklaring = Forklaringstekster.HOVEDAVSNITT,
                underavsnitt = listOf(RentekstElementDto("")),
            )
            it.ytelse shouldBe Ytelse.Arbeidsavklaringspenger.brevmeta()
            it.signatur shouldBe expectedSignatur
            it.avsnitt[0].tittel shouldBe "Dette er grunnen til at du har fått for mye utbetalt"
            it.avsnitt[0].underavsnitt shouldBe listOf(
                RentekstElementDto(""),
                PakrevdBegrunnelseDto(
                    tittel = VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER.tittel,
                    forklaring = VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER.forklaring,
                    begrunnelseType = VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER.name,
                    underavsnitt = listOf(RentekstElementDto("")),
                    meldingerTilSaksbehandler = emptyList(),
                ),
            )
        }
    }

    @Test
    fun `fletter inn lagrede felter i vedtaksbrev-data`() {
        val behandlingId = lagBehandlingId()
        val periodeId = UUID.randomUUID()
        val vedtaksbrevInfo = vedtaksbrevInfo(periodeId)
        nyVedtaksbrevService.oppdaterVedtaksbrevData(
            behandlingId,
            VedtaksbrevRedigerbareDataUpdateDto(
                hovedavsnitt = HovedavsnittUpdateDto(
                    tittel = "Du må betale tilbake arbeidsavklaringspengene",
                    underavsnitt = listOf(
                        RentekstElementDto("Vi oppdaget at du stjal penger"),
                    ),
                ),
                avsnitt = listOf(
                    AvsnittUpdateItemDto(
                        tittel = "Dette er grunnen til at du har fått for mye utbetalt",
                        id = periodeId,
                        underavsnitt = listOf(
                            RentekstElementDto("Det var jo litt uaktsomt..."),
                            RentekstElementDto("Derfor må du betale tilbake"),
                            PakrevdBegrunnelseUpdateItemDto(
                                begrunnelseType = VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER.name,
                                underavsnitt = listOf(RentekstElementDto("Ja, det syntes jeg.")),
                            ),
                        ),
                    ),
                ),
            ),
            vedtaksbrevInfo,
        )

        nyVedtaksbrevService.hentVedtaksbrevData(behandlingId, vedtaksbrevInfo).should {
            it.hovedavsnitt shouldBe HovedavsnittDto(
                tittel = "Du må betale tilbake arbeidsavklaringspengene",
                forklaring = Forklaringstekster.HOVEDAVSNITT,
                underavsnitt = listOf(
                    RentekstElementDto("Vi oppdaget at du stjal penger"),
                ),
            )
            it.ytelse shouldBe Ytelse.Arbeidsavklaringspenger.brevmeta()
            it.signatur shouldBe expectedSignatur
            it.avsnitt[0].tittel shouldBe "Dette er grunnen til at du har fått for mye utbetalt"
            it.avsnitt[0].underavsnitt shouldBe listOf(
                RentekstElementDto("Det var jo litt uaktsomt..."),
                RentekstElementDto("Derfor må du betale tilbake"),
                PakrevdBegrunnelseDto(
                    begrunnelseType = VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER.name,
                    forklaring = VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER.forklaring,
                    tittel = VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER.tittel,
                    underavsnitt = listOf(RentekstElementDto("Ja, det syntes jeg.")),
                    meldingerTilSaksbehandler = emptyList(),
                ),
            )
        }
    }

    @Test
    fun `bytting av vilkårsvurdering gir nye påkrevde vurderinger`() {
        val behandlingId = lagBehandlingId()
        val periodeId = UUID.randomUUID()
        val originalVedtaksbrevInfo = vedtaksbrevInfo(periodeId)
        val hovedavsnitt = HovedavsnittUpdateDto(
            tittel = "Du må betale tilbake arbeidsavklaringspengene",
            underavsnitt = listOf(
                RentekstElementDto("Vi oppdaget at du stjal penger"),
            ),
        )
        nyVedtaksbrevService.oppdaterVedtaksbrevData(
            behandlingId,
            VedtaksbrevRedigerbareDataUpdateDto(
                hovedavsnitt = hovedavsnitt,
                avsnitt = listOf(
                    AvsnittUpdateItemDto(
                        tittel = "Dette er grunnen til at du har fått for mye utbetalt",
                        id = periodeId,
                        underavsnitt = listOf(
                            RentekstElementDto("Det var jo litt uaktsomt..."),
                            RentekstElementDto("Derfor må du betale tilbake"),
                            PakrevdBegrunnelseUpdateItemDto(
                                begrunnelseType = VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER.name,
                                underavsnitt = listOf(RentekstElementDto("Ja, det syntes jeg.")),
                            ),
                        ),
                    ),
                ),
            ),
            originalVedtaksbrevInfo,
        )

        val midlertidigVedtaksbrevInfo = vedtaksbrevInfo(periodeId, VilkårsvurderingBegrunnelse.REDUSERT_SÆRLIGE_GRUNNER)
        nyVedtaksbrevService.hentVedtaksbrevData(behandlingId, midlertidigVedtaksbrevInfo).should {
            it.avsnitt[0].tittel shouldBe "Dette er grunnen til at du har fått for mye utbetalt"
            it.avsnitt[0].underavsnitt shouldBe listOf(
                RentekstElementDto("Det var jo litt uaktsomt..."),
                RentekstElementDto("Derfor må du betale tilbake"),
                PakrevdBegrunnelseDto(
                    begrunnelseType = VilkårsvurderingBegrunnelse.REDUSERT_SÆRLIGE_GRUNNER.name,
                    forklaring = VilkårsvurderingBegrunnelse.REDUSERT_SÆRLIGE_GRUNNER.forklaring,
                    tittel = VilkårsvurderingBegrunnelse.REDUSERT_SÆRLIGE_GRUNNER.tittel,
                    underavsnitt = listOf(RentekstElementDto("")),
                    meldingerTilSaksbehandler = emptyList(),
                ),
            )
        }
    }

    private fun vedtaksbrevInfo(
        periodeId: UUID,
        vararg påkrevdeBegrunnelser: VilkårsvurderingBegrunnelse = arrayOf(VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER),
    ) = VedtaksbrevInfo(
        brukerdata = brevmottaker,
        ytelse = Ytelse.Arbeidsavklaringspenger.brevmeta(),
        signatur = Signatur(
            ansvarligSaksbehandlerIdent = "Z999999",
            ansvarligBeslutterIdent = null,
            ansvarligEnhet = "NAV Arbeid og Ytelser",
        ),
        perioder = listOf(
            BegrunnetPeriode(
                id = periodeId,
                periode = 1.januar(2021) til 31.januar(2021),
                meldingerTilSaksbehandler = emptySet(),
                påkrevdeVurderinger = påkrevdeBegrunnelser.toSet(),
            ),
        ),
        bunntekster = emptySet(),
        tilbakekrevingId = UUID.randomUUID().toString(),
    )

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
