package no.nav.familie.tilbake.iverksettvedtak

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forOne
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingsvedtakService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.FagsakService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.common.exceptionhandler.IntegrasjonException
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.OppdragClientRestMock
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.iverksettvedtak.domain.KodeResultat
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.GjelderType
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingService
import no.nav.tilbakekreving.api.v1.dto.AktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.SkalUnnlates
import no.nav.tilbakekreving.api.v1.dto.SærligGrunnDto
import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.KodeAksjonDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.PosteringDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.TilbakekrevingsvedtakRequestDto
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsresultatstype
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.vedtak.IverksettRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.math.BigInteger
import java.time.YearMonth
import java.util.UUID

internal class IverksettelseServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @Autowired
    private lateinit var tilbakekrevingsvedtakBeregningService: TilbakekrevingsvedtakBeregningService

    @Autowired
    private lateinit var behandlingVedtakService: BehandlingsvedtakService

    @Autowired
    private lateinit var beregningService: TilbakekrevingsberegningService

    @Autowired
    private lateinit var logService: LogService

    private lateinit var iverksettelseService: IverksettelseService

    @Autowired
    private lateinit var oppdragRestClient: OppdragClientRestMock

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var iverksettRepository: IverksettRepository

    private lateinit var fagsak: Fagsak
    private lateinit var behandling: Behandling
    private lateinit var behandlingId: UUID
    private val perioder = listOf(
        Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 1)),
        Månedsperiode(YearMonth.of(2021, 2), YearMonth.of(2021, 2)),
    )
    private lateinit var kravgrunnlag431: Kravgrunnlag431

    @BeforeEach
    fun init() {
        fagsak = Testdata.fagsak()
        behandling = Testdata.lagBehandling(fagsakId = fagsak.id)
        behandlingId = behandling.id
        fagsakRepository.insert(fagsak)
        behandlingRepository.insert(behandling)

        kravgrunnlag431 = lagKravgrunnlag()
        lagVilkårsvurdering()

        behandlingVedtakService.opprettBehandlingsvedtak(behandlingId)

        iverksettelseService =
            IverksettelseService(
                behandlingRepository,
                kravgrunnlagRepository,
                tilbakekrevingsvedtakBeregningService,
                behandlingVedtakService,
                oppdragRestClient,
                logService,
                fagsakRepository,
                iverksettRepository,
            )
    }

    @Test
    fun `sendIverksettVedtak skal sende iverksettvedtak til økonomi for suksess respons`() {
        mockIverksettelseResponse(kravgrunnlag431.vedtakId, "00", "OK")

        iverksettelseService.sendIverksettVedtak(behandlingId)

        oppdragRestClient.shouldHaveIverksettelse(kravgrunnlag431.vedtakId) {
            assertRequest(it, kravgrunnlag431.vedtakId)
        }

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val aktivBehandlingsresultat = behandling.sisteResultat
        aktivBehandlingsresultat.shouldNotBeNull()
        aktivBehandlingsresultat.type shouldBe Behandlingsresultatstype.FULL_TILBAKEBETALING
    }

    @Test
    fun `sendIverksettVedtak skal sende iverksettvedtak til økonomi for feil respons`() {
        mockIverksettelseResponse(kravgrunnlag431.vedtakId, "10", "feil")

        val exception = shouldThrow<RuntimeException> { iverksettelseService.sendIverksettVedtak(behandlingId) }

        exception.shouldBeInstanceOf<IntegrasjonException>()
        exception.message shouldBe "Fikk feil respons fra økonomi ved iverksetting. Mottatt respons: 10, feil"

        oppdragRestClient.shouldHaveIverksettelse(kravgrunnlag431.vedtakId) {}
        val iverksattVedtak = iverksettRepository.findByBehandlingId(behandlingId)
        iverksattVedtak.shouldBeNull()
    }

    @Test
    fun `sendIverksettVedtak for allerede iverksatt behandling - skal returnere KVITTERING_OK og ikke iverksette`() {
        mockIverksettelseResponse(kravgrunnlag431.vedtakId, "08", "B441012F") // Denne kan håndteres dersom oppdrag skiller på om vedtak finnes eller om det er feil status

        val exception = shouldThrow<RuntimeException> { iverksettelseService.sendIverksettVedtak(behandlingId) }
        exception.shouldBeInstanceOf<IntegrasjonException>()
    }

    private fun mockIverksettelseResponse(
        vedtakId: BigInteger,
        alvorlighetsgrad: String,
        kodeMelding: String,
    ) {
        oppdragRestClient.mockIversettelse(vedtakId, alvorlighetsgrad, kodeMelding)
    }

    private fun lagKravgrunnlag(): Kravgrunnlag431 {
        val feilPostering = lagKravgrunnlagsbeløp(
            klassetype = Klassetype.FEIL,
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            nyttBeløp = BigDecimal(5000),
        )

        val ytelPostering = lagKravgrunnlagsbeløp(
            klassetype = Klassetype.YTEL,
            klassekode = Klassekode.BATR,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(5000),
        )

        val kravgrunnlagsperioder = perioder
            .map {
                Kravgrunnlagsperiode432(
                    periode = it,
                    månedligSkattebeløp = BigDecimal.ZERO,
                    beløp =
                        setOf(
                            feilPostering.copy(id = UUID.randomUUID()),
                            ytelPostering.copy(id = UUID.randomUUID()),
                        ),
                )
            }.toSet()

        return kravgrunnlagRepository.insert(
            Kravgrunnlag431(
                behandlingId = behandlingId,
                vedtakId = KravgrunnlagGenerator.nextPaddedId(6).toBigInteger(),
                kravstatuskode = Kravstatuskode.NYTT,
                fagområdekode = Fagområdekode.BA,
                fagsystemId = fagsak.eksternFagsakId,
                gjelderVedtakId = "testverdi",
                gjelderType = GjelderType.PERSON,
                utbetalesTilId = "testverdi",
                utbetIdType = GjelderType.PERSON,
                ansvarligEnhet = "testverdi",
                bostedsenhet = "testverdi",
                behandlingsenhet = "testverdi",
                kontrollfelt = "2025-12-24-11.12.13.123456",
                referanse = behandling.aktivFagsystemsbehandling.eksternId,
                eksternKravgrunnlagId = BigInteger.ZERO,
                saksbehandlerId = "testverdi",
                perioder = kravgrunnlagsperioder,
            ),
        )
    }

    private fun lagKravgrunnlagsbeløp(
        klassetype: Klassetype,
        klassekode: Klassekode,
        nyttBeløp: BigDecimal = BigDecimal.ZERO,
        utbetaltBeløp: BigDecimal = BigDecimal.ZERO,
        tilbakekrevesBeløp: BigDecimal = BigDecimal.ZERO,
    ): Kravgrunnlagsbeløp433 = Kravgrunnlagsbeløp433(
        klassetype = klassetype,
        klassekode = klassekode,
        nyttBeløp = nyttBeløp,
        opprinneligUtbetalingsbeløp = utbetaltBeløp,
        tilbakekrevesBeløp = tilbakekrevesBeløp,
        skatteprosent = BigDecimal.ZERO,
    )

    private fun lagVilkårsvurdering() {
        val vilkårsperioder = perioder.map {
            VilkårsvurderingsperiodeDto(
                periode = it.toDatoperiode(),
                begrunnelse = "testverdi",
                aktsomhetDto =
                    AktsomhetDto(
                        aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                        begrunnelse = "testverdi",
                        særligeGrunnerTilReduksjon = false,
                        unnlates4Rettsgebyr = SkalUnnlates.TILBAKEKREVES,
                        særligeGrunnerBegrunnelse = "testverdi",
                        særligeGrunner =
                            listOf(
                                SærligGrunnDto(
                                    særligGrunn = SærligGrunnType.ANNET,
                                    begrunnelse = "testverdi",
                                ),
                            ),
                    ),
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
            )
        }
        vilkårsvurderingService.lagreVilkårsvurdering(behandling.id, BehandlingsstegVilkårsvurderingDto(vilkårsperioder))
    }

    private fun assertRequest(request: TilbakekrevingsvedtakRequestDto, expectedVedtakId: BigInteger) {
        request.kodeAksjon shouldBe KodeAksjonDto.FATTE_VEDTAK
        request.vedtaksDato.shouldNotBeNull()
        request.vedtakId shouldBe expectedVedtakId
        request.kodeHjemmel shouldBe "22-15"
        request.enhetAnsvarlig shouldBe kravgrunnlag431.ansvarligEnhet

        val førstePeriode = request.perioder[0]
        førstePeriode.periodeFom.shouldNotBeNull()
        førstePeriode.periodeTom.shouldNotBeNull()
        førstePeriode.belopRenter shouldBe BigDecimal.ZERO
        førstePeriode.posteringer.size shouldBe 2
        assertBeløp(
            beløpene = førstePeriode.posteringer,
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = null,
            kodeÅrsak = null,
            kodeSkyld = null,
        )
        assertBeløp(
            beløpene = førstePeriode.posteringer,
            klassekode = Klassekode.BATR,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(5000),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
            kodeÅrsak = "ANNET",
            kodeSkyld = "IKKE_FORDELT",
        )

        val andrePeriode = request.perioder[1]
        andrePeriode.periodeFom.shouldNotBeNull()
        andrePeriode.periodeTom.shouldNotBeNull()
        andrePeriode.belopRenter shouldBe BigDecimal.ZERO
        andrePeriode.posteringer.size shouldBe 2
        assertBeløp(
            beløpene = andrePeriode.posteringer,
            klassekode = Klassekode.KL_KODE_FEIL_BA,
            nyttBeløp = BigDecimal(5000),
            kodeResultat = null,
            kodeÅrsak = null,
            kodeSkyld = null,
        )
        assertBeløp(
            beløpene = andrePeriode.posteringer,
            klassekode = Klassekode.BATR,
            utbetaltBeløp = BigDecimal(5000),
            tilbakekrevesBeløp = BigDecimal(5000),
            kodeResultat = KodeResultat.FULL_TILBAKEKREVING,
            kodeÅrsak = "ANNET",
            kodeSkyld = "IKKE_FORDELT",
        )
    }

    private fun assertBeløp(
        beløpene: List<PosteringDto>,
        klassekode: Klassekode,
        nyttBeløp: BigDecimal = BigDecimal.ZERO,
        utbetaltBeløp: BigDecimal = BigDecimal.ZERO,
        tilbakekrevesBeløp: BigDecimal = BigDecimal.ZERO,
        uinnkrevdBeløp: BigDecimal = BigDecimal.ZERO,
        skattBeløp: BigDecimal = BigDecimal.ZERO,
        kodeResultat: KodeResultat?,
        kodeÅrsak: String?,
        kodeSkyld: String?,
    ) {
        beløpene.forOne {
            it.kodeKlasse shouldBe klassekode.name
            it.belopNy shouldBe nyttBeløp
            it.belopOpprinneligUtbetalt shouldBe utbetaltBeløp
            it.belopTilbakekreves shouldBe tilbakekrevesBeløp
            it.belopUinnkrevd shouldBe uinnkrevdBeløp
            it.belopSkatt shouldBe skattBeløp
        }

        beløpene.forOne {
            it.kodeResultat shouldBe (kodeResultat?.kode ?: "")
            it.kodeAarsak shouldBe (kodeÅrsak ?: "")
            it.kodeSkyld shouldBe (kodeSkyld ?: "")
        }
    }
}
