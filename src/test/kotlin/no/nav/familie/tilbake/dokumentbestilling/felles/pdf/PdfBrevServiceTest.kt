package no.nav.familie.tilbake.dokumentbestilling.felles.pdf

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.mockk
import io.mockk.runs
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.prosessering.domene.ITask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.micrometer.TellerService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.postgresql.util.Base64
import java.util.UUID

internal class PdfBrevServiceTest {

    private val journalføringService: JournalføringService = mockk(relaxed = true)
    private val tellerService: TellerService = mockk(relaxed = true)
    private val taskService: TaskService = mockk(relaxed = true)

    private val pdfBrevService = PdfBrevService(journalføringService,
                                                tellerService,
                                                taskService)


    @Test
    fun `sendBrev oppretter en task med korrekt fritekst`() {
        val fritekst = "Dette er en \n\nfritekst med \n\nlinjeskift"
        val slot = CapturingSlot<Task>()
        every { taskService.save(capture(slot)) } returns mockk()
        val brevdata = Brevdata(metadata = Brevmetadata(sakspartId = "",
                                                        sakspartsnavn = "",
                                                        mottageradresse = Adresseinfo(" ", ""),
                                                        behandlendeEnhetsNavn = "",
                                                        ansvarligSaksbehandler = "Bob",
                                                        språkkode = Språkkode.NB,
                                                        ytelsestype = Ytelsestype.OVERGANGSSTØNAD,
                                                        behandlingstype = Behandlingstype.TILBAKEKREVING),
                                overskrift = "",
                                mottager = Brevmottager.BRUKER,
                                brevtekst = "")

        pdfBrevService.sendBrev(Testdata.behandling, Testdata.fagsak, brevtype = Brevtype.VARSEL, brevdata, 5L, fritekst)

        val base64fritekst = slot.captured.metadata.getProperty("fritekst")
        assertThat(Base64.decode(base64fritekst).decodeToString()).isEqualTo(fritekst)
    }
}