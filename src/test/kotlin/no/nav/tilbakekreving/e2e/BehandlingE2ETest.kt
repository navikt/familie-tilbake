package no.nav.tilbakekreving.e2e

import io.kotest.assertions.fail
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.SærligeGrunner
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.UtvidetVilkårsresultat
import no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak.VedtakPeriode
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.api.v1.dto.AktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegsinfoDto
import no.nav.tilbakekreving.api.v1.dto.GodTroDto
import no.nav.tilbakekreving.api.v1.dto.SkalUnnlates
import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.api.v2.PeriodeDto
import no.nav.tilbakekreving.api.v2.fagsystem.svar.FagsysteminfoSvarHendelse
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.fagsystem.events.BehandlingEndretEventDto
import no.nav.tilbakekreving.fagsystem.events.BehandlingsstatusEventDto
import no.nav.tilbakekreving.fagsystem.events.PeriodeEventDto
import no.nav.tilbakekreving.integrasjoner.KafkaProducerStub
import no.nav.tilbakekreving.integrasjoner.KafkaProducerStub.Companion.finnHendelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.kontrakter.frontend.models.OppdagetDto
import no.nav.tilbakekreving.kontrakter.frontend.models.OppdaterFaktaOmFeilutbetalingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.OppdaterFaktaPeriodeDto
import no.nav.tilbakekreving.kontrakter.frontend.models.RettsligGrunnlagDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VurderingDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.juli
import no.nav.tilbakekreving.test.mai
import no.nav.tilbakekreving.test.mars
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.LocalDate

class BehandlingE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

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
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))

        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        lagreUttalelse(behandlingId)

        somSaksbehandler(ansvarligSaksbehandler) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }

        val dvhHendelser = kafkaProducer.finnSaksdata(behandlingId)
        dvhHendelser.size shouldBe 2

        dvhHendelser[0].ansvarligSaksbehandler shouldBe "VL"
        dvhHendelser[0].ansvarligBeslutter shouldBe null
        dvhHendelser[0].ansvarligEnhet shouldBe "0425"
        dvhHendelser[0].behandlingsstatus shouldBe Behandlingsstatus.OPPRETTET

        dvhHendelser[1].ansvarligSaksbehandler shouldBe ansvarligSaksbehandler
        dvhHendelser[1].ansvarligBeslutter shouldBe null
        dvhHendelser[1].ansvarligEnhet shouldBe "0425"
        dvhHendelser[1].behandlingsstatus shouldBe Behandlingsstatus.UTREDES

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(),
        )
        dvhHendelser.size shouldBe 3
        dvhHendelser.last().ansvarligSaksbehandler shouldBe ansvarligSaksbehandler
        dvhHendelser.last().ansvarligBeslutter shouldBe null
        dvhHendelser.last().ansvarligEnhet shouldBe "0425"
        dvhHendelser.last().behandlingsstatus shouldBe Behandlingsstatus.UTREDES

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingFullTilbakekreving(),
        )
        dvhHendelser.size shouldBe 4
        dvhHendelser.last().ansvarligSaksbehandler shouldBe ansvarligSaksbehandler
        dvhHendelser.last().ansvarligBeslutter shouldBe null
        dvhHendelser.last().ansvarligEnhet shouldBe "0425"
        dvhHendelser.last().behandlingsstatus shouldBe Behandlingsstatus.UTREDES

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagForeslåVedtakVurdering(),
        )
        dvhHendelser.size shouldBe 5
        dvhHendelser.last().ansvarligSaksbehandler shouldBe ansvarligSaksbehandler
        dvhHendelser.last().ansvarligBeslutter shouldBe null
        dvhHendelser.last().ansvarligEnhet shouldBe "0425"
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
        dvhHendelser[5].ansvarligEnhet shouldBe "0425"
        dvhHendelser[5].behandlingsstatus shouldBe Behandlingsstatus.IVERKSETTER_VEDTAK

        dvhHendelser[6].ansvarligSaksbehandler shouldBe ansvarligSaksbehandler
        dvhHendelser[6].ansvarligBeslutter shouldBe ansvarligBeslutter
        dvhHendelser[6].ansvarligEnhet shouldBe "0425"
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
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))

        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()

        somSaksbehandler(ansvarligSaksbehandler) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }

        tilbakekreving(behandlingId).faktastegFrontendDto(behandlingId).feilutbetaltePerioder.size shouldBe 1
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
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))

        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
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
        val vilkårsvurderingFrontendDto = tilbakekreving.hentBehandling(behandlingId).vilkårsvurderingsstegDto.tilFrontendDto(saksbehandlerContext())
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
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))

        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        lagreUttalelse(behandlingId)

        somSaksbehandler(ansvarligSaksbehandler) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }
        somSaksbehandler(ansvarligSaksbehandler) {
            vilkårsvurderingController.hentVurdertVilkårsvurdering(behandlingId).data!!.perioder.size shouldBe 1
        }

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
                        periode = 1.januar(2021) til 1.februar(2021),
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

        val tilbakekreving = tilbakekreving(behandlingId)
        val vilkårsvurderingFrontendDto = tilbakekreving.hentBehandling(behandlingId).vilkårsvurderingsstegDto.tilFrontendDto(saksbehandlerContext())
        vilkårsvurderingFrontendDto.perioder.size shouldBe 1
        vilkårsvurderingFrontendDto.perioder.first().begrunnelse shouldBe "Jepp1"
        vilkårsvurderingFrontendDto.perioder.first().vilkårsvurderingsresultatInfo?.godTro?.begrunnelse shouldBe "Japp1"
    }

    @Test
    fun `slår sammen flere perioder`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
                perioder = listOf(
                    KravgrunnlagGenerator.standardPeriode(1.januar(2021) til 1.januar(2021)),
                    KravgrunnlagGenerator.standardPeriode(15.mars(2021) til 15.mars(2021)),
                    KravgrunnlagGenerator.standardPeriode(21.mai(2021) til 21.mai(2021)),
                    KravgrunnlagGenerator.standardPeriode(14.juli(2021) til 14.juli(2021)),
                ),
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
        somSaksbehandler(ansvarligSaksbehandler) {
            vilkårsvurderingController.hentVurdertVilkårsvurdering(behandlingId).data shouldNotBeNull {
                perioder.size shouldBe 1
                perioder.first().periode shouldBe (1.januar(2021) til 14.juli(2021))
            }
        }

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(
                1.januar(2021) til 1.januar(2021),
                15.mars(2021) til 15.mars(2021),
                21.mai(2021) til 21.mai(2021),
                14.juli(2021) til 14.juli(2021),
            ),
        )

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegVilkårsvurderingDto(
                vilkårsvurderingsperioder = listOf(
                    VilkårsvurderingsperiodeDto(
                        periode = 1.januar(2021) til 14.juli(2021),
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

        val tilbakekreving = tilbakekreving(behandlingId)
        val vilkårsvurderingFrontendDto = tilbakekreving.hentBehandling(behandlingId).vilkårsvurderingsstegDto.tilFrontendDto(saksbehandlerContext())
        vilkårsvurderingFrontendDto.perioder.size shouldBe 1
        vilkårsvurderingFrontendDto.perioder.first().begrunnelse shouldBe "Jepp1"
        vilkårsvurderingFrontendDto.perioder.first().vilkårsvurderingsresultatInfo?.godTro?.begrunnelse shouldBe "Japp1"
        vilkårsvurderingFrontendDto.perioder.first().feilutbetaltBeløp shouldBe 8000.0.kroner
    }

    @ParameterizedTest
    @ValueSource(strings = ["SIMPEL_UAKTSOMHET", "GROV_UAKTSOMHET", "FORSETT"])
    fun `begrunnelse for aktsomhet forårsaket av bruker`(aktsomhet: Aktsomhet) {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))

        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
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
            stegData = BehandlingsstegVilkårsvurderingDto(
                vilkårsvurderingsperioder = listOf(
                    VilkårsvurderingsperiodeDto(
                        periode = 1.januar(2021) til 1.januar(2021),
                        vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
                        begrunnelse = "Jepp",
                        aktsomhetDto = AktsomhetDto(
                            aktsomhet = aktsomhet,
                            ileggRenter = false,
                            andelTilbakekreves = null,
                            beløpTilbakekreves = null,
                            begrunnelse = "Japp",
                            særligeGrunner = emptyList(),
                            særligeGrunnerTilReduksjon = false,
                            unnlates4Rettsgebyr = SkalUnnlates.UNNLATES,
                            særligeGrunnerBegrunnelse = "",
                        ),
                    ),
                ),
            ),
        )

        val tilbakekreving = tilbakekreving(behandlingId)
        val vilkårsvurderingFrontendDto = tilbakekreving.hentBehandling(behandlingId).vilkårsvurderingsstegDto.tilFrontendDto(saksbehandlerContext())
        vilkårsvurderingFrontendDto.perioder.size shouldBe 1
        vilkårsvurderingFrontendDto.perioder.single().begrunnelse shouldBe "Jepp"
        vilkårsvurderingFrontendDto.perioder.single().vilkårsvurderingsresultatInfo?.aktsomhet?.begrunnelse shouldBe "Japp"
    }

    @Test
    fun `endringer i behandling skal føre til kafka-meldinger til fagsystem`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )

        val eksternBehandlingId = "ekstern_behandling_id"

        fagsystemIntegrasjonService.håndter(
            Ytelse.Tilleggsstønad,
            Testdata.fagsysteminfoSvar(
                fagsystemId = fagsystemId,
                eksternBehandlingId = eksternBehandlingId,
                utvidPerioder = listOf(
                    FagsysteminfoSvarHendelse.UtvidetPeriodeDto(
                        kravgrunnlagPeriode = PeriodeDto(1.januar(2021), 1.januar(2021)),
                        vedtaksperiode = PeriodeDto(1.januar(2021), 31.januar(2021)),
                    ),
                ),
            ),
        )

        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        lagreUttalelse(behandlingId)

        tilbakekrevVedtak(behandlingId, listOf(1.januar(2021) til 31.januar(2021)))

        val event = kafkaProducer.finnHendelse<BehandlingEndretEventDto>(fagsystemId)
            .lastOrNull()
            .shouldNotBeNull()

        event.eksternFagsakId shouldBe fagsystemId
        event.hendelseOpprettet.toLocalDate() shouldBe LocalDate.now()
        event.eksternBehandlingId shouldBe eksternBehandlingId
        event.tilbakekreving.behandlingId shouldBe behandlingId
        event.tilbakekreving.sakOpprettet.toLocalDate() shouldBe LocalDate.now()
        event.tilbakekreving.varselSendt shouldBe null
        event.tilbakekreving.behandlingsstatus shouldBe BehandlingsstatusEventDto.AVSLUTTET
        event.tilbakekreving.totaltFeilutbetaltBeløp shouldBe 2000.0.kroner
        event.tilbakekreving.fullstendigPeriode shouldBe PeriodeEventDto(1.januar(2021), 31.januar(2021))
        event.tilbakekreving.saksbehandlingURL shouldBe "https://tilbakekreving.intern.nav.no/fagsystem/TS/fagsak/$fagsystemId/behandling/$behandlingId"
    }

    @Test
    fun `vurdering av oppdaget blir lagret for faktasteget`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))
        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()

        val oppdagetDato = LocalDate.now()
        somSaksbehandler("Z999999") {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = OppdaterFaktaOmFeilutbetalingDto(
                    perioder = null,
                    vurdering = VurderingDto(
                        årsak = "Årsak til feilutbetaling",
                        oppdaget = OppdagetDto(
                            dato = oppdagetDato,
                            av = OppdagetDto.Av.BRUKER,
                            beskrivelse = "Beskrivelse av oppdagelse",
                        ),
                    ),
                ),
            ).statusCode shouldBe HttpStatus.OK
        }

        somSaksbehandler("Z999999") {
            behandlingApiController.behandlingFakta(
                behandlingId = behandlingId.toString(),
            ).body?.vurdering?.oppdaget shouldBe OppdagetDto(
                dato = oppdagetDato,
                av = OppdagetDto.Av.BRUKER,
                beskrivelse = "Beskrivelse av oppdagelse",
            )
        }
    }

    @Test
    fun `vurdering av årsak blir lagret for faktasteget`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))
        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()

        somSaksbehandler("Z999999") {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = OppdaterFaktaOmFeilutbetalingDto(
                    vurdering = VurderingDto(
                        årsak = "årsak",
                    ),
                ),
            ).statusCode shouldBe HttpStatus.OK
        }

        somSaksbehandler("Z999999") {
            behandlingApiController.behandlingFakta(
                behandlingId = behandlingId.toString(),
            ).body?.vurdering?.årsak shouldBe "årsak"
        }
    }

    @Test
    fun `vurdering av perioder blir lagret for faktasteget`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))
        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()

        somSaksbehandler("Z999999") {
            behandlingApiController.behandlingFakta(
                behandlingId = behandlingId.toString(),
            ).body?.perioder?.single()?.rettsligGrunnlag shouldBe listOf(
                RettsligGrunnlagDto(
                    bestemmelse = Hendelsestype.ANNET.name,
                    grunnlag = Hendelsesundertype.ANNET_FRITEKST.name,
                ),
            )
        }

        val periodeId = tilbakekreving(behandlingId).shouldNotBeNull().tilFeilutbetalingFrontendDto(behandlingId, SystemKlokke).perioder.single().id
        val faktaPerioder = listOf(
            OppdaterFaktaPeriodeDto(
                id = periodeId,
                rettsligGrunnlag = listOf(
                    RettsligGrunnlagDto(
                        bestemmelse = Hendelsestype.VILKÅR_SØKER.name,
                        grunnlag = Hendelsesundertype.KONTANTSTØTTE.name,
                    ),
                ),
            ),
        )
        somSaksbehandler("Z999999") {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = OppdaterFaktaOmFeilutbetalingDto(
                    perioder = faktaPerioder,
                ),
            ).statusCode shouldBe HttpStatus.OK
        }

        somSaksbehandler("Z999999") {
            behandlingApiController.behandlingFakta(
                behandlingId = behandlingId.toString(),
            ).body?.perioder?.single()?.rettsligGrunnlag shouldBe
                listOf(
                    RettsligGrunnlagDto(
                        bestemmelse = Hendelsestype.VILKÅR_SØKER.name,
                        grunnlag = Hendelsesundertype.KONTANTSTØTTE.name,
                    ),
                )
        }
    }

    @Test
    fun `nullstilling av perioder, årsak og vurdering blir lagret i faktasteget`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, årsakTilFeilutbetaling = "original"))
        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()

        val periodeId = tilbakekreving(behandlingId).shouldNotBeNull().tilFeilutbetalingFrontendDto(behandlingId, SystemKlokke).perioder.single().id
        val faktaPerioder = listOf(
            OppdaterFaktaPeriodeDto(
                id = periodeId,
                rettsligGrunnlag = listOf(
                    RettsligGrunnlagDto(
                        bestemmelse = Hendelsestype.ANNET.name,
                        grunnlag = Hendelsesundertype.ANNET_FRITEKST.name,
                    ),
                ),
            ),
        )
        val oppdagetDato = LocalDate.now()
        somSaksbehandler("Z999999") {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = OppdaterFaktaOmFeilutbetalingDto(
                    perioder = faktaPerioder,
                    vurdering = VurderingDto(
                        årsak = "oppdatert",
                        oppdaget = OppdagetDto(
                            dato = oppdagetDato,
                            av = OppdagetDto.Av.BRUKER,
                            beskrivelse = "Beskrivelse av oppdagelse",
                        ),
                    ),
                ),
            ).statusCode shouldBe HttpStatus.OK
        }

        somSaksbehandler("Z999999") {
            behandlingController.flyttBehandlingTilFakta(behandlingId).status shouldBe Ressurs.Status.SUKSESS
        }
        somSaksbehandler("Z999999") {
            val faktaSteg = behandlingApiController.behandlingFakta(
                behandlingId = behandlingId.toString(),
            ).body

            faktaSteg?.perioder?.single()?.rettsligGrunnlag shouldBe listOf(
                RettsligGrunnlagDto(
                    bestemmelse = Hendelsestype.ANNET.name,
                    grunnlag = Hendelsesundertype.ANNET_FRITEKST.name,
                ),
            )
            faktaSteg?.vurdering shouldBe VurderingDto(
                årsak = "original",
                oppdaget = OppdagetDto(
                    dato = null,
                    av = OppdagetDto.Av.IKKE_VURDERT,
                    beskrivelse = null,
                ),
            )
        }
    }

    @Test
    fun `trenger ny vurdering av steg blir lagret`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))

        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
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

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagForeslåVedtakVurdering(),
        )

        utførSteg(
            ident = ansvarligBeslutter,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeGodkjennVedtakVurdering(),
        )

        val behandling = somSaksbehandler(ansvarligSaksbehandler) { behandlingController.hentBehandling(behandlingId).data.shouldNotBeNull() }
        behandling.status shouldBe Behandlingsstatus.UTREDES
        behandling.behandlingsstegsinfo.skalHaSteg(Behandlingssteg.FAKTA).behandlingsstegstatus shouldBe Behandlingsstegstatus.TILBAKEFØRT
        behandling.behandlingsstegsinfo.skalHaSteg(Behandlingssteg.FORHÅNDSVARSEL).behandlingsstegstatus shouldBe Behandlingsstegstatus.TILBAKEFØRT
        behandling.behandlingsstegsinfo.skalHaSteg(Behandlingssteg.FORELDELSE).behandlingsstegstatus shouldBe Behandlingsstegstatus.TILBAKEFØRT
        behandling.behandlingsstegsinfo.skalHaSteg(Behandlingssteg.VILKÅRSVURDERING).behandlingsstegstatus shouldBe Behandlingsstegstatus.TILBAKEFØRT
        behandling.behandlingsstegsinfo.skalHaSteg(Behandlingssteg.FORESLÅ_VEDTAK).behandlingsstegstatus shouldBe Behandlingsstegstatus.TILBAKEFØRT
    }

    private fun List<BehandlingsstegsinfoDto>.skalHaSteg(behandlingssteg: Behandlingssteg): BehandlingsstegsinfoDto {
        return this.singleOrNull { it.behandlingssteg == behandlingssteg } ?: fail("Fant ikke $behandlingssteg i ${this.map(BehandlingsstegsinfoDto::behandlingssteg)}")
    }
}
