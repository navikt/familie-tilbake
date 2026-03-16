package no.nav.tilbakekreving.e2e.brev

import com.fasterxml.jackson.module.kotlin.convertValue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldMatchEach
import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.api.v2.PeriodeDto
import no.nav.tilbakekreving.api.v2.fagsystem.svar.FagsysteminfoSvarHendelse
import no.nav.tilbakekreving.breeeev.begrunnelse.Forklaringstekster
import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler
import no.nav.tilbakekreving.breeeev.begrunnelse.VilkårsvurderingBegrunnelse
import no.nav.tilbakekreving.brev.Vedtaksbrev
import no.nav.tilbakekreving.brev.vedtaksbrev.BrevFormatterer.tilDto
import no.nav.tilbakekreving.builders.VilkårsvurderingDtoBuilder.forårsaketAvBruker
import no.nav.tilbakekreving.e2e.BehandlingsstegGenerator
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.frontend.models.AvsnittDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HovedavsnittDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RentekstElementDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevDataDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VedtaksbrevRedigerbareDataUpdateDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.test.ingenReduksjon
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.skalIkkeUnnlates
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class VedtaksbrevE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    private val ansvarligSaksbehandler = "Z999999"
    private val ansvarligBeslutter = "Z111111"

    @Test
    fun `vedtaksbrev sendes og lagres riktig i DB`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))

        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        lagreUttalelse(behandlingId)

        somSaksbehandler(ansvarligSaksbehandler) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }
        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(),
        )
        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingFullTilbakekreving(),
        )
        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagForeslåVedtakVurdering(),
        )
        utførSteg(
            ident = ansvarligBeslutter,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagGodkjennVedtakVurdering(),
        )

        tilbakekrevingService.hentTilbakekreving(behandlingId).shouldNotBeNull {
            brevHistorikk.nåværende().entry.shouldBeInstanceOf<Vedtaksbrev>()
        }
    }

    @MethodSource("lagreOgGjenopprett")
    @ParameterizedTest
    fun `opprettelse og gjenopprettelse gir samme data for brev`(
        vurdering: VilkårsvurderingsperiodeDto,
        assertions: AvsnittAssertions,
    ) {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(
            ytelse = Ytelse.Tilleggsstønad,
            fagsysteminfo = Testdata.fagsysteminfoSvar(
                fagsystemId = fagsystemId,
                utvidPerioder = listOf(
                    FagsysteminfoSvarHendelse.UtvidetPeriodeDto(
                        kravgrunnlagPeriode = PeriodeDto(1.januar(2021), 1.januar(2021)),
                        vedtaksperiode = PeriodeDto(1.januar(2021), 31.januar(2021)),
                    ),
                ),
            ),
        )

        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        lagreUttalelse(behandlingId, uttalelse = "Jeg svindlet NAV")

        somSaksbehandler(ansvarligSaksbehandler) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }
        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(1.januar(2021) til 31.januar(2021)),
        )
        håndterVilkårsvurdering(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            vurderinger = arrayOf(vurdering),
        )
        val avsnittFørOppdatering = behandlingApiController.behandlingHentVedtaksbrev(behandlingId.toString()).body.shouldNotBeNull()
        avsnittFørOppdatering.hovedavsnitt shouldBe assertions.hovedavsnitt
        avsnittFørOppdatering.avsnitt.shouldMatchEach(assertions.periodeAvsnitt) { actual, expected ->
            actual.shouldBeEqualToIgnoringFields(expected, AvsnittDto::id, AvsnittDto::underavsnitt, AvsnittDto::meldingerTilSaksbehandler)
            actual.meldingerTilSaksbehandler shouldContainExactly expected.meldingerTilSaksbehandler
            actual.underavsnitt shouldContainExactly expected.underavsnitt
        }

        behandlingApiController.behandlingOppdaterVedtaksbrev(
            behandlingId = behandlingId,
            vedtaksbrevRedigerbareDataUpdateDto = objectMapper.convertValue<VedtaksbrevRedigerbareDataUpdateDto>(avsnittFørOppdatering),
        )

        val avsnittEtterOppdatering = behandlingApiController.behandlingHentVedtaksbrev(behandlingId.toString()).body.shouldNotBeNull()
        avsnittEtterOppdatering.shouldBeEqualToIgnoringFields(
            avsnittFørOppdatering,
            VedtaksbrevDataDto::sistOppdatert,
            VedtaksbrevDataDto::avsnitt,
        )
        avsnittEtterOppdatering.avsnitt.shouldMatchEach(avsnittFørOppdatering.avsnitt) { actual, expected ->
            actual.shouldBeEqualToIgnoringFields(expected, AvsnittDto::underavsnitt, AvsnittDto::meldingerTilSaksbehandler, AvsnittDto::id)
            actual.meldingerTilSaksbehandler shouldContainExactly expected.meldingerTilSaksbehandler
            actual.underavsnitt shouldContainExactly expected.underavsnitt
        }
    }

    companion object {
        @JvmStatic
        fun lagreOgGjenopprett(): List<Arguments> {
            return listOf(
                Arguments.argumentSet(
                    "forårsaket av bruker, uaktsomt, bruker har uttalt seg",
                    forårsaketAvBruker()
                        .uaktsomt(skalIkkeUnnlates(), ingenReduksjon())
                        .copy(periode = 1.januar(2021) til 31.januar(2021)),
                    AvsnittAssertions(
                        hovedavsnitt = HovedavsnittDto(
                            tittel = "Du må betale tilbake tilleggsstønaden",
                            forklaring = Forklaringstekster.HOVEDAVSNITT,
                            underavsnitt = listOf(RentekstElementDto(tekst = "")),
                        ),
                        periodeAvsnitt = listOf(
                            AvsnittDto(
                                tittel = "Dette er grunnen til at du har fått for mye utbetalt",
                                forklaring = Forklaringstekster.PERIODE_AVSNITT,
                                meldingerTilSaksbehandler = listOf(MeldingTilSaksbehandler.BEGRUNN_BRUKERS_UTTALELSE.melding),
                                id = UUID.randomUUID(),
                                underavsnitt = listOf(
                                    RentekstElementDto(tekst = ""),
                                    VilkårsvurderingBegrunnelse.TILBAKEKREVES.tilDto(meldingerTilSaksbehandler = listOf(MeldingTilSaksbehandler.BEGRUNN_BRUKERS_UTTALELSE)),
                                    VilkårsvurderingBegrunnelse.SKAL_IKKE_UNNLATES_4_RETTSGEBYR.tilDto(),
                                    VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER.tilDto(),
                                ),
                            ),
                        ),
                    ),
                ),
            )
        }

        data class AvsnittAssertions(
            val hovedavsnitt: HovedavsnittDto,
            val periodeAvsnitt: List<AvsnittDto>,
        )
    }
}
