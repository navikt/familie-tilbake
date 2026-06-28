package no.nav.tilbakekreving.e2e

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAny
import io.kotest.inspectors.forOne
import io.kotest.inspectors.forSingle
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.config.OppdragClientMock
import no.nav.familie.tilbake.config.PdlClientMock
import no.nav.familie.tilbake.datavarehus.saksstatistikk.SendSakshendelseTilDvhTask
import no.nav.familie.tilbake.forvaltning.ForvaltningService
import no.nav.familie.tilbake.forvaltning.ForvaltningServiceTest.Companion.assertBehandlingssteg
import no.nav.familie.tilbake.forvaltning.ForvaltningServiceTest.Companion.lagMottattXml
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.Historikkinnslag
import no.nav.familie.tilbake.historikkinnslag.HistorikkinnslagRepository
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.iverksettvedtak.domain.ØkonomiXmlSendt
import no.nav.familie.tilbake.iverksettvedtak.ØkonomiXmlSendtRepository
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.familie.tilbake.kravgrunnlag.HentKravgrunnlagService
import no.nav.familie.tilbake.kravgrunnlag.HentKravgrunnlagTaskTest
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.familie.tilbake.kravgrunnlag.task.HentKravgrunnlagTask
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.oppgave.FerdigstillOppgaveTask
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.api.v1.dto.BehandlerRolle
import no.nav.tilbakekreving.api.v1.dto.OpprettRevurderingDto
import no.nav.tilbakekreving.api.v2.PeriodeDto
import no.nav.tilbakekreving.api.v2.fagsystem.svar.FagsysteminfoSvarHendelse
import no.nav.tilbakekreving.avstemmingMediator.AvstemmingMediator
import no.nav.tilbakekreving.avstemmingMediator.AvstemmingMediatorTest
import no.nav.tilbakekreving.avstemmingMediator.AvstemmingMediatorTest.Companion.lagTilbakekrevingsvedtakDto
import no.nav.tilbakekreving.avstemmingMediator.AvstemmingMediatorTest.Companion.readXml
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsbeløp.Companion.medFeilutbetaling
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.entities.AktørEntity
import no.nav.tilbakekreving.entities.AktørType
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.fagsystem.Ytelsestype
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.integrasjoner.KafkaProducerStub
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsresultatstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.kontrakter.historikk.Historikkinnslagstype
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.test.FellesTestdata.BESLUTTER_IDENT
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.util.kroner
import no.nav.tilbakekreving.vedtak.IverksattVedtak
import no.nav.tilbakekreving.vedtak.IverksettRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.math.BigInteger
import java.time.LocalDate
import java.util.UUID
import kotlin.random.Random
import no.nav.familie.tilbake.data.Testdata as GammelTestdata

