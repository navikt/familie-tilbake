package no.nav.familie.tilbake.behandling

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerService
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.person.PersonService
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.*

class ValiderBrevmottakerServiceTest {
    private val manuellBrevmottakerService = mockk<ManuellBrevmottakerService>()
    private val fagsakService = mockk<FagsakService>()
    private val personService = mockk<PersonService>()
    val validerBrevmottakerService = ValiderBrevmottakerService(
        manuellBrevmottakerService,
        fagsakService,
        personService
    )
    private val behandlingId = UUID.randomUUID()
    private val fagsak = Testdata.fagsak
    private val manuellBrevmottaker = ManuellBrevmottaker(
        type = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
        behandlingId = behandlingId,
        navn = "Donald Duck",
        adresselinje1 = "adresselinje1",
        postnummer = "postnummer",
        poststed = "poststed",
        landkode = "NO"
    )

    @AfterEach
    internal fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `Skal ikke kaste en Feil exception når en behandling ikke inneholder noen manuelle brevmottakere`() {
        every { manuellBrevmottakerService.hentBrevmottakere(any()) } returns emptyList()
        validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligPersonMedManuelleBrevmottakere(
            behandlingId,
            fagsak.id
        )
        verify(exactly = 1) { manuellBrevmottakerService.hentBrevmottakere(behandlingId) }
        verify(exactly = 0) { fagsakService.hentFagsak(any()) }
        verify(exactly = 0) {
            personService.hentIdenterMedStrengtFortroligAdressebeskyttelse(
                any(),
                any()
            )
        }
    }

    @Test
    fun `Skal kaste en Feil exception når en behandling inneholder en strengt fortrolig person og minst en manuell brevmottaker`() {
        every { manuellBrevmottakerService.hentBrevmottakere(behandlingId) } returns listOf(manuellBrevmottaker)
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { personService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any(), any()) } returns listOf(
            fagsak.bruker.ident
        )
        assertThatThrownBy {
            validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligPersonMedManuelleBrevmottakere(
                behandlingId,
                fagsak.id
            )
        }.isInstanceOf(Feil::class.java)
            .hasMessageContaining("strengt fortrolig adressebeskyttelse og kan ikke kombineres med manuelle brevmottakere")
    }

    @Test
    fun `Skal ikke kaste Feil exception når behandling ikke inneholder strengt fortrolig person og inneholder en manuell brevmottaker`() {
        every { manuellBrevmottakerService.hentBrevmottakere(behandlingId) } returns listOf(manuellBrevmottaker)
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { personService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any(), any()) } returns emptyList()
        validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligPersonMedManuelleBrevmottakere(
            behandlingId,
            fagsak.id
        )
        verify(exactly = 1) {
            personService.hentIdenterMedStrengtFortroligAdressebeskyttelse(
                any(),
                any()
            )
        }
    }

    @Test
    fun `Skal ikke kaste Feil exception når en behandling inneholder strengt fortrolig person og ingen manuelle brevmottakere`() {
        every { manuellBrevmottakerService.hentBrevmottakere(behandlingId) } returns emptyList()
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { personService.hentIdenterMedStrengtFortroligAdressebeskyttelse(any(), any()) } returns listOf(
            fagsak.bruker.ident
        )
        validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligPersonMedManuelleBrevmottakere(
            behandlingId,
            fagsak.id
        )
    }
}
