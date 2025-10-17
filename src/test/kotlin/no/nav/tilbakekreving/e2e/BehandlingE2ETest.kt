package no.nav.tilbakekreving.e2e

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.SærligeGrunner
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.UtvidetVilkårsresultat
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.VedtakPeriode
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.GodTroDto
import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.februar
import no.nav.tilbakekreving.integrasjoner.KafkaProducerStub
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BehandlingE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var kafkaProducer: KafkaProducerStub

    private val ansvarligSaksbehandler = "Z999999"
    private val ansvarligBeslutter = "Z111111"

    @Test
    fun `endringer i behandling skal føre til kafka-meldinger til dvh`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )

        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(),
        )

        val dvhHendelser = kafkaProducer.finnSaksdata(behandlingId)
        dvhHendelser.size shouldBe 2

        dvhHendelser[0].ansvarligSaksbehandler shouldBe "VL"
        dvhHendelser[0].ansvarligBeslutter shouldBe null
        dvhHendelser[0].ansvarligEnhet shouldBe "Ukjent"
        dvhHendelser[0].behandlingsstatus shouldBe Behandlingsstatus.OPPRETTET

        dvhHendelser[1].ansvarligSaksbehandler shouldBe ansvarligSaksbehandler
        dvhHendelser[1].ansvarligBeslutter shouldBe null
        dvhHendelser[1].ansvarligEnhet shouldBe "Ukjent"
        dvhHendelser[1].behandlingsstatus shouldBe Behandlingsstatus.UTREDES

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(),
        )
        dvhHendelser.size shouldBe 3
        dvhHendelser.last().ansvarligSaksbehandler shouldBe ansvarligSaksbehandler
        dvhHendelser.last().ansvarligBeslutter shouldBe null
        dvhHendelser.last().ansvarligEnhet shouldBe "Ukjent"
        dvhHendelser.last().behandlingsstatus shouldBe Behandlingsstatus.UTREDES

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingFullTilbakekreving(),
        )
        dvhHendelser.size shouldBe 4
        dvhHendelser.last().ansvarligSaksbehandler shouldBe ansvarligSaksbehandler
        dvhHendelser.last().ansvarligBeslutter shouldBe null
        dvhHendelser.last().ansvarligEnhet shouldBe "Ukjent"
        dvhHendelser.last().behandlingsstatus shouldBe Behandlingsstatus.UTREDES

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagForeslåVedtakVurdering(),
        )
        dvhHendelser.size shouldBe 5
        dvhHendelser.last().ansvarligSaksbehandler shouldBe ansvarligSaksbehandler
        dvhHendelser.last().ansvarligBeslutter shouldBe null
        dvhHendelser.last().ansvarligEnhet shouldBe "Ukjent"
        dvhHendelser.last().behandlingsstatus shouldBe Behandlingsstatus.FATTER_VEDTAK
        kafkaProducer.finnVedtaksoppsummering(behandlingId).size shouldBe 0

        utførSteg(
            ident = ansvarligBeslutter,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagGodkjennVedtakVurdering(),
        )
        dvhHendelser.size shouldBe 7
        dvhHendelser[5].ansvarligSaksbehandler shouldBe ansvarligSaksbehandler
        dvhHendelser[5].ansvarligBeslutter shouldBe ansvarligBeslutter
        dvhHendelser[5].ansvarligEnhet shouldBe "Ukjent"
        dvhHendelser[5].behandlingsstatus shouldBe Behandlingsstatus.IVERKSETTER_VEDTAK

        dvhHendelser[6].ansvarligSaksbehandler shouldBe ansvarligSaksbehandler
        dvhHendelser[6].ansvarligBeslutter shouldBe ansvarligBeslutter
        dvhHendelser[6].ansvarligEnhet shouldBe "Ukjent"
        dvhHendelser[6].behandlingsstatus shouldBe Behandlingsstatus.AVSLUTTET

        val vedtaksoppsummeringer = kafkaProducer.finnVedtaksoppsummering(behandlingId)
        vedtaksoppsummeringer.size shouldBe 1
        val vedtaksoppsummering = vedtaksoppsummeringer.single()
        vedtaksoppsummering.ansvarligSaksbehandler shouldBe ansvarligSaksbehandler
        vedtaksoppsummering.ansvarligBeslutter shouldBe ansvarligBeslutter
        vedtaksoppsummering.perioder shouldBe listOf(
            VedtakPeriode(
                fom = 1.januar(2021),
                tom = 1.januar(2021),
                hendelsestype = "ANNET",
                hendelsesundertype = "ANNET_FRITEKST",
                vilkårsresultat = UtvidetVilkårsresultat.FORSTO_BURDE_FORSTÅTT,
                feilutbetaltBeløp = 2000.kroner,
                bruttoTilbakekrevingsbeløp = 2000.kroner,
                rentebeløp = 0.kroner,
                harBruktSjetteLedd = false,
                aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                særligeGrunner = SærligeGrunner(
                    erSærligeGrunnerTilReduksjon = false,
                    særligeGrunner = emptyList(),
                ),
            ),
        )
    }

    @Test
    fun `lagrer vurderingsperioder for fakta`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )

        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(),
        )

        tilbakekreving(behandlingId).faktastegFrontendDto().feilutbetaltePerioder.size shouldBe 1
    }

    @Test
    fun `lagrer begrunnelse riktig for god tro`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )

        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(),
        )

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(),
        )

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegVilkårsvurderingDto(
                vilkårsvurderingsperioder = listOf(
                    VilkårsvurderingsperiodeDto(
                        periode = 1.januar(2021) til 1.januar(2021),
                        vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                        begrunnelse = "Jepp",
                        godTroDto = GodTroDto(
                            beløpErIBehold = false,
                            beløpTilbakekreves = null,
                            begrunnelse = "Japp",
                        ),
                    ),
                ),
            ),
        )

        val tilbakekreving = tilbakekreving(behandlingId)
        val vilkårsvurderingFrontendDto = tilbakekreving.behandlingHistorikk.nåværende().entry.vilkårsvurderingsstegDto.tilFrontendDto()
        vilkårsvurderingFrontendDto.perioder.size shouldBe 1
        vilkårsvurderingFrontendDto.perioder.single().begrunnelse shouldBe "Jepp"
        vilkårsvurderingFrontendDto.perioder.single().vilkårsvurderingsresultatInfo?.godTro?.begrunnelse shouldBe "Japp"
    }

    @Test
    fun `lagrer begrunnelse riktig for god tro med to perioder`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
                perioder = listOf(
                    KravgrunnlagGenerator.standardPeriode(1.januar(2021) til 1.januar(2021)),
                    KravgrunnlagGenerator.standardPeriode(1.februar(2021) til 1.februar(2021)),
                ),
            ),
        )

        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(
                1.januar(2021) til 1.januar(2021),
                1.februar(2021) til 1.februar(2021),
            ),
        )

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(
                1.januar(2021) til 1.januar(2021),
                1.februar(2021) til 1.februar(2021),
            ),
        )

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegVilkårsvurderingDto(
                vilkårsvurderingsperioder = listOf(
                    VilkårsvurderingsperiodeDto(
                        periode = 1.januar(2021) til 1.januar(2021),
                        vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                        begrunnelse = "Jepp1",
                        godTroDto = GodTroDto(
                            beløpErIBehold = false,
                            beløpTilbakekreves = null,
                            begrunnelse = "Japp1",
                        ),
                    ),
                ),
            ),
        )

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegVilkårsvurderingDto(
                vilkårsvurderingsperioder = listOf(
                    VilkårsvurderingsperiodeDto(
                        periode = 1.februar(2021) til 1.februar(2021),
                        vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                        begrunnelse = "Jepp2",
                        godTroDto = GodTroDto(
                            beløpErIBehold = false,
                            beløpTilbakekreves = null,
                            begrunnelse = "Japp2",
                        ),
                    ),
                ),
            ),
        )

        val tilbakekreving = tilbakekreving(behandlingId)
        val vilkårsvurderingFrontendDto = tilbakekreving.behandlingHistorikk.nåværende().entry.vilkårsvurderingsstegDto.tilFrontendDto()
        vilkårsvurderingFrontendDto.perioder.size shouldBe 2
        vilkårsvurderingFrontendDto.perioder[0].begrunnelse shouldBe "Jepp1"
        vilkårsvurderingFrontendDto.perioder[0].vilkårsvurderingsresultatInfo?.godTro?.begrunnelse shouldBe "Japp1"
        vilkårsvurderingFrontendDto.perioder[1].begrunnelse shouldBe "Jepp2"
        vilkårsvurderingFrontendDto.perioder[1].vilkårsvurderingsresultatInfo?.godTro?.begrunnelse shouldBe "Japp2"
    }
}
