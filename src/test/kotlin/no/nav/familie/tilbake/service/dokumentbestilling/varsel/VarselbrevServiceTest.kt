package no.nav.familie.tilbake.service.dokumentbestilling.varsel

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.tilbakekreving.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.Periode
import no.nav.familie.kontrakter.felles.tilbakekreving.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.FeilutbetaltePerioderDto
import no.nav.familie.tilbake.api.dto.ForhåndsvisVarselbrevRequest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.integration.pdl.PdlClient
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.PdfBrevService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.time.LocalDate

internal class VarselbrevServiceTest : OppslagSpringRunnerTest() {

    private val behandlingRepository: BehandlingRepository = mockk()
    private val fagsakRepository: FagsakRepository = mockk()
    private val eksterneDataForBrevService: EksterneDataForBrevService = mockk(relaxed = true)

    @Autowired
    private lateinit var pdfBrevService: PdfBrevService

    private lateinit var varselbrevService: VarselbrevService

    @BeforeEach
    fun init() {
        varselbrevService = VarselbrevService(behandlingRepository,
                                              fagsakRepository,
                                              eksterneDataForBrevService,
                                              pdfBrevService)

        val personinfo = Personinfo("28056325874", LocalDate.now(), "Fiona")

        every { eksterneDataForBrevService.hentPerson(Testdata.fagsak.bruker.ident, any()) }.returns(personinfo)
        every {
            eksterneDataForBrevService.hentAdresse(any(), any(), any<Verge>(), any())
        }.returns(Adresseinfo("Test", "12345678901"))
    }


    @Test
    fun hentForhåndsvisningVarselbrev() {
        val forhåndsvisVarselbrevRequest =
                ForhåndsvisVarselbrevRequest("Dette er et varsel!",
                                             Ytelsestype.OVERGANGSSTØNAD,
                                             "1570",
                                             "Bodø",
                                             Språkkode.NB,
                                             LocalDate.now(),
                                             FeilutbetaltePerioderDto(157468, listOf(Periode(LocalDate.of(2020, 5, 4),
                                                                                             LocalDate.now()))),
                                             Fagsystem.EF,
                                             "321654",
                                             Testdata.fagsak.bruker.ident,
                                             null)

        val bytes = varselbrevService.hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest)

        assertThat(bytes).isNotEmpty
    }
}