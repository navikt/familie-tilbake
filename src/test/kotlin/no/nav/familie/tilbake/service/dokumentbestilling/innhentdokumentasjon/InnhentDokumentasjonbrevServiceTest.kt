package no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.tilbakekreving.Fagsystem
import no.nav.familie.tilbake.OppslagSpringRunnerTest
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
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.time.LocalDate

class InnhentDokumentasjonbrevServiceTest : OppslagSpringRunnerTest() {

    private val flereOpplysninger = "Vi trenger flere opplysninger"
    private val mockEksterneDataForBrevService: EksterneDataForBrevService = mockk()

    @Autowired
    lateinit var pdfBrevService: PdfBrevService
    private val fagsakRepository: FagsakRepository = mockk()
    private lateinit var innhentDokumentasjonBrevService: InnhentDokumentasjonbrevService

    @BeforeEach
    fun setup() {
        innhentDokumentasjonBrevService = InnhentDokumentasjonbrevService(fagsakRepository,
                                                                          mockEksterneDataForBrevService,
                                                                          pdfBrevService)
        every { fagsakRepository.findByIdOrThrow(Testdata.fagsak.id) } returns Testdata.fagsak
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
