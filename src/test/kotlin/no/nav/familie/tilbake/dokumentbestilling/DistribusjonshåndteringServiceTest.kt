package no.nav.familie.tilbake.dokumentbestilling

import io.mockk.mockk
import no.nav.familie.tilbake.data.Testdata
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DistribusjonshåndteringServiceTest {

    private lateinit var distribusjonshåndteringService: DistribusjonshåndteringService

    @BeforeEach
    fun setUp() {
        distribusjonshåndteringService = DistribusjonshåndteringService(
            behandlingRepository = mockk(),
            fagsakRepository = mockk(),
            manuelleBrevmottakerRepository = mockk(),
            pdfBrevService = mockk(),
            eksterneDataForBrevService = mockk(),
            organisasjonService = mockk(),
            featureToggleService = mockk(),
            vedtaksbrevgrunnlagService = mockk()
        )
    }

    @Test
    fun sendBrev() {
    }

    @Test
    fun genererMetadataForBrev() {
        distribusjonshåndteringService.genererMetadataForBrev(
            Testdata.behandling.id,
            Testdata.vedtaksbrevgrunnlag
        )
    }
}