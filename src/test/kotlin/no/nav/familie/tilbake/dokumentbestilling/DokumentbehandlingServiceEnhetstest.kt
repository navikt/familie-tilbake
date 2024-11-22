package no.nav.familie.tilbake.dokumentbestilling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.tilbakekreving.MottakerType
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.brevmaler.Dokumentmalstype
import no.nav.familie.tilbake.dokumentbestilling.innhentdokumentasjon.InnhentDokumentasjonbrevService
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerRepository
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.dokumentbestilling.varsel.manuelt.ManueltVarselbrevService
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.junit.jupiter.api.Test
import java.util.UUID

class DokumentbehandlingServiceEnhetstest {
    private val mockBehandlingRepository: BehandlingRepository = mockk()
    private val mockFagsakRepository: FagsakRepository = mockk()
    private val mockBehandlingskontrollService: BehandlingskontrollService = mockk()
    private val mockKravgrunnlagRepository: KravgrunnlagRepository = mockk()
    private val mockTaskService: TaskService = mockk()
    private val mockManueltVarselBrevService: ManueltVarselbrevService = mockk()
    private val mockInnhentDokumentasjonBrevService: InnhentDokumentasjonbrevService = mockk()
    private val mockManuellBrevmottakerRepository: ManuellBrevmottakerRepository = mockk()

    val dokumentBehandlingService = DokumentbehandlingService(mockBehandlingRepository, mockFagsakRepository, mockBehandlingskontrollService, mockKravgrunnlagRepository, mockTaskService, mockManueltVarselBrevService, mockInnhentDokumentasjonBrevService, mockManuellBrevmottakerRepository)

    @Test
    fun `bestillBrev skal ikke kunne bestille brev når brevmottakerne er ugyldige`() {
        // Arrange
        val behandling = Testdata.lagBehandling()
        every { mockBehandlingRepository.findByIdOrThrow(behandling.id) } returns behandling
        every { mockManuellBrevmottakerRepository.findByBehandlingId(any()) } returns
            listOf(
                ManuellBrevmottaker(
                    id = UUID.randomUUID(),
                    behandlingId = behandling.id,
                    navn = "Test testesen",
                    type = MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE,
                    adresselinje1 = "Testadresse med ugyldig postnummer og poststed (fordi det er i utlandet)",
                    postnummer = "0661",
                    poststed = "Oslo",
                ),
            )

        // Act & assert
        shouldThrow<Feil> {
            dokumentBehandlingService.bestillBrev(behandling.id, Dokumentmalstype.VARSEL, "Bestilt varselbrev")
        }.message shouldBe "Det finnes ugyldige brevmottakere i utsending av manuelt brev"
    }
}
