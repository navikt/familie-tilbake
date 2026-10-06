package no.nav.familie.tilbake.api

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forSingle
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.common.exceptionhandler.ApiExceptionHandler
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingRepository
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.sikkerhet.AuditLoggerEvent
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.TilgangskontrollService
import no.nav.familie.tilbake.sikkerhet.ValideringContext
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.brev.Vedtaksbrev
import no.nav.tilbakekreving.e2e.ContextServiceHelpers.somSaksbehandler
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.config.TilgangskontrollServiceMock
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.hendelse.BrukerinfoHendelse
import no.nav.tilbakekreving.hendelse.FagsysteminfoHendelse
import no.nav.tilbakekreving.kontrakter.bruker.Kjønn
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagMapper
import no.nav.tilbakekreving.repository.TilbakekrevingRepository
import no.nav.tilbakekreving.saksbehandler.Behandler
import no.nav.tilbakekreving.systemContext
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.vedtak.IverksettRepository
import no.nav.tilbakekreving.vedtak.iverksattVedtakForDokumentTest
import no.nav.tilbakekreving.vedtak.nyttDokumentVedtakId
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.AopTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.util.UUID

@ActiveProfiles("ny-modell")
@Transactional
class VedtakDokumentServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var iverksettRepository: IverksettRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var brevsporingRepository: BrevsporingRepository

    @Autowired
    private lateinit var tilbakekrevingRepository: TilbakekrevingRepository

    private lateinit var tilgang: DokumentTilgangStub
    private lateinit var service: VedtakDokumentService

    @BeforeEach
    fun opprettService() {
        tilgang = DokumentTilgangStub()
        val tilbakekrevingService = TilbakekrevingService(
            pdlClient = bean(),
            iverksettService = bean(),
            tilbakekrevingRepository = tilbakekrevingRepository,
            bigQueryService = bean(),
            endringObservatørService = bean(),
            kafkaProducer = bean(),
            statusmeldingBufferRepository = bean(),
            dokdistService = bean(),
            featureService = bean(),
            forhåndsvarselService = bean(),
            vedtaksbrevService = bean(),
            tilgangskontrollService = tilgang,
        )
        service = VedtakDokumentService(
            iverksettRepository = iverksettRepository,
            behandlingRepository = behandlingRepository,
            fagsakRepository = fagsakRepository,
            brevsporingRepository = brevsporingRepository,
            tilgangskontrollService = tilgang,
            tilbakekrevingService = tilbakekrevingService,
        )
    }

    @Test
    fun `GET med vedtakId som queryparameter`() {
        val vedtakId = nyttDokumentVedtakId()
        val fagsak = opprettGammelBehandling(vedtakId, Testdata.STANDARD_BRUKERIDENT)
        val behandling = behandlingRepository.findByFagsakId(fagsak.id).single()
        val brev = brevsporingRepository.insert(
            Testdata.lagBrevsporing(behandling.id).copy(
                journalpostId = "journalpost",
                dokumentId = "dokument",
            ),
        )
        val mvc = MockMvcBuilders.standaloneSetup(VedtakDokumentController(service)).build()

        val response = mvc.perform(
            get("/api/vedtak/dokumenter/v1")
                .param("vedtakId", vedtakId.toString())
                .accept(MediaType.APPLICATION_JSON),
        ).andReturn().response

        response.status shouldBe HttpStatus.OK.value()
        val body = objectMapper.readValue<Ressurs<List<Map<String, String>>>>(response.contentAsString)
        body.status shouldBe Ressurs.Status.SUKSESS
        body.data.shouldNotBeNull().forSingle {
            it.keys shouldBe setOf("journalpostId", "dokumentInfoId")
            it["journalpostId"] shouldBe brev.journalpostId
            it["dokumentInfoId"] shouldBe brev.dokumentId
        }
    }

    @Test
    fun `GET uten vedtakId`() {
        val response = dokumentMvc().perform(
            get("/api/vedtak/dokumenter/v1")
                .accept(MediaType.APPLICATION_JSON),
        ).andReturn().response

        response.status shouldBe HttpStatus.BAD_REQUEST.value()
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "abc", "12a"])
    fun `GET med blank eller ikke-numerisk vedtakId`(vedtakId: String) {
        val response = dokumentMvc().perform(
            get("/api/vedtak/dokumenter/v1")
                .param("vedtakId", vedtakId)
                .accept(MediaType.APPLICATION_JSON),
        ).andReturn().response

        response.status shouldBe HttpStatus.BAD_REQUEST.value()
    }

    @Test
    fun `GET med vedtakId lengre enn 64 tegn`() {
        val response = dokumentMvc().perform(
            get("/api/vedtak/dokumenter/v1")
                .param("vedtakId", "0".repeat(64) + "1")
                .accept(MediaType.APPLICATION_JSON),
        ).andReturn().response

        response.status shouldBe HttpStatus.BAD_REQUEST.value()
    }

    @Test
    fun `GET med vedtakId større enn Long MAX_VALUE`() {
        val vedtakId = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE)
        val response = dokumentMvc().perform(
            get("/api/vedtak/dokumenter/v1")
                .param("vedtakId", vedtakId.toString())
                .accept(MediaType.APPLICATION_JSON),
        ).andReturn().response

        response.status shouldBe HttpStatus.BAD_REQUEST.value()
    }

    @Test
    fun `gammel modell med brev fra flere behandlinger i fagsaken`() {
        val vedtakId = nyttDokumentVedtakId()
        val fagsak = opprettGammelBehandling(vedtakId, Testdata.STANDARD_BRUKERIDENT)
        val førsteBehandling = behandlingRepository.findByFagsakId(fagsak.id).single()
        val andreBehandling = behandlingRepository.insert(Testdata.lagBehandling(fagsak.id))
        val førsteBrev = brevsporingRepository.insert(
            Testdata.lagBrevsporing(førsteBehandling.id).copy(journalpostId = "første-journalpost", dokumentId = "første-dokument"),
        )
        val andreBrev = brevsporingRepository.insert(
            Testdata.lagBrevsporing(andreBehandling.id).copy(journalpostId = "andre-journalpost", dokumentId = "andre-dokument"),
        )
        val annenFagsak = opprettGammelBehandling(nyttDokumentVedtakId(), Testdata.STANDARD_BRUKERIDENT)
        brevsporingRepository.insert(Testdata.lagBrevsporing(behandlingRepository.findByFagsakId(annenFagsak.id).single().id))

        val dokumenter = service.hentDokumentreferanser(vedtakId)

        dokumenter.forSingle {
            it shouldBe VedtakDokumentreferanseDto(førsteBrev.journalpostId, førsteBrev.dokumentId)
        }
        dokumenter shouldNotContain VedtakDokumentreferanseDto(andreBrev.journalpostId, andreBrev.dokumentId)
        tilgang.gamleTilganger.forSingle {
            it.fagsystem shouldBe fagsak.fagsystem.tilDTO()
            it.eksternFagsakId shouldBe fagsak.eksternFagsakId
            it.minimumBehandlerrolle shouldBe Behandlerrolle.VEILEDER
            it.auditLoggerEvent shouldBe AuditLoggerEvent.ACCESS
            it.handling shouldBe "Henter dokumentreferanser for iverksatt vedtak"
        }
        tilgang.nyeTilganger shouldBe emptyList()
    }

    @Test
    fun `gammel modell uten brev`() {
        val vedtakId = nyttDokumentVedtakId()
        val fagsak = opprettGammelBehandling(vedtakId, Testdata.STANDARD_BRUKERIDENT)

        service.hentDokumentreferanser(vedtakId) shouldBe emptyList()
        tilgang.gamleTilganger.size shouldBe 1
    }

    @Test
    fun `gammel modell med avvist tilgang`() {
        val vedtakId = nyttDokumentVedtakId()
        val fagsak = opprettGammelBehandling(vedtakId, Testdata.STANDARD_BRUKERIDENT)
        brevsporingRepository.insert(Testdata.lagBrevsporing(behandlingRepository.findByFagsakId(fagsak.id).single().id))
        val tilgangsfeil = avvisTilgang()

        shouldThrow<Feil> {
            service.hentDokumentreferanser(vedtakId)
        } shouldBe tilgangsfeil
    }

    @Test
    fun `ny modell med sendte dokumenter i brevhistorikken`() {
        val vedtakId = nyttDokumentVedtakId()
        val tilbakekreving = opprettNyBehandling(vedtakId, Testdata.STANDARD_BRUKERIDENT)

        val dokumenter = somSaksbehandler {
            service.hentDokumentreferanser(vedtakId)
        }

        dokumenter.size shouldBe 2
        dokumenter.toSet() shouldBe setOf(
            VedtakDokumentreferanseDto("journalpost-1", "dokument-1"),
            VedtakDokumentreferanseDto("journalpost-2", "dokument-2"),
        )
        tilgang.nyeTilganger.forSingle {
            it.first shouldBe tilbakekreving.id
            it.second shouldBe ValideringContext.ListJournalposter
        }
        tilgang.gamleTilganger shouldBe emptyList()
    }

    @Test
    fun `ny modell med avvist tilgang`() {
        val vedtakId = nyttDokumentVedtakId()
        opprettNyBehandling(vedtakId, Testdata.STANDARD_BRUKERIDENT)
        val tilgangsfeil = avvisTilgang()

        shouldThrow<Feil> {
            somSaksbehandler {
                service.hentDokumentreferanser(vedtakId)
            }
        } shouldBe tilgangsfeil
        tilgang.nyeTilganger.forSingle {
            it.second shouldBe ValideringContext.ListJournalposter
        }
    }

    @Test
    fun `vedtakId uten iverksatt vedtak`() {
        service.hentDokumentreferanser(nyttDokumentVedtakId()) shouldBe emptyList()
        tilgang.gamleTilganger shouldBe emptyList()
        tilgang.nyeTilganger shouldBe emptyList()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `iverksatt vedtak uten tilhørende behandling`(nyModell: Boolean) {
        val vedtakId = nyttDokumentVedtakId()
        iverksettRepository.lagreIverksattVedtak(
            iverksattVedtakForDokumentTest(UUID.randomUUID(), vedtakId, nyModell, Testdata.STANDARD_BRUKERIDENT),
        )

        service.hentDokumentreferanser(vedtakId) shouldBe emptyList()
        tilgang.gamleTilganger shouldBe emptyList()
        tilgang.nyeTilganger shouldBe emptyList()
    }

    @Test
    fun `samme vedtakId i begge modeller med forskjellig skyldner`() {
        val vedtakId = nyttDokumentVedtakId()
        opprettGammelBehandling(vedtakId, Testdata.STANDARD_BRUKERIDENT)
        opprettNyBehandling(vedtakId, no.nav.tilbakekreving.Testdata.TESTBRUKER)

        val feil = shouldThrow<Feil> {
            service.hentDokumentreferanser(vedtakId)
        }

        feil.httpStatus shouldBe HttpStatus.CONFLICT
        tilgang.gamleTilganger shouldBe emptyList()
        tilgang.nyeTilganger shouldBe emptyList()
    }

    @Test
    fun `samme vedtakId i begge modeller`() {
        val vedtakId = nyttDokumentVedtakId()
        opprettGammelBehandling(vedtakId, Testdata.STANDARD_BRUKERIDENT)
        opprettNyBehandling(vedtakId, Testdata.STANDARD_BRUKERIDENT)

        val feil = shouldThrow<Feil> {
            service.hentDokumentreferanser(vedtakId)
        }

        feil.httpStatus shouldBe HttpStatus.CONFLICT
        tilgang.gamleTilganger shouldBe emptyList()
        tilgang.nyeTilganger shouldBe emptyList()
    }

    private fun dokumentMvc(): MockMvc =
        MockMvcBuilders.standaloneSetup(VedtakDokumentController(service))
            .setControllerAdvice(ApiExceptionHandler())
            .build()

    private fun opprettGammelBehandling(vedtakId: BigInteger, ident: String): Fagsak {
        val fagsak = fagsakRepository.insert(Testdata.fagsak(brukerident = ident))
        val behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsak.id))
        iverksettRepository.lagreIverksattVedtak(
            iverksattVedtakForDokumentTest(behandling.id, vedtakId, false, no.nav.tilbakekreving.Testdata.TESTBRUKER),
        )
        return fagsak
    }

    private fun opprettNyBehandling(vedtakId: BigInteger, ident: String): Tilbakekreving {
        val context = systemContext()
        val kravgrunnlag = KravgrunnlagUtil.unmarshalKravgrunnlag(
            KravgrunnlagGenerator.forTilleggsstønader(
                vedtakId = vedtakId.toString(),
                fødselsnummer = ident,
                perioder = listOf(KravgrunnlagGenerator.standardPeriode(1.januar(2025) til 31.januar(2025))),
            ),
        )
        val tilbakekreving = Tilbakekreving.opprett(
            id = tilbakekrevingRepository.nesteId(),
            opprettTilbakekrevingEvent = KravgrunnlagMapper.tilOpprettTilbakekrevingHendelse(kravgrunnlag),
            sideeffektContext = context,
        )
        tilbakekreving.håndter(
            KravgrunnlagMapper.tilKravgrunnlagHendelse(kravgrunnlag, emptyMap(), context.klokke),
            context,
        )
        val svar = no.nav.tilbakekreving.Testdata.fagsysteminfoSvar(kravgrunnlag.fagsystemId)
        tilbakekreving.håndter(
            FagsysteminfoHendelse(
                aktør = null,
                revurdering = FagsysteminfoHendelse.Revurdering(
                    behandlingId = svar.revurdering.behandlingId,
                    årsak = EksternFagsakRevurdering.Revurderingsårsak.NYE_OPPLYSNINGER,
                    årsakTilFeilutbetaling = svar.revurdering.årsakTilFeilutbetaling,
                    vedtaksdato = svar.revurdering.vedtaksdato,
                    url = svar.revurdering.url,
                ),
                utvidPerioder = null,
                behandlendeEnhet = svar.behandlendeEnhet,
            ),
            context,
        )
        tilbakekreving.håndter(
            BrukerinfoHendelse(
                ident = ident,
                navn = "Testbruker",
                fødselsdato = 1.januar(1990),
                kjønn = Kjønn.UKJENT,
                dødsdato = null,
                språkkode = Språkkode.NB,
            ),
            context,
        )
        listOf("1", "2").forEach { nummer ->
            tilbakekreving.brevHistorikk.lagre(
                Vedtaksbrev.opprett(SystemKlokke).apply { brevSendt("journalpost-$nummer", "dokument-$nummer") },
            )
        }
        tilbakekreving.brevHistorikk.lagre(Vedtaksbrev.opprett(SystemKlokke))
        // Bypass REQUIRES_NEW only for fixture creation so these writes join the test transaction.
        AopTestUtils.getUltimateTargetObject<TilbakekrevingRepository>(tilbakekrevingRepository)
            .opprett(tilbakekreving.tilEntity(), context.behandlingslogg)
        iverksettRepository.lagreIverksattVedtak(
            iverksattVedtakForDokumentTest(
                tilbakekreving.hentBehandlingsinformasjon().behandlingId,
                vedtakId,
                true,
                no.nav.tilbakekreving.Testdata.TESTBRUKER,
            ),
        )
        return tilbakekreving
    }

    private fun avvisTilgang(): Feil = Feil(
        message = "Ingen tilgang til dokumenter",
        httpStatus = HttpStatus.FORBIDDEN,
        logContext = SecureLog.Context.tom(),
    ).also { tilgang.tilgangsfeil = it }

    private inline fun <reified T : Any> bean(): T = applicationContext.getBean(T::class.java)

    private class DokumentTilgangStub : TilgangskontrollService by TilgangskontrollServiceMock() {
        val gamleTilganger = mutableListOf<GammelTilgang>()
        val nyeTilganger = mutableListOf<Pair<String, ValideringContext>>()
        var tilgangsfeil: Feil? = null

        override fun validerTilgangFagsystemOgFagsakId(
            fagsystem: FagsystemDTO,
            eksternFagsakId: String,
            minimumBehandlerrolle: Behandlerrolle,
            auditLoggerEvent: AuditLoggerEvent,
            handling: String,
        ) {
            gamleTilganger.add(GammelTilgang(fagsystem, eksternFagsakId, minimumBehandlerrolle, auditLoggerEvent, handling))
            tilgangsfeil?.let { throw it }
        }

        override fun validerTilgangTilbakekreving(
            tilbakekreving: Tilbakekreving,
            valideringContext: ValideringContext,
            behandler: Behandler,
        ): Behandlerrolle {
            nyeTilganger.add(tilbakekreving.id to valideringContext)
            tilgangsfeil?.let { throw it }
            return Behandlerrolle.VEILEDER
        }
    }

    private data class GammelTilgang(
        val fagsystem: FagsystemDTO,
        val eksternFagsakId: String,
        val minimumBehandlerrolle: Behandlerrolle,
        val auditLoggerEvent: AuditLoggerEvent,
        val handling: String,
    )
}
