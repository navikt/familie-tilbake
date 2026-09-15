package no.nav.familie.tilbake.dokumentbestilling.innhentdokumentasjon

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.data.Testdata
import no.nav.tilbakekreving.pdf.validering.PdfaValidator
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class InnhentDokumentasjonbrevServiceTest : OppslagSpringRunnerTest() {
    private val flereOpplysninger = "Vi trenger flere opplysninger"

    @Autowired
    private lateinit var innhentDokumentasjonBrevService: InnhentDokumentasjonbrevService

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Test
    fun `hentForhåndsvisningInnhentDokumentasjonBrev returnere pdf for innhent dokumentasjonbrev`() {
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        val behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsakId = fagsak.id))

        val data =
            innhentDokumentasjonBrevService.hentForhåndsvisningInnhentDokumentasjonBrev(
                behandling.id,
                flereOpplysninger,
            )

        PdfaValidator.validatePdf(data)
    }
}
