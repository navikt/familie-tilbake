package no.nav.familie.tilbake.dokumentbestilling

import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DistribusjonshåndteringServiceTest {

    private lateinit var distribusjonshåndteringService: DistribusjonshåndteringService

    @BeforeEach
    fun setUp() {
        distribusjonshåndteringService = DistribusjonshåndteringService(
            brevmetadataUtil = mockk(),
            fagsakRepository = mockk(),
            manuelleBrevmottakerRepository = mockk(),
            pdfBrevService = mockk(),
            vedtaksbrevgrunnlagService = mockk(),
            featureToggleService = mockk(),
        )
    }

    @Test
    fun sendBrev() {

    }

}