package no.nav.familie.tilbake.dokumentbestilling.felles.pdf

import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstidspunkt
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstype
import no.nav.familie.tilbake.micrometer.TellerService
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Adresseinfo
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmottager
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.header.Institusjon
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.Brevdata
import org.junit.jupiter.api.Test
import java.util.Base64

internal class PdfBrevServiceTest {
    private val journalføringService: JournalføringService = mockk(relaxed = true)
    private val tellerService: TellerService = mockk(relaxed = true)
    private val taskService: TracableTaskService = mockk(relaxed = true)

    private val pdfBrevService =
        PdfBrevService(
            journalføringService,
            tellerService,
            taskService,
        )

    @Test
    fun `sendBrev oppretter en task med korrekt fritekst`() {
        val fritekst = "Dette er en \n\nfritekst med \n\nlinjeskift"
        val slot = CapturingSlot<Task>()
        every { taskService.save(capture(slot), any()) } returns mockk()

        val brevdata = lagBrevdata()
        val fagsak = Testdata.fagsak()
        pdfBrevService.sendBrev(
            Testdata.lagBehandling(fagsakId = fagsak.id),
            fagsak,
            brevtype = Brevtype.VARSEL,
            brevdata,
            5L,
            fritekst,
        )

        val task = slot.captured
        val base64fritekst = task.metadata.getProperty("fritekst")
        Base64.getDecoder().decode(base64fritekst.toByteArray()).decodeToString() shouldBe fritekst

        val distribusjonstype = task.metadata.getProperty("distribusjonstype")
        distribusjonstype.shouldBe(Distribusjonstype.VIKTIG.name)

        val distribusjonstidspunkt = task.metadata.getProperty("distribusjonstidspunkt")
        distribusjonstidspunkt.shouldBe(Distribusjonstidspunkt.KJERNETID.name)
    }

    @Test
    fun `sendBrev sender vedtaksbrev med riktig distribusjonstype og distribusjonstidspunkt`() {
        val slot = CapturingSlot<Task>()
        every { taskService.save(capture(slot), any()) } returns mockk()
        val brevdata = lagBrevdata()
        val fagsak = Testdata.fagsak()
        pdfBrevService.sendBrev(Testdata.lagBehandling(fagsakId = fagsak.id), fagsak, brevtype = Brevtype.VEDTAK, brevdata)

        val task = slot.captured

        val distribusjonstype = task.metadata.getProperty("distribusjonstype")
        distribusjonstype.shouldBe(Distribusjonstype.VEDTAK.name)

        val distribusjonstidspunkt = task.metadata.getProperty("distribusjonstidspunkt")
        distribusjonstidspunkt.shouldBe(Distribusjonstidspunkt.KJERNETID.name)
    }

    @Test
    fun `sendBrev sender henleggelsesbrev med riktig distribusjonstype og distribusjonstidspunkt`() {
        val slot = CapturingSlot<Task>()
        every { taskService.save(capture(slot), any()) } returns mockk()
        val brevdata = lagBrevdata()
        val fagsak = Testdata.fagsak()
        pdfBrevService.sendBrev(Testdata.lagBehandling(fagsakId = fagsak.id), fagsak, brevtype = Brevtype.HENLEGGELSE, brevdata)

        val task = slot.captured

        val distribusjonstype = task.metadata.getProperty("distribusjonstype")
        distribusjonstype.shouldBe(Distribusjonstype.ANNET.name)

        val distribusjonstidspunkt = task.metadata.getProperty("distribusjonstidspunkt")
        distribusjonstidspunkt.shouldBe(Distribusjonstidspunkt.KJERNETID.name)
    }

    @Test
    fun `sendBrev støtter å sende brev til institusjon med ampsand i navnet`() {
        val slot = CapturingSlot<Task>()
        every { taskService.save(capture(slot), any()) } returns mockk()
        val brevdata =
            lagBrevdata().apply {
                metadata = this.metadata.copy(institusjon = Institusjon("876543210", "Foo & Bar AS"))
            }
        val fagsak = Testdata.fagsak()
        pdfBrevService.sendBrev(Testdata.lagBehandling(fagsakId = fagsak.id), fagsak, brevtype = Brevtype.HENLEGGELSE, brevdata)

        val task = slot.captured

        val distribusjonstype = task.metadata.getProperty("distribusjonstype")
        distribusjonstype.shouldBe(Distribusjonstype.ANNET.name)

        val distribusjonstidspunkt = task.metadata.getProperty("distribusjonstidspunkt")
        distribusjonstidspunkt.shouldBe(Distribusjonstidspunkt.KJERNETID.name)
    }

    private fun lagBrevdata() =
        Brevdata(
            metadata =
                Brevmetadata(
                    sakspartId = "",
                    sakspartsnavn = "",
                    mottageradresse = Adresseinfo(" ", ""),
                    behandlendeEnhetsNavn = "",
                    ansvarligSaksbehandler = "Bob",
                    språkkode = Språkkode.NB,
                    ytelsestype = YtelsestypeDTO.OVERGANGSSTØNAD,
                    saksnummer = "1232456",
                    behandlingstype = Behandlingstype.TILBAKEKREVING,
                    gjelderDødsfall = false,
                ),
            overskrift = "Overskrift",
            mottager = Brevmottager.BRUKER,
            brevtekst = "",
        )
}
