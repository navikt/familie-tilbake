package no.nav.tilbakekreving.api

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAll
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
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
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
import no.nav.tilbakekreving.vedtak.VedtakDokumentRepository
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.AopTestUtils
import org.springframework.test.util.ReflectionTestUtils
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
    private lateinit var vedtakDokumentRepository: VedtakDokumentRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

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
        val tilbakekrevingService =
            applicationContext.autowireCapableBeanFactory.createBean(TilbakekrevingService::class.java)
        ReflectionTestUtils.setField(
            AopTestUtils.getUltimateTargetObject<TilbakekrevingService>(tilbakekrevingService),
            "tilgangskontrollService",
            tilgang,
        )
        service = VedtakDokumentService(
            vedtakDokumentRepository = vedtakDokumentRepository,
            behandlingRepository = behandlingRepository,
            fagsakRepository = fagsakRepository,
            brevsporingRepository = brevsporingRepository,
            tilgangskontrollService = tilgang,
            tilbakekrevingService = tilbakekrevingService,
        )
    }

    @Test
    fun `GET med vedtakId i path`() {
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
            get("/api/dokumenter/vedtak/{vedtakId}/v1", vedtakId)
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
    fun `GET uten vedtakId path-segment`() {
        val mvc = MockMvcBuilders.standaloneSetup(VedtakDokumentController(service)).build()
        val response = mvc.perform(
            get("/api/dokumenter/vedtak/v1")
                .accept(MediaType.APPLICATION_JSON),
        ).andReturn().response

        response.status shouldBe HttpStatus.NOT_FOUND.value()
        response.contentAsString shouldBe ""
    }

    @ParameterizedTest
    @ValueSource(strings = [" ", "abc", "12a", "１２"])
    fun `GET med blank eller ikke-numerisk vedtakId`(vedtakId: String) {
        val response = dokumentMvc().perform(
            get("/api/dokumenter/vedtak/{vedtakId}/v1", vedtakId)
                .accept(MediaType.APPLICATION_JSON),
        ).andReturn().response

        response.status shouldBe HttpStatus.BAD_REQUEST.value()
    }

    @Test
    fun `GET med vedtakId lengre enn 64 tegn`() {
        val response = dokumentMvc().perform(
            get("/api/dokumenter/vedtak/{vedtakId}/v1", "0".repeat(64) + "1")
                .accept(MediaType.APPLICATION_JSON),
        ).andReturn().response

        response.status shouldBe HttpStatus.BAD_REQUEST.value()
    }

    @Test
    fun `GET med vedtakId større enn Long MAX_VALUE`() {
        val vedtakId = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE)
        val response = dokumentMvc().perform(
            get("/api/dokumenter/vedtak/{vedtakId}/v1", vedtakId.toString())
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

        val dokumenter = hentDokumentreferanser(vedtakId)

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

        hentDokumentreferanser(vedtakId) shouldBe emptyList()
        tilgang.gamleTilganger.size shouldBe 1
    }

    @Test
    fun `gammel modell med flere kildeoppføringer og duplikater på samme fagsak`() {
        val vedtakId = nyttDokumentVedtakId()
        val fagsak = opprettGammelBehandling(vedtakId, Testdata.STANDARD_BRUKERIDENT)
        val førsteBehandling = behandlingRepository.findByFagsakId(fagsak.id).single()
        val andreBehandling = behandlingRepository.insert(Testdata.lagBehandling(fagsak.id))
        val behandlingUtenIverksattVedtak = behandlingRepository.insert(Testdata.lagBehandling(fagsak.id))
        kravgrunnlagRepository.insert(
            Testdata.lagKravgrunnlag(andreBehandling.id).copy(vedtakId = vedtakId),
        )
        kravgrunnlagRepository.insert(
            Testdata.lagKravgrunnlag(førsteBehandling.id).copy(vedtakId = vedtakId, aktiv = false),
        )
        listOf(førsteBehandling, andreBehandling).forEach { behandling ->
            brevsporingRepository.insert(
                Testdata.lagBrevsporing(behandling.id).copy(journalpostId = "felles-journalpost", dokumentId = "felles-dokument"),
            )
        }
        brevsporingRepository.insert(
            Testdata.lagBrevsporing(førsteBehandling.id).copy(journalpostId = "første-journalpost", dokumentId = "første-dokument"),
        )
        brevsporingRepository.insert(
            Testdata.lagBrevsporing(andreBehandling.id).copy(journalpostId = "andre-journalpost", dokumentId = "andre-dokument"),
        )
        brevsporingRepository.insert(Testdata.lagBrevsporing(behandlingUtenIverksattVedtak.id))

        val dokumenter = hentDokumentreferanser(vedtakId)

        dokumenter.size shouldBe 3
        dokumenter.toSet() shouldBe setOf(
            VedtakDokumentreferanseDto("felles-journalpost", "felles-dokument"),
            VedtakDokumentreferanseDto("første-journalpost", "første-dokument"),
            VedtakDokumentreferanseDto("andre-journalpost", "andre-dokument"),
        )
        tilgang.gamleTilganger.size shouldBe 2
        tilgang.gamleTilganger.forAll {
            it.fagsystem shouldBe fagsak.fagsystem.tilDTO()
            it.eksternFagsakId shouldBe fagsak.eksternFagsakId
            it.minimumBehandlerrolle shouldBe Behandlerrolle.VEILEDER
            it.auditLoggerEvent shouldBe AuditLoggerEvent.ACCESS
            it.handling shouldBe "Henter dokumentreferanser for iverksatt vedtak"
        }
        tilgang.nyeTilganger shouldBe emptyList()
    }

    @Test
    fun `gammel modell med avvist tilgang`() {
        val vedtakId = nyttDokumentVedtakId()
        val fagsak = opprettGammelBehandling(vedtakId, Testdata.STANDARD_BRUKERIDENT)
        brevsporingRepository.insert(Testdata.lagBrevsporing(behandlingRepository.findByFagsakId(fagsak.id).single().id))
        val tilgangsfeil = avvisTilgang()

        shouldThrow<Feil> {
            hentDokumentreferanser(vedtakId)
        } shouldBe tilgangsfeil
    }

    @Test
    fun `ny modell med sendte dokumenter i brevhistorikken`() {
        val vedtakId = nyttDokumentVedtakId()
        val tilbakekreving = opprettNyBehandling(vedtakId, Testdata.STANDARD_BRUKERIDENT)

        val dokumenter = somSaksbehandler {
            hentDokumentreferanser(vedtakId)
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
                hentDokumentreferanser(vedtakId)
            }
        } shouldBe tilgangsfeil
        tilgang.nyeTilganger.forSingle {
            it.second shouldBe ValideringContext.ListJournalposter
        }
    }

    @Test
    fun `ny modell med flere kildeoppføringer og duplikater`() {
        val vedtakId = nyttDokumentVedtakId()
        val førsteTilbakekreving = opprettNyBehandling(vedtakId, Testdata.STANDARD_BRUKERIDENT, listOf("1", "2", "2"))
        val andreTilbakekreving = opprettNyBehandling(vedtakId, Testdata.STANDARD_BRUKERIDENT, listOf("2", "3"))
        dupliserKravgrunnlag(førsteTilbakekreving.id, vedtakId)

        val dokumenter = somSaksbehandler {
            hentDokumentreferanser(vedtakId)
        }

        dokumenter.size shouldBe 3
        dokumenter.toSet() shouldBe setOf(
            VedtakDokumentreferanseDto("journalpost-1", "dokument-1"),
            VedtakDokumentreferanseDto("journalpost-2", "dokument-2"),
            VedtakDokumentreferanseDto("journalpost-3", "dokument-3"),
        )
        tilgang.nyeTilganger.size shouldBe 2
        tilgang.nyeTilganger.toSet() shouldBe setOf(
            førsteTilbakekreving.id to ValideringContext.ListJournalposter,
            andreTilbakekreving.id to ValideringContext.ListJournalposter,
        )
        tilgang.gamleTilganger shouldBe emptyList()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `flere kildeoppføringer med avvist tilgang etter en tillatt behandling`(nyModell: Boolean) {
        val vedtakId = nyttDokumentVedtakId()
        if (nyModell) {
            opprettNyBehandling(vedtakId, Testdata.STANDARD_BRUKERIDENT, listOf("1"))
            opprettNyBehandling(vedtakId, Testdata.STANDARD_BRUKERIDENT, listOf("2"))
        } else {
            val fagsak = opprettGammelBehandling(vedtakId, Testdata.STANDARD_BRUKERIDENT)
            val førsteBehandling = behandlingRepository.findByFagsakId(fagsak.id).single()
            val andreBehandling = behandlingRepository.insert(Testdata.lagBehandling(fagsak.id))
            kravgrunnlagRepository.insert(
                Testdata.lagKravgrunnlag(andreBehandling.id).copy(vedtakId = vedtakId),
            )
            listOf(førsteBehandling, andreBehandling).forEach { behandling ->
                brevsporingRepository.insert(Testdata.lagBrevsporing(behandling.id))
            }
        }
        val tilgangsfeil = avvisTilgang(antallTillatteKontroller = 1)

        shouldThrow<Feil> {
            somSaksbehandler {
                hentDokumentreferanser(vedtakId)
            }
        } shouldBe tilgangsfeil

        if (nyModell) {
            tilgang.nyeTilganger.size shouldBe 2
            tilgang.nyeTilganger.forAll {
                it.second shouldBe ValideringContext.ListJournalposter
            }
            tilgang.gamleTilganger shouldBe emptyList()
        } else {
            tilgang.gamleTilganger.size shouldBe 2
            tilgang.nyeTilganger shouldBe emptyList()
        }
    }

    @Test
    fun `ukjent vedtakId`() {
        hentDokumentreferanser(nyttDokumentVedtakId()) shouldBe emptyList()
        tilgang.gamleTilganger shouldBe emptyList()
        tilgang.nyeTilganger shouldBe emptyList()
    }

    @Test
    fun `vedtakId uten samsvarende kravgrunnlag`() {
        val annetVedtakId = nyttDokumentVedtakId()
        opprettGammelBehandling(annetVedtakId, Testdata.STANDARD_BRUKERIDENT)
        opprettNyBehandling(annetVedtakId, Testdata.STANDARD_BRUKERIDENT)

        hentDokumentreferanser(nyttDokumentVedtakId()) shouldBe emptyList()
        tilgang.gamleTilganger shouldBe emptyList()
        tilgang.nyeTilganger shouldBe emptyList()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `iverksatt vedtak uten kravgrunnlag gir ingen dokumentreferanser`(nyModell: Boolean) {
        val vedtakId = nyttDokumentVedtakId()
        iverksettRepository.lagreIverksattVedtak(
            iverksattVedtakForDokumentTest(UUID.randomUUID(), vedtakId, nyModell, Testdata.STANDARD_BRUKERIDENT),
        )

        hentDokumentreferanser(vedtakId) shouldBe emptyList()
        tilgang.gamleTilganger shouldBe emptyList()
        tilgang.nyeTilganger shouldBe emptyList()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `iverksatt vedtak uten samsvarende kravgrunnlag gir ingen dokumentreferanser`(nyModell: Boolean) {
        val kravgrunnlagVedtakId = nyttDokumentVedtakId()
        val iverksattVedtakId = nyttDokumentVedtakId()
        if (nyModell) {
            opprettNyBehandling(kravgrunnlagVedtakId, Testdata.STANDARD_BRUKERIDENT)
        } else {
            val fagsak = opprettGammelBehandling(kravgrunnlagVedtakId, Testdata.STANDARD_BRUKERIDENT)
            val behandling = behandlingRepository.findByFagsakId(fagsak.id).single()
            brevsporingRepository.insert(Testdata.lagBrevsporing(behandling.id))
        }
        iverksettRepository.lagreIverksattVedtak(
            iverksattVedtakForDokumentTest(
                UUID.randomUUID(),
                iverksattVedtakId,
                nyModell,
                Testdata.STANDARD_BRUKERIDENT,
            ),
        )

        hentDokumentreferanser(iverksattVedtakId) shouldBe emptyList()
        tilgang.gamleTilganger shouldBe emptyList()
        tilgang.nyeTilganger shouldBe emptyList()
    }

    private fun hentDokumentreferanser(vedtakId: BigInteger): List<VedtakDokumentreferanseDto> =
        requireNotNull(VedtakDokumentController(service).hentDokumentreferanser(vedtakId.toString()).data)

    private fun dupliserKravgrunnlag(tilbakekrevingId: String, vedtakId: BigInteger) {
        jdbcTemplate.update(
            """
            INSERT INTO tilbakekreving_kravgrunnlag (
                id, tilbakekreving_id, vedtak_id, kravstatuskode, fagsystem_vedtaksdato,
                vedtak_gjelder_type, vedtak_gjelder_ident, utbetales_til_type, utbetales_til_ident,
                skal_beregne_renter, ansvarlig_enhet, kontrollfelt, kravgrunnlag_id, referanse, opprettet
            )
            SELECT ?, tilbakekreving_id, vedtak_id, kravstatuskode, fagsystem_vedtaksdato,
                vedtak_gjelder_type, vedtak_gjelder_ident, utbetales_til_type, utbetales_til_ident,
                skal_beregne_renter, ansvarlig_enhet, kontrollfelt, kravgrunnlag_id, referanse, localtimestamp
            FROM tilbakekreving_kravgrunnlag
            WHERE tilbakekreving_id = ? AND vedtak_id = ?
            """.trimIndent(),
            UUID.randomUUID(),
            tilbakekrevingId.toInt(),
            vedtakId.toLong(),
        ) shouldBe 1
    }

    private fun dokumentMvc(): MockMvc =
        MockMvcBuilders.standaloneSetup(VedtakDokumentController(service))
            .setControllerAdvice(ApiExceptionHandler())
            .build()

    private fun opprettGammelBehandling(vedtakId: BigInteger, ident: String): Fagsak {
        val fagsak = fagsakRepository.insert(Testdata.fagsak(brukerident = ident))
        val behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsak.id))
        kravgrunnlagRepository.insert(
            Testdata.lagKravgrunnlag(behandling.id).copy(vedtakId = vedtakId),
        )
        iverksettRepository.lagreIverksattVedtak(
            iverksattVedtakForDokumentTest(behandling.id, vedtakId, false, no.nav.tilbakekreving.Testdata.TESTBRUKER),
        )
        return fagsak
    }

    private fun opprettNyBehandling(
        vedtakId: BigInteger,
        ident: String,
        dokumentnumre: List<String> = listOf("1", "2"),
    ): Tilbakekreving {
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
        dokumentnumre.forEach { nummer ->
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

    private fun avvisTilgang(antallTillatteKontroller: Int = 0): Feil = Feil(
        message = "Ingen tilgang til dokumenter",
        httpStatus = HttpStatus.FORBIDDEN,
        logContext = SecureLog.Context.tom(),
    ).also {
        tilgang.tilgangsfeil = it
        tilgang.antallTillatteKontroller = antallTillatteKontroller
    }

    private class DokumentTilgangStub : TilgangskontrollService by TilgangskontrollServiceMock() {
        val gamleTilganger = mutableListOf<GammelTilgang>()
        val nyeTilganger = mutableListOf<Pair<String, ValideringContext>>()
        var tilgangsfeil: Feil? = null
        var antallTillatteKontroller: Int = 0

        override fun validerTilgangFagsystemOgFagsakId(
            fagsystem: FagsystemDTO,
            eksternFagsakId: String,
            minimumBehandlerrolle: Behandlerrolle,
            auditLoggerEvent: AuditLoggerEvent,
            handling: String,
        ) {
            gamleTilganger.add(GammelTilgang(fagsystem, eksternFagsakId, minimumBehandlerrolle, auditLoggerEvent, handling))
            avvisVedManglendeTilgang()
        }

        override fun validerTilgangTilbakekreving(
            tilbakekreving: Tilbakekreving,
            valideringContext: ValideringContext,
            behandler: Behandler,
        ): Behandlerrolle {
            nyeTilganger.add(tilbakekreving.id to valideringContext)
            avvisVedManglendeTilgang()
            return Behandlerrolle.VEILEDER
        }

        private fun avvisVedManglendeTilgang() {
            if (gamleTilganger.size + nyeTilganger.size > antallTillatteKontroller) {
                tilgangsfeil?.let { throw it }
            }
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
