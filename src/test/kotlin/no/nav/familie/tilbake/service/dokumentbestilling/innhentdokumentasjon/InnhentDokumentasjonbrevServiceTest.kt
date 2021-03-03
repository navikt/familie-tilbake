package no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.tilbakekreving.Fagsystem
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.PdfBrevService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InnhentDokumentasjonbrevServiceTest {

    private val flereOpplysninger = "Vi trenger flere opplysninger"
    private val mockEksterneDataForBrevService: EksterneDataForBrevService = mockk()
    private val mockPdfBrevService: PdfBrevService = mockk()
    private val fagsakRepository: FagsakRepository = mockk()
    private var innhentDokumentasjonBrevService = InnhentDokumentasjonbrevService(fagsakRepository,
                                                                                  mockEksterneDataForBrevService,
                                                                                  mockPdfBrevService)

    @BeforeEach
    fun setup() {
        every { fagsakRepository.findByIdOrThrow(Testdata.fagsak.id) } returns Testdata.fagsak
        every { mockPdfBrevService.genererForhåndsvisning(any()) }
                .returns(flereOpplysninger.toByteArray())
        val personinfo = Personinfo("DUMMY_FØDSELSNUMMER", LocalDate.now(), "Fiona")
        val ident = Testdata.fagsak.bruker.ident
        every { mockEksterneDataForBrevService.hentPerson(ident, Fagsystem.BA) } returns personinfo
        every { mockEksterneDataForBrevService.hentAdresse(any(), any(), any<Verge>(), any()) }
                .returns(Adresseinfo("Bob", "DUMMY_FØDSELSNUMMER"))
    }

    @Test
    fun `hentForhåndsvisningInnhentDokumentasjonBrev skal forhåndsvise innhent dokumentasjonbrev`() {
        val data = innhentDokumentasjonBrevService.hentForhåndsvisningInnhentDokumentasjonBrev(Testdata.behandling,
                                                                                                flereOpplysninger)
        Assertions.assertThat(data).isNotEmpty
    }

}
