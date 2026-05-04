package no.nav.tilbakekreving.brev.vedtaksbrev

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.breeeev.begrunnelse.Forklaringstekster
import no.nav.tilbakekreving.breeeev.begrunnelse.VilkårsvurderingBegrunnelse
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.frontend.models.AvsnittUpdateItemDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedavsnittDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedavsnittUpdateDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PakrevdBegrunnelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.PakrevdBegrunnelseUpdateItemDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RentekstElementDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SignaturDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevRedigerbareDataUpdateDto
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
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

    @Test
    fun `det finnes ingen lagrede felter i vedtaksbrev`() {
        val vedtaksbrevInfo = vedtaksbrevInfo(UUID.randomUUID())
        nyVedtaksbrevService.hentVedtaksbrevData(lagBehandlingId(), vedtaksbrevInfo, null).should {
            it.hovedavsnitt shouldBe HovedavsnittDto(
                tittel = "Du må betale tilbake arbeidsavklaringspenger",
                forklaring = Forklaringstekster.HOVEDAVSNITT,
                underavsnitt = listOf(RentekstElementDto("")),
                hjemler = "Vedtaket er gjort etter folketrygdloven § 22-15.",
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
                    tittel = "Du må betale tilbake arbeidsavklaringspenger",
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

        nyVedtaksbrevService.hentVedtaksbrevData(behandlingId, vedtaksbrevInfo, null).should {
            it.hovedavsnitt shouldBe HovedavsnittDto(
                tittel = "Du må betale tilbake arbeidsavklaringspenger",
                forklaring = Forklaringstekster.HOVEDAVSNITT,
                underavsnitt = listOf(
                    RentekstElementDto("Vi oppdaget at du stjal penger"),
                ),
                hjemler = "Vedtaket er gjort etter folketrygdloven § 22-15.",
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
            tittel = "Du må betale tilbake arbeidsavklaringspenger",
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
        nyVedtaksbrevService.hentVedtaksbrevData(behandlingId, midlertidigVedtaksbrevInfo, null).should {
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

    @Test
    fun `gjenoppretter tidligere lagret vedtakstekster ved endring av vilkår`() {
        val behandlingId = lagBehandlingId()
        val periodeId = UUID.randomUUID()
        val originalVedtaksbrevInfo = vedtaksbrevInfo(periodeId)
        val hovedavsnitt = HovedavsnittUpdateDto(
            tittel = "Du må betale tilbake arbeidsavklaringspenger",
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
                                begrunnelseType = VilkårsvurderingBegrunnelse.REDUSERT_SÆRLIGE_GRUNNER.name,
                                underavsnitt = listOf(RentekstElementDto("Redusert!!!")),
                            ),
                        ),
                    ),
                ),
            ),
            info = midlertidigVedtaksbrevInfo,
        )

        nyVedtaksbrevService.hentVedtaksbrevData(behandlingId, midlertidigVedtaksbrevInfo, null).should {
            it.avsnitt[0].tittel shouldBe "Dette er grunnen til at du har fått for mye utbetalt"
            it.avsnitt[0].underavsnitt shouldBe listOf(
                RentekstElementDto("Det var jo litt uaktsomt..."),
                RentekstElementDto("Derfor må du betale tilbake"),
                PakrevdBegrunnelseDto(
                    begrunnelseType = VilkårsvurderingBegrunnelse.REDUSERT_SÆRLIGE_GRUNNER.name,
                    forklaring = VilkårsvurderingBegrunnelse.REDUSERT_SÆRLIGE_GRUNNER.forklaring,
                    tittel = VilkårsvurderingBegrunnelse.REDUSERT_SÆRLIGE_GRUNNER.tittel,
                    underavsnitt = listOf(RentekstElementDto("Redusert!!!")),
                    meldingerTilSaksbehandler = emptyList(),
                ),
            )
        }

        nyVedtaksbrevService.hentVedtaksbrevData(behandlingId, originalVedtaksbrevInfo, null).should {
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
    fun `under 4 rettsgebyr - tilbakekreves - har standardtekst i første vedtaksbrev-info`() {
        val behandlingId = lagBehandlingId()
        val periodeId = UUID.randomUUID()
        val info = vedtaksbrevInfo(periodeId, VilkårsvurderingBegrunnelse.SKAL_IKKE_UNNLATES_4_RETTSGEBYR)

        nyVedtaksbrevService.hentVedtaksbrevData(behandlingId, info, null).should {
            it.avsnitt[0].tittel shouldBe "Dette er grunnen til at du har fått for mye utbetalt"
            it.avsnitt[0].underavsnitt shouldBe listOf(
                RentekstElementDto(""),
                PakrevdBegrunnelseDto(
                    begrunnelseType = VilkårsvurderingBegrunnelse.SKAL_IKKE_UNNLATES_4_RETTSGEBYR.name,
                    forklaring = VilkårsvurderingBegrunnelse.SKAL_IKKE_UNNLATES_4_RETTSGEBYR.forklaring,
                    tittel = VilkårsvurderingBegrunnelse.SKAL_IKKE_UNNLATES_4_RETTSGEBYR.tittel,
                    underavsnitt = listOf(RentekstElementDto("Nav kan la være å kreve tilbake hvis det feilutbetalte beløpet er lavere enn fire ganger rettsgebyret. Dette gjelder ikke hvis du har handlet forsettlig eller grovt uaktsomt. Se folketrygdloven § 22-15 sjette avsnitt.")),
                    meldingerTilSaksbehandler = emptyList(),
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