@TestPropertySource(
    properties = ["tilbakekreving.toggles.nyModell.OppdragRestClient=false"],
)
class KravgrunnlagSoapE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Autowired
    private lateinit var kafkaProducerStub: KafkaProducerStub

    @Autowired
    private lateinit var oppdragClient: OppdragClientMock

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    lateinit var avstemmingMediator: AvstemmingMediator

    @Autowired
    lateinit var iverksettRepository: IverksettRepository

    @Autowired
    lateinit var økonomiXmlSendtRepository: ØkonomiXmlSendtRepository

    @Autowired
    lateinit var gammelBehandlingRepository: BehandlingRepository

    @Autowired
    lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    lateinit var forvaltningService: ForvaltningService

    @Autowired
    lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    lateinit var taskService: TaskService

    @Autowired
    lateinit var historikkService: HistorikkService

    @Autowired
    private lateinit var økonomiXmlMottattRepository: ØkonomiXmlMottattRepository

    @Autowired
    private lateinit var hentKravgrunnlagService: HentKravgrunnlagService

    @Autowired
    private lateinit var logService: LogService

    @Autowired
    private lateinit var stegService: StegService

    @Autowired
    private lateinit var historikkinnslagRepository: HistorikkinnslagRepository

    @Test
    fun `iverksettelse av vedtak med utvidet periode - SOAP`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            QUEUE_NAME,
            KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
                perioder = listOf(
                    KravgrunnlagGenerator.standardPeriode(1.januar(2021) til 1.januar(2021)),
                ),
            ),
        )
        fagsystemIntegrasjonService.håndter(
            Ytelse.Tilleggsstønad,
            Testdata.fagsysteminfoSvar(
                fagsystemId = fagsystemId,
                utvidPerioder = listOf(
                    FagsysteminfoSvarHendelse.UtvidetPeriodeDto(
                        kravgrunnlagPeriode = PeriodeDto(fom = 1.januar(2021), tom = 1.januar(2021)),
                        vedtaksperiode = PeriodeDto(fom = 1.januar(2021), tom = 31.januar(2021)),
                    ),
                ),
            ),
        )

        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        lagreUttalelse(behandlingId)

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }

        utførSteg(behandlingId, BehandlingsstegGenerator.lagIkkeForeldetVurdering(1.januar(2021) til 31.januar(2021)))
        utførSteg(behandlingId, BehandlingsstegGenerator.lagVilkårsvurderingFullTilbakekreving(1.januar(2021) til 31.januar(2021)))
        utførSteg(behandlingId, BehandlingsstegGenerator.lagForeslåVedtakVurdering())
        utførSteg(behandlingId, BehandlingsstegGenerator.lagGodkjennVedtakVurdering(), BESLUTTER_IDENT)

        oppdragClient.shouldHaveIverksettelse(behandlingId) { vedtak ->
            vedtak.tilbakekrevingsperiode shouldHaveSize 1
            val tilbakekrevingsperiode = vedtak.tilbakekrevingsperiode.single()
            tilbakekrevingsperiode.tilbakekrevingsbelop shouldHaveSize 2
            tilbakekrevingsperiode.tilbakekrevingsbelop.forOne { beløp ->
                beløp.kodeResultat shouldBe "FULL_TILBAKEKREV"
            }
        }

        val periode = kafkaProducerStub.finnVedtaksoppsummering(behandlingId).single().perioder.single()
        periode.fom shouldBe 1.januar(2021)
        periode.tom shouldBe 31.januar(2021)
    }

    @Test
    fun `det opprettes en korrekt iverksettelse for begge behandlinger for samme fagsystemid - SOAP`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)

        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
                perioder = listOf(KravgrunnlagGenerator.standardPeriode(1.januar(2021) til 1.januar(2021), feilutbetaltBeløp = 2000.kroner)),
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))

        val førsteBehandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        lagreUttalelse(førsteBehandlingId)
        tilbakekrevVedtak(førsteBehandlingId, listOf(1.januar(2021) til 31.januar(2021)))

        tilbakekreving(førsteBehandlingId)
            .frontendDtoForBehandling(førsteBehandlingId, saksbehandlerContext(), true, BehandlerRolle.BESLUTTER)
            .status shouldBe Behandlingsstatus.AVSLUTTET

        oppdragClient.shouldHaveIverksettelse(førsteBehandlingId) { vedtak ->
            vedtak.tilbakekrevingsperiode shouldHaveSize 1
            vedtak.tilbakekrevingsperiode.single().tilbakekrevingsbelop.forOne { beløp ->
                beløp.kodeResultat shouldBe "FULL_TILBAKEKREV"
                beløp.belopTilbakekreves shouldBe 2000.kroner
            }
        }

        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
                perioder = listOf(KravgrunnlagGenerator.standardPeriode(1.januar(2021) til 1.januar(2021), feilutbetaltBeløp = 1000.kroner)),
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))

        val nyBehandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        nyBehandlingId shouldNotBe førsteBehandlingId
        lagreUttalelse(nyBehandlingId)
        tilbakekrevVedtak(nyBehandlingId, listOf(1.januar(2021) til 31.januar(2021)))

        tilbakekreving(nyBehandlingId)
            .frontendDtoForBehandling(nyBehandlingId, saksbehandlerContext(), true, BehandlerRolle.BESLUTTER)
            .status shouldBe Behandlingsstatus.AVSLUTTET

        oppdragClient.shouldHaveIverksettelse(nyBehandlingId) { vedtak ->
            vedtak.tilbakekrevingsperiode shouldHaveSize 1
            vedtak.tilbakekrevingsperiode.single().tilbakekrevingsbelop.forOne { beløp ->
                beløp.kodeResultat shouldBe "FULL_TILBAKEKREV"
                beløp.belopTilbakekreves shouldBe 1000.kroner
            }
        }
    }

    @Test
    fun `kravgrunnlag fører til sak klar til behandling`() {
        val fnr = Random.nextLong(0, 31129999999).toString().padStart(11, '0')
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        val vedtakId = KravgrunnlagGenerator.nextPaddedId(6)
        val ansvarligEnhet = KravgrunnlagGenerator.nextPaddedId(4)
        sendKravgrunnlagOgAvventLesing(
            TILLEGGSSTØNADER_KØ_NAVN,
            KravgrunnlagGenerator.forTilleggsstønader(
                fødselsnummer = fnr,
                fagsystemId = fagsystemId,
                vedtakId = vedtakId,
                ansvarligEnhet = ansvarligEnhet,
                perioder = listOf(
                    KravgrunnlagGenerator.Tilbakekrevingsperiode(
                        1.januar(2021) til 1.januar(2021),
                        tilbakekrevingsbeløp = listOf(
                            KravgrunnlagGenerator.Tilbakekrevingsbeløp.forKlassekode(
                                klassekode = KravgrunnlagGenerator.NyKlassekode.TSTBASISP4_OP,
                                beløpTilbakekreves = 2000.kroner,
                                beløpOpprinneligUtbetalt = 20000.kroner,
                            ),
                        ).medFeilutbetaling(KravgrunnlagGenerator.NyKlassekode.KL_KODE_FEIL_ARBYT),
                    ),
                ),
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))

        pdlClient.hentPersoninfoHits(fnr) shouldBe listOf(
            PdlClientMock.PersoninfoHit(
                ident = fnr,
                fagsystem = FagsystemDTO.TS,
            ),
        )

        val frontendDto = tilbakekreving(FagsystemDTO.TS, fagsystemId)
            .shouldNotBeNull()
            .tilFrontendDto(saksbehandlerContext().klokke)
        frontendDto.behandlinger shouldHaveSize 1
        frontendDto.behandlinger.single().status shouldBe Behandlingsstatus.UTREDES
        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()

        tilbakekreving(behandlingId) kanBehandle Behandlingssteg.FAKTA
        tilbakekreving(behandlingId) avventerBehandling Behandlingssteg.FORHÅNDSVARSEL

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }

        tilbakekreving(behandlingId) kanBehandle Behandlingssteg.FORHÅNDSVARSEL
        tilbakekreving(behandlingId) avventerBehandling Behandlingssteg.FORELDELSE

        lagreUttalelse(behandlingId)

        tilbakekreving(behandlingId) kanBehandle Behandlingssteg.FORELDELSE
        tilbakekreving(behandlingId) avventerBehandling Behandlingssteg.VILKÅRSVURDERING

        utførSteg(
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(1.januar(2021) til 1.januar(2021)),
        )

        behandling(behandlingId).vilkårsvurderingsstegDto.tilFrontendDto(saksbehandlerContext()).perioder[0].foreldet shouldBe false

        tilbakekreving(behandlingId) kanBehandle Behandlingssteg.VILKÅRSVURDERING
        tilbakekreving(behandlingId) avventerBehandling Behandlingssteg.FORESLÅ_VEDTAK

        utførSteg(
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingFullTilbakekreving(1.januar(2021) til 1.januar(2021)),
        )

        tilbakekreving(behandlingId) kanBehandle Behandlingssteg.FORESLÅ_VEDTAK
        tilbakekreving(behandlingId) avventerBehandling Behandlingssteg.FATTE_VEDTAK

        utførSteg(behandlingId, BehandlingsstegGenerator.lagForeslåVedtakVurdering())
        tilbakekreving(behandlingId) kanBehandle Behandlingssteg.FATTE_VEDTAK
        tilbakekreving(behandlingId) avventerBehandling Behandlingssteg.IVERKSETT_VEDTAK

        utførSteg(
            ident = BESLUTTER_IDENT,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagGodkjennVedtakVurdering(),
        )
        oppdragClient.shouldHaveIverksettelse(behandlingId) { vedtak ->
            vedtak.vedtakId shouldBe BigInteger(vedtakId)
            vedtak.kodeAksjon shouldBe KodeAksjon.FATTE_VEDTAK.kode
            vedtak.kodeHjemmel shouldBe "22-15"
            vedtak.datoVedtakFagsystem shouldBe LocalDate.now()
            vedtak.enhetAnsvarlig shouldBe ansvarligEnhet
            vedtak.kontrollfelt shouldBe "2025-12-24-11.12.13.123456"
            vedtak.saksbehId shouldBe SAKSBEHANDLER_IDENT
            vedtak.tilbakekrevingsperiode.forSingle { periode ->
                periode.periode.fom shouldBe 1.januar(2021)
                periode.periode.tom shouldBe 1.januar(2021)
                periode.belopRenter shouldBe 0.kroner
                periode.tilbakekrevingsbelop shouldHaveSize 2
                periode.tilbakekrevingsbelop.forOne { beløp ->
                    beløp.kodeKlasse shouldBe "TSTBASISP4-OP"
                    beløp.belopNy shouldBe 18000.kroner
                    beløp.belopOpprUtbet shouldBe 20000.0.kroner
                    beløp.belopTilbakekreves shouldBe 2000.kroner
                    beløp.belopUinnkrevd shouldBe 0.kroner
                    beløp.belopSkatt shouldBe 0.kroner
                    beløp.kodeResultat shouldBe "FULL_TILBAKEKREV"
                    beløp.kodeAarsak shouldBe "ANNET"
                    beløp.kodeSkyld shouldBe "IKKE_FORDELT"
                }
                periode.tilbakekrevingsbelop.forOne { beløp ->
                    beløp.kodeKlasse shouldBe "KL_KODE_FEIL_ARBYT"
                    beløp.belopNy shouldBe 2000.0.kroner
                    beløp.belopOpprUtbet shouldBe 0.kroner
                    beløp.belopTilbakekreves shouldBe 0.kroner
                    beløp.belopUinnkrevd shouldBe 0.kroner
                    beløp.belopSkatt shouldBe 0.kroner
                    beløp.kodeResultat shouldBe null
                    beløp.kodeAarsak shouldBe null
                    beløp.kodeSkyld shouldBe null
                }
            }
        }
    }

    @Test
    fun `revurdering av vedtak med full utbetaling fører til ingen tilbakekreving`() {
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

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }
        utførSteg(behandlingId, BehandlingsstegGenerator.lagIkkeForeldetVurdering())
        utførSteg(behandlingId, BehandlingsstegGenerator.lagVilkårsvurderingFullTilbakekreving())
        utførSteg(behandlingId, BehandlingsstegGenerator.lagForeslåVedtakVurdering())
        utførSteg(
            ident = BESLUTTER_IDENT,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagGodkjennVedtakVurdering(),
        )

        oppdragClient.shouldHaveIverksettelse(behandlingId) { vedtak ->
            vedtak.tilbakekrevingsperiode shouldHaveSize 1
            val tilbakekrevingsperiode = vedtak.tilbakekrevingsperiode.single()
            tilbakekrevingsperiode.tilbakekrevingsbelop shouldHaveSize 2
            tilbakekrevingsperiode.tilbakekrevingsbelop.forOne { beløp ->
                beløp.kodeResultat shouldBe "FULL_TILBAKEKREV"
            }
        }

        val exception = shouldThrow<ModellFeil.UtenforScopeException> {
            behandlingController.opprettRevurdering(
                OpprettRevurderingDto(
                    YtelsestypeDTO.TILLEGGSSTØNAD,
                    behandlingId,
                    Behandlingsårsakstype.REVURDERING_KLAGE_KA,
                ),
            )
        }

        exception.utenforScope shouldBe UtenforScope.Revurdering
    }

    @Test
    fun `test avstemming`() {
        val opprettetTid = LocalDate.of(2025, 1, 9)

        val fagsak = fagsakRepository.insert(no.nav.familie.tilbake.data.Testdata.fagsak())
        val behandling = gammelBehandlingRepository.insert(no.nav.familie.tilbake.data.Testdata.lagBehandling(fagsakId = fagsak.id))
        val xml = readXml("/tilbakekrevingsvedtak/tilbakekrevingsvedtak.xml")
        val økonomiXmlSendt = ØkonomiXmlSendt(
            behandlingId = behandling.id,
            melding = xml,
            kvittering = objectMapper.writeValueAsString(AvstemmingMediatorTest.lagMmmelDto("00", "OK")),
            sporbar =
                Sporbar(
                    opprettetAv = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
                    opprettetTid = opprettetTid.atStartOfDay(),
                ),
        )

        val iverksattVedtak1 = IverksattVedtak(
            id = UUID.fromString("21111111-2222-3333-4444-55555555555a"),
            behandlingId = UUID.fromString("21111111-2222-3333-4444-55555555555b"),
            nyModell = true,
            vedtakId = BigInteger("1234"),
            aktør = AktørEntity(aktørType = AktørType.Person, "11223344556"),
            ytelsestypeKode = Ytelsestype.TILLEGGSSTØNAD.kode,
            kvittering = "04",
            perioder = lagTilbakekrevingsvedtakDto(3000.00.kroner, 3.kroner, 100.kroner),
            vedtaksdato = opprettetTid,
            behandlingstype = Behandlingstype.TILBAKEKREVING,
        )

        val iverksattVedtak2 = IverksattVedtak(
            id = UUID.fromString("21111111-2222-3333-4444-5555555555aa"),
            behandlingId = UUID.fromString("21111111-2222-3333-4444-5555555555bb"),
            nyModell = true,
            vedtakId = BigInteger("1235"),
            aktør = AktørEntity(aktørType = AktørType.Person, "11223344557"),
            ytelsestypeKode = Ytelsestype.TILLEGGSSTØNAD.kode,
            kvittering = "00",
            perioder = lagTilbakekrevingsvedtakDto(0.kroner, 0.kroner, 0.kroner),
            vedtaksdato = opprettetTid,
            behandlingstype = Behandlingstype.REVURDERING_TILBAKEKREVING,
        )

        økonomiXmlSendtRepository.insert(økonomiXmlSendt)
        iverksettRepository.lagreIverksattVedtak(iverksattVedtak = iverksattVedtak1)
        iverksettRepository.lagreIverksattVedtak(iverksattVedtak = iverksattVedtak2)

        val result = avstemmingMediator.avstem(dato = opprettetTid)

        val expected = """
            avsender;vedtakId;fnr;vedtaksdato;fagsakYtelseType;tilbakekrevesBruttoUtenRenter;skatt;tilbakekrevesNettoUtenRenter;renter;erOmgjøringTilIngenTilbakekreving
            familie-tilbake;1234;11223344556;20250109;TSO;6000;6;5994;200;
            familie-tilbake;1235;11223344557;20250109;TSO;0;0;0;0;Omgjoring0
            familie-tilbake;0;32132132111;20250109;BA;2108;0;2108;0;
        """.trimIndent().trimEnd()

        result?.toString(Charsets.UTF_8) shouldBe expected
    }

    @Test
    fun `korrigerKravgrunnlag skal hente korrigert kravgrunnlag når behandling allerede har et kravgrunnlag`() {
        val fagsak = fagsakRepository.insert(GammelTestdata.fagsak())
        val behandling = gammelBehandlingRepository.insert(GammelTestdata.lagBehandling(fagsak.id))
        kravgrunnlagRepository.insert(GammelTestdata.lagKravgrunnlag(behandling.id))
        behandlingsstegstilstandRepository
            .insert(
                Behandlingsstegstilstand(
                    behandlingId = behandling.id,
                    behandlingssteg = Behandlingssteg.GRUNNLAG,
                    behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                    tidsfrist = LocalDate.now().plusWeeks(3),
                    venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                ),
            )

        forvaltningService.korrigerKravgrunnlag(
            behandling.id,
            GammelTestdata.lagKravgrunnlag(behandling.id).eksternKravgrunnlagId,
        )

        val kravgrunnlagene = kravgrunnlagRepository.findByBehandlingId(behandling.id)
        kravgrunnlagene.size shouldBe 2
        kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandling.id).shouldBeTrue()

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
    }

    @Test
    fun `tvingHenleggBehandling skal henlegge behandling når behandling ikke er avsluttet`() {
        val fagsak = fagsakRepository.insert(GammelTestdata.fagsak())
        val behandling = gammelBehandlingRepository.insert(GammelTestdata.lagBehandling(fagsak.id))
        kravgrunnlagRepository.insert(GammelTestdata.lagKravgrunnlag(behandling.id))

        behandlingsstegstilstandRepository.insert(
            Behandlingsstegstilstand(
                behandlingId = behandling.id,
                behandlingssteg = Behandlingssteg.GRUNNLAG,
                behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                tidsfrist = LocalDate.now().plusWeeks(3),
                venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
            ),
        )
        forvaltningService.korrigerKravgrunnlag(behandling.id, GammelTestdata.lagKravgrunnlag(behandling.id).eksternKravgrunnlagId)

        var behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)

        forvaltningService.tvingHenleggBehandling(behandling.id)

        behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.AVBRUTT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.AVBRUTT)

        val oppdatertBehandling = gammelBehandlingRepository.findByIdOrThrow(behandling.id)
        oppdatertBehandling.erAvsluttet.shouldBeTrue()
        oppdatertBehandling.ansvarligSaksbehandler shouldBe ContextService.hentSaksbehandler(SecureLog.Context.tom())
        oppdatertBehandling.avsluttetDato shouldBe LocalDate.now()
        oppdatertBehandling.sisteResultat!!.type shouldBe Behandlingsresultatstype.HENLAGT_TEKNISK_VEDLIKEHOLD

        val tasker = taskService.findAll()
        historikkService.hentHistorikkinnslag(behandling.id).forOne {
            it.type shouldBe TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT.type
            it.tittel shouldBe TilbakekrevingHistorikkinnslagstype.BEHANDLING_HENLAGT.tittel
            it.tekst shouldBe "Årsak: Teknisk vedlikehold"
            it.aktør shouldBe Historikkinnslag.Aktør.SAKSBEHANDLER
            it.opprettetAv shouldBe oppdatertBehandling.ansvarligSaksbehandler
        }
        tasker.forAny {
            it.type shouldBe SendSakshendelseTilDvhTask.TASK_TYPE
            it.payload shouldBe behandling.id.toString()
        }
        tasker.forOne {
            it.type shouldBe FerdigstillOppgaveTask.TYPE
            it.payload shouldBe behandling.id.toString()
        }
    }

    @Test
    fun `korrigerKravgrunnlag skal hente korrigert kravgrunnlag når behandling ikke har et kravgrunnlag`() {
        val fagsak = fagsakRepository.insert(GammelTestdata.fagsak())
        val behandling = gammelBehandlingRepository.insert(GammelTestdata.lagBehandling(fagsak.id))
        behandlingsstegstilstandRepository.insert(
            Behandlingsstegstilstand(
                behandlingId = behandling.id,
                behandlingssteg = Behandlingssteg.GRUNNLAG,
                behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                tidsfrist = LocalDate.now().plusWeeks(3),
                venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
            ),
        )
        lagMottattXml(økonomiXmlMottattRepository)
        forvaltningService.korrigerKravgrunnlag(behandling.id, BigInteger.ZERO)

        val kravgrunnlagene = kravgrunnlagRepository.findByBehandlingId(behandling.id)
        kravgrunnlagene.size shouldBe 1
        kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(behandling.id).shouldBeTrue()

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.GRUNNLAG, Behandlingsstegstatus.UTFØRT)
        assertBehandlingssteg(behandlingsstegstilstand, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)
    }

    @Test
    fun `doTask skal hente kravgrunnlag for revurderingstilbakekreving`() {
        val fagsak = fagsakRepository.insert(GammelTestdata.fagsak())
        val behandling = gammelBehandlingRepository.insert(GammelTestdata.lagBehandling(fagsak.id))
        val hentKravgrunnlagTask = HentKravgrunnlagTask(gammelBehandlingRepository, hentKravgrunnlagService, stegService, logService)
        val revurdering = gammelBehandlingRepository.insert(GammelTestdata.lagRevurdering(behandling.id, fagsak.id))
        kravgrunnlagRepository.insert(GammelTestdata.lagKravgrunnlag(behandling.id))
        behandlingsstegstilstandRepository
            .insert(
                Behandlingsstegstilstand(
                    behandlingId = revurdering.id,
                    behandlingssteg = Behandlingssteg.GRUNNLAG,
                    behandlingsstegsstatus = Behandlingsstegstatus.VENTER,
                    venteårsak = Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
                    tidsfrist = LocalDate.now().plusWeeks(3),
                ),
            )

        hentKravgrunnlagTask.doTask(HentKravgrunnlagTaskTest.lagTask(revurdering.id))
        kravgrunnlagRepository.existsByBehandlingIdAndAktivTrue(revurdering.id).shouldBeTrue()

        historikkinnslagRepository.findByBehandlingId(revurdering.id).forOne {
            it.type shouldBe Historikkinnslagstype.HENDELSE
            it.tittel shouldBe TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_HENT.tittel
            it.tekst shouldBe TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_HENT.tekst
            it.aktør shouldBe Aktør.Vedtaksløsning.type
            it.opprettetAv shouldBe Constants.BRUKER_ID_VEDTAKSLØSNINGEN
        }

        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(revurdering.id)
        behandlingsstegstilstand
            .any {
                Behandlingssteg.GRUNNLAG == it.behandlingssteg &&
                    Behandlingsstegstatus.UTFØRT == it.behandlingsstegsstatus
            }.shouldBeTrue()

        behandlingsstegstilstand
            .any {
                Behandlingssteg.FAKTA == it.behandlingssteg &&
                    Behandlingsstegstatus.KLAR == it.behandlingsstegsstatus
            }.shouldBeTrue()
    }

    @Test
    fun `annulerKravgrunnlag skal annulere kravgrunnlag som er koblet med en behandling`() {
        val fagsak = fagsakRepository.insert(GammelTestdata.fagsak())
        val behandling = gammelBehandlingRepository.insert(GammelTestdata.lagBehandling(fagsak.id))
        val kravgrunnlag = kravgrunnlagRepository.insert(GammelTestdata.lagKravgrunnlag(behandling.id))
        shouldNotThrowAny { forvaltningService.annulerKravgrunnlag(kravgrunnlag.eksternKravgrunnlagId) }
    }

    @Test
    fun `annulerKravgrunnlag skal annulere kravgrunnlag som er mottatt i økonomiXmlMottatt`() {
        val økonomiXmlMottatt = økonomiXmlMottattRepository.insert(GammelTestdata.getøkonomiXmlMottatt())
        shouldNotThrowAny { forvaltningService.annulerKravgrunnlag(økonomiXmlMottatt.eksternKravgrunnlagId!!) }
    }

    companion object {
        const val QUEUE_NAME = "LOCAL_TILLEGGSSTONADER.KRAVGRUNNLAG"
    }
}
