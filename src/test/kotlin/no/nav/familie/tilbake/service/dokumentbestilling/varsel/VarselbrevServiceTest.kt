package no.nav.familie.tilbake.service.dokumentbestilling.varsel

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.FeilutbetaltePerioderDto
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Periode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.PdfBrevService
import no.nav.familie.tilbake.service.pdfgen.validering.PdfaValidator
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class VarselbrevServiceTest : OppslagSpringRunnerTest() {

    private val fagsakRepository: FagsakRepository = mockk()
    private val eksterneDataForBrevService: EksterneDataForBrevService = mockk(relaxed = true)

    @Autowired
    private lateinit var pdfBrevService: PdfBrevService

    @Autowired
    private lateinit var varselbrevUtil: VarselbrevUtil

    private lateinit var varselbrevService: VarselbrevService

    @BeforeEach
    fun init() {
        varselbrevService = VarselbrevService(fagsakRepository,
                                              eksterneDataForBrevService,
                                              pdfBrevService,
                                              varselbrevUtil)

        val personinfo = Personinfo("28056325874", LocalDate.now(), "Fiona")

        every { eksterneDataForBrevService.hentPerson(Testdata.fagsak.bruker.ident, any()) }.returns(personinfo)
        every {
            eksterneDataForBrevService.hentAdresse(any(), any(), any<Verge>(), any())
        }.returns(Adresseinfo("12345678901", "Test"))
    }


    @Test
    fun hentForhåndsvisningVarselbrev() {
        val forhåndsvisVarselbrevRequest =
                ForhåndsvisVarselbrevRequest("Dette er et varsel!",
                                             Ytelsestype.OVERGANGSSTØNAD,
                                             "1570",
                                             "Bodø",
                                             "321321",
                                             Språkkode.NB,
                                             LocalDate.now(),
                                             FeilutbetaltePerioderDto(157468, listOf(Periode(LocalDate.of(2020, 5, 4),
                                                                                             LocalDate.now()))),
                                             Fagsystem.EF,
                                             "321654",
                                             Testdata.fagsak.bruker.ident,
                                             null)

        val bytes = varselbrevService.hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest)

        PdfaValidator.validatePdf(bytes)
    }
}