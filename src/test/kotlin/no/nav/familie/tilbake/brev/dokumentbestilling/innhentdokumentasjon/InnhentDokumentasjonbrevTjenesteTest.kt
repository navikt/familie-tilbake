package no.nav.familie.tilbake.brev.dokumentbestilling.innhentdokumentasjon

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.tilbakekreving.Fagsystem
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.brev.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.brev.dokumentbestilling.felles.EksternDataForBrevTjeneste
import no.nav.familie.tilbake.brev.dokumentbestilling.felles.pdf.PdfBrevTjeneste
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.integration.pdl.internal.PersonInfo
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class InnhentDokumentasjonbrevTjenesteTest {

    private val flereOpplysninger = "Vi trenger flere opplysninger"
    private val mockEksternDataForBrevTjeneste: EksternDataForBrevTjeneste = mockk()
    private val mockPdfBrevTjeneste: PdfBrevTjeneste = mockk()
    private val fagsakRepository: FagsakRepository = mockk()
    private var innhentDokumentasjonBrevTjeneste = InnhentDokumentasjonbrevTjeneste(fagsakRepository,
                                                                                    mockEksternDataForBrevTjeneste,
                                                                                    mockPdfBrevTjeneste)

    @BeforeEach
    fun setup() {
        every { fagsakRepository.findByIdOrThrow(Testdata.fagsak.id) } returns Testdata.fagsak
        every { mockPdfBrevTjeneste.genererForhåndsvisning(any()) }
                .returns(flereOpplysninger.toByteArray())
        val personinfo = PersonInfo("DUMMY_FØDSELSNUMMER", LocalDate.now(), "Fiona")
        val ident = Testdata.fagsak.bruker.ident
        every { mockEksternDataForBrevTjeneste.hentPerson(ident, Fagsystem.BA) } returns personinfo
        every { mockEksternDataForBrevTjeneste.hentAdresse(any(), any(), any(), any()) }
                .returns(Adresseinfo("Bob", "DUMMY_FØDSELSNUMMER"))
    }

    @Test
    fun `skal forhåndsvise innhent dokumentasjonbrev`() {
        val data = innhentDokumentasjonBrevTjeneste.hentForhåndsvisningInnhentDokumentasjonBrev(Testdata.behandling,
                                                                                                flereOpplysninger)
        Assertions.assertThat(data).isNotEmpty
    }

}
