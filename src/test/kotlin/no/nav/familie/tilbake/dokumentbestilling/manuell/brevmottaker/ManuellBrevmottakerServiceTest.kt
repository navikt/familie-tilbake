package no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.shouldForExactly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.FagsakService
import no.nav.familie.tilbake.behandling.ValiderBrevmottakerService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstilstand
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.kontrakter.organisasjon.Organisasjon
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.api.v1.dto.ManuellBrevmottakerRequestDto
import no.nav.tilbakekreving.applicationProps
import no.nav.tilbakekreving.arbeidsforhold.ArbeidsforholdService
import no.nav.tilbakekreving.config.FeatureService
import no.nav.tilbakekreving.integrasjoner.arbeidsforhold.EregClient
import no.nav.tilbakekreving.integrasjoner.arbeidsforhold.kontrakter.HentOrganisasjonResponse
import no.nav.tilbakekreving.integrasjoner.arbeidsforhold.kontrakter.Navn
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import no.nav.tilbakekreving.kontrakter.brev.ManuellAdresseInfo
import no.nav.tilbakekreving.kontrakter.brev.MottakerType
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.DØDSBO
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class ManuellBrevmottakerServiceTest(
    @Autowired private val historikkService: HistorikkService,
) : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var manuellBrevmottakerRepository: ManuellBrevmottakerRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var behandlingskontrollService: BehandlingskontrollService

    @Autowired
    private lateinit var behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var validerBrevmottakerService: ValiderBrevmottakerService

    @Autowired
    private lateinit var logService: LogService

    @Autowired
    private lateinit var manuellBrevmottakerService: ManuellBrevmottakerService

    private lateinit var behandling: Behandling
    private lateinit var featureService: FeatureService
    private lateinit var arbeidsforholdService: ArbeidsforholdService
    private lateinit var eregClient: EregClient

    private val manuellBrevmottakerRequestDto =
        ManuellBrevmottakerRequestDto(
            type = DØDSBO,
            navn = "John Doe",
            manuellAdresseInfo =
                ManuellAdresseInfo(
                    adresselinje1 = "test adresse1",
                    adresselinje2 = "test adresse2",
                    postnummer = "0000",
                    poststed = "Oslo",
                    landkode = "NO",
                ),
        )

    private val manuellBrevmottakerIUtlandetRequestDto =
        ManuellBrevmottakerRequestDto(
            type = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
            navn = "John Doe",
            manuellAdresseInfo =
                ManuellAdresseInfo(
                    adresselinje1 = "test adresse1",
                    adresselinje2 = "0000 Stockholm",
                    postnummer = "",
                    poststed = "",
                    landkode = "SE",
                ),
        )

    private val mockIntegrasjonerClient: IntegrasjonerClient = mockk()

    @BeforeEach
    fun init() {
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsakId = fagsak.id))
        featureService = FeatureService(applicationProperties = applicationProps())
        eregClient = mockk<EregClient> {
            every { hentOrganisasjon(any()) } returns HentOrganisasjonResponse(Navn(""), null)
        }
        arbeidsforholdService = ArbeidsforholdService(eregClient)

        every { mockIntegrasjonerClient.validerOrganisasjon(any()) } returns true
        every { mockIntegrasjonerClient.hentOrganisasjon("123456789") } returns
            Organisasjon("123456789", navn = "Organisasjon AS")
    }

    @Test
    fun `leggTilBrevmottaker skal legge til brevmottakere og oppdatere med oppdaterBrevmottaker`() {
        shouldNotThrowAny {
            manuellBrevmottakerService.leggTilBrevmottaker(behandling.id, manuellBrevmottakerRequestDto)
        }
        shouldNotThrowAny {
            manuellBrevmottakerService.leggTilBrevmottaker(
                behandling.id,
                manuellBrevmottakerRequestDto.copy(
                    navn = "Kari Nordmann",
                    manuellAdresseInfo = null,
                    personIdent = "12345678910",
                ),
            )
        }

        var manuellBrevmottakere = manuellBrevmottakerService.hentBrevmottakere(behandling.id)

        manuellBrevmottakere.shouldHaveSize(2)
        val dbManuellBrevmottaker = manuellBrevmottakere.filter { it.navn.equals("John Doe") }.first()
        assertEqualsManuellBrevmottaker(dbManuellBrevmottaker, manuellBrevmottakerRequestDto)

        historikkService.hentHistorikkinnslag(behandling.id).shouldForExactly(2) {
            it.tittel shouldBe "Dødsbo er lagt til som brevmottaker"
        }

        val oppdatertManuellBrevmottaker =
            manuellBrevmottakerRequestDto.copy(
                manuellAdresseInfo =
                    ManuellAdresseInfo(
                        adresselinje1 = "ny",
                        postnummer = "1111",
                        poststed = "stavanger",
                        landkode = "NO",
                    ),
            )
        shouldNotThrowAny {
            manuellBrevmottakerService.oppdaterBrevmottaker(
                behandling.id,
                dbManuellBrevmottaker.id,
                oppdatertManuellBrevmottaker,
            )
        }

        manuellBrevmottakere = manuellBrevmottakerService.hentBrevmottakere(behandling.id)
        manuellBrevmottakere.shouldHaveSize(2)
        val dbOppdatertManuellBrevmottaker = manuellBrevmottakere.filter { it.navn.equals("John Doe") }.first()
        assertEqualsManuellBrevmottaker(dbOppdatertManuellBrevmottaker, oppdatertManuellBrevmottaker)
    }

    @Test
    fun `leggTilBrevmottaker skal legge til brevmottakere i utlandet uten postadresse og oppdatere med oppdaterBrevmottaker`() {
        shouldNotThrowAny {
            manuellBrevmottakerService.leggTilBrevmottaker(behandling.id, manuellBrevmottakerIUtlandetRequestDto)
        }

        val manuellBrevmottakere = manuellBrevmottakerService.hentBrevmottakere(behandling.id)

        manuellBrevmottakere.shouldHaveSize(1)
        val dbManuellBrevmottaker = manuellBrevmottakere.first()
        assertEqualsManuellBrevmottaker(dbManuellBrevmottaker, manuellBrevmottakerIUtlandetRequestDto)

        historikkService.hentHistorikkinnslag(behandling.id).shouldForExactly(1) {
            it.tittel shouldBe "Bruker med utenlandsk adresse er lagt til som brevmottaker"
        }

        val oppdatertManuellBrevmottaker =
            manuellBrevmottakerIUtlandetRequestDto.copy(
                manuellAdresseInfo =
                    ManuellAdresseInfo(
                        adresselinje1 = "Ny adresse",
                        adresselinje2 = "0001 Stockholm",
                        postnummer = "",
                        poststed = "",
                        landkode = "SE",
                    ),
            )
        shouldNotThrowAny {
            manuellBrevmottakerService.oppdaterBrevmottaker(
                behandling.id,
                dbManuellBrevmottaker.id,
                oppdatertManuellBrevmottaker,
            )
        }

        val oppdaterteBrevmottakere = manuellBrevmottakerService.hentBrevmottakere(behandling.id)
        oppdaterteBrevmottakere.shouldHaveSize(1)
        val dbOppdatertManuellBrevmottaker = oppdaterteBrevmottakere.first()
        assertEqualsManuellBrevmottaker(dbOppdatertManuellBrevmottaker, oppdatertManuellBrevmottaker)
    }

    @Test
    fun `fjernBrevmottaker fjerner brevmottaker`() {
        shouldNotThrowAny {
            manuellBrevmottakerService.leggTilBrevmottaker(behandling.id, manuellBrevmottakerRequestDto)
        }
        shouldNotThrowAny {
            manuellBrevmottakerService.leggTilBrevmottaker(
                behandling.id,
                manuellBrevmottakerRequestDto.copy(navn = "Kari Nordmann"),
            )
        }

        val manuellBrevmottakere = manuellBrevmottakerService.hentBrevmottakere(behandling.id)

        manuellBrevmottakere.shouldHaveSize(2)
        val dbManuellBrevmottaker = manuellBrevmottakere.filter { it.navn.equals("John Doe") }.first()
        assertEqualsManuellBrevmottaker(dbManuellBrevmottaker, manuellBrevmottakerRequestDto)

        historikkService.hentHistorikkinnslag(behandling.id).shouldForExactly(2) {
            it.tittel shouldBe "Dødsbo er lagt til som brevmottaker"
        }

        shouldNotThrowAny {
            manuellBrevmottakerService.fjernBrevmottaker(behandling.id, dbManuellBrevmottaker.id)
        }

        manuellBrevmottakerService
            .hentBrevmottakere(behandling.id)
            .filter { it.navn.equals("John Doe") }
            .shouldBeEmpty()

        historikkService.hentHistorikkinnslag(behandling.id).shouldForExactly(1) {
            it.tittel shouldBe "Dødsbo er fjernet som brevmottaker"
        }
    }

    @Test
    fun `fjernBrevmottaker fjerner brevmottaker i utlandet`() {
        shouldNotThrowAny {
            manuellBrevmottakerService.leggTilBrevmottaker(behandling.id, manuellBrevmottakerIUtlandetRequestDto)
        }

        val manuellBrevmottakere = manuellBrevmottakerService.hentBrevmottakere(behandling.id)

        manuellBrevmottakere.shouldHaveSize(1)
        val dbManuellBrevmottaker = manuellBrevmottakere.first()
        assertEqualsManuellBrevmottaker(dbManuellBrevmottaker, manuellBrevmottakerIUtlandetRequestDto)

        historikkService.hentHistorikkinnslag(behandling.id).shouldForExactly(1) {
            it.tittel shouldBe "Bruker med utenlandsk adresse er lagt til som brevmottaker"
        }

        shouldNotThrowAny {
            manuellBrevmottakerService.fjernBrevmottaker(behandling.id, dbManuellBrevmottaker.id)
        }

        manuellBrevmottakerService
            .hentBrevmottakere(behandling.id)
            .shouldBeEmpty()

        historikkService.hentHistorikkinnslag(behandling.id).shouldForExactly(1) {
            it.tittel shouldBe "Bruker med utenlandsk adresse er fjernet som brevmottaker"
        }
    }

    @Test
    fun `opprettBrevmottakerSteg skal opprette og autoutføre behandlingssteg BREVMOTTAKER`() {
        manuellBrevmottakerService.opprettBrevmottakerSteg(behandling.id)
        val behandlingsstegstilstand = behandlingsstegstilstandRepository.findByBehandlingId(behandling.id)
        behandlingsstegstilstand.shouldHaveSingleElement {
            it.behandlingssteg == Behandlingssteg.BREVMOTTAKER &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.AUTOUTFØRT
        }
    }

    @Test
    fun `opprettBrevmottakerSteg skal ikke opprette steg når behandling er avsluttet`() {
        behandlingRepository.update(behandling.copy(status = Behandlingsstatus.AVSLUTTET))

        val exception = shouldThrow<RuntimeException> { manuellBrevmottakerService.opprettBrevmottakerSteg(behandling.id) }
        exception.message shouldBe "Behandling med id=${behandling.id} er allerede ferdig behandlet."
    }

    @Test
    fun `opprettBrevmottakerSteg skal ikke opprette steg når behandling er på vent`() {
        kravgrunnlagRepository.insert(Testdata.lagKravgrunnlag(behandling.id))
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)

        behandlingskontrollService.settBehandlingPåVent(
            behandling.id,
            Venteårsak.VENT_PÅ_TILBAKEKREVINGSGRUNNLAG,
            LocalDate.now().plusWeeks(4),
            SecureLog.Context.tom(),
        )

        val exception = shouldThrow<RuntimeException> { manuellBrevmottakerService.opprettBrevmottakerSteg(behandling.id) }
        exception.message shouldBe "Behandling med id=${behandling.id} er på vent."
    }

    @Test
    fun `fjernManuelleBrevmottakereOgTilbakeførSteg skal fjerne brevmottakere og tilbakeføre steget`() {
        kravgrunnlagRepository.insert(Testdata.lagKravgrunnlag(behandling.id))
        lagBehandlingsstegstilstand(behandling.id, Behandlingssteg.FAKTA, Behandlingsstegstatus.KLAR)

        manuellBrevmottakerService.opprettBrevmottakerSteg(behandling.id)
        manuellBrevmottakerService.leggTilBrevmottaker(behandling.id, manuellBrevmottakerRequestDto)
        manuellBrevmottakerService.hentBrevmottakere(behandling.id).shouldHaveSize(1)

        manuellBrevmottakerService.fjernManuelleBrevmottakereOgTilbakeførSteg(behandling.id)
        manuellBrevmottakerService.hentBrevmottakere(behandling.id).shouldHaveSize(0)

        behandlingsstegstilstandRepository.findByBehandlingId(behandling.id).shouldHaveSingleElement {
            it.behandlingssteg == Behandlingssteg.BREVMOTTAKER &&
                it.behandlingsstegsstatus == Behandlingsstegstatus.TILBAKEFØRT
        }
    }

    @Test
    fun `skal hente og legge til navn fra registeroppslag når request inneholder identinformasjon`() {
        every { eregClient.hentOrganisasjon(any()) } returns HentOrganisasjonResponse(Navn("Organisasjon AS"), null)
        val requestMedPersonIdent =
            manuellBrevmottakerRequestDto.copy(
                personIdent = "12345678910",
                manuellAdresseInfo = null,
            )
        manuellBrevmottakerService.leggTilBrevmottaker(behandling.id, requestMedPersonIdent)

        var lagretMottaker = manuellBrevmottakerService.hentBrevmottakere(behandling.id).single()
        lagretMottaker.navn shouldBe "testverdi"

        val requestMedOrgnrUtenKontaktperson =
            manuellBrevmottakerRequestDto.copy(
                navn = " ",
                organisasjonsnummer = "123456789",
                manuellAdresseInfo = null,
            )
        manuellBrevmottakerService.oppdaterBrevmottaker(behandling.id, lagretMottaker.id, requestMedOrgnrUtenKontaktperson)

        lagretMottaker = manuellBrevmottakerService.hentBrevmottakere(behandling.id).single()
        lagretMottaker.navn shouldBe "Bobs Burgers"

        val requestMedOrgnrMedKontaktperson =
            manuellBrevmottakerRequestDto.copy(
                organisasjonsnummer = "123456789",
                manuellAdresseInfo = null,
            )
        manuellBrevmottakerService.oppdaterBrevmottaker(behandling.id, lagretMottaker.id, requestMedOrgnrMedKontaktperson)

        lagretMottaker = manuellBrevmottakerService.hentBrevmottakere(behandling.id).single()
        lagretMottaker.navn shouldBe "Bobs Burgers v/ ${manuellBrevmottakerRequestDto.navn}"
    }

    private fun assertEqualsManuellBrevmottaker(
        a: ManuellBrevmottaker,
        b: ManuellBrevmottakerRequestDto,
    ) {
        a.id.shouldNotBeNull()
        a.orgNr shouldBe b.organisasjonsnummer
        a.ident shouldBe b.personIdent
        a.type shouldBe b.type
        a.vergetype shouldBe b.vergetype
        a.adresselinje1 shouldBe b.manuellAdresseInfo?.adresselinje1
        a.adresselinje2 shouldBe b.manuellAdresseInfo?.adresselinje2
        a.postnummer shouldBe b.manuellAdresseInfo?.postnummer
        a.poststed shouldBe b.manuellAdresseInfo?.poststed
        a.landkode shouldBe b.manuellAdresseInfo?.landkode
    }

    private fun lagBehandlingsstegstilstand(
        behandlingId: UUID,
        behandlingssteg: Behandlingssteg,
        behandlingsstegstatus: Behandlingsstegstatus,
    ) {
        behandlingsstegstilstandRepository.insert(
            Behandlingsstegstilstand(
                behandlingId = behandlingId,
                behandlingssteg = behandlingssteg,
                behandlingsstegsstatus = behandlingsstegstatus,
            ),
        )
    }
}
