package no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.tilbakekreving.Vergetype
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.ManuellBrevmottaker
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.domene.MottakerType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ManuellBrevmottakerTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var manuellBrevmottakerRepository: ManuellBrevmottakerRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private lateinit var behandling: Behandling

    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandling = behandlingRepository.insert(Testdata.behandling)
    }

    @Test
    fun `legger man til brevMottaker i db mister man ingen informasjon`() {
        val manuellBrevmottaker1 = ManuellBrevmottaker(
            behandlingId = behandling.id,
            type = MottakerType.VERGE,
            vergetype = Vergetype.ADVOKAT,
            navn = "Kari Nordmann",
            adresselinje1 = "test adresse1",
            postnummer = "0000",
            poststed = "OSLO",
            landkode = "NO"
        )

        val manuellBrevmottaker2 = ManuellBrevmottaker(
            behandlingId = behandling.id,
            type = MottakerType.VERGE,
            vergetype = Vergetype.ADVOKAT,
            navn = "Ola Nordmann",
            adresselinje1 = "test adresse2",
            postnummer = "0001",
            poststed = "OSLO",
            landkode = "NO"
        )

        manuellBrevmottakerRepository.insertAll(listOf(manuellBrevmottaker1, manuellBrevmottaker2))

        val brevMottakereIDb = manuellBrevmottakerRepository.findByBehandlingId(behandling.id)
        brevMottakereIDb.shouldHaveSize(2)
        val brevMottakerInDb = brevMottakereIDb.singleOrNull { it.navn.equals("Kari Nordmann") }.shouldNotBeNull()

        brevMottakerInDb.adresselinje1 shouldBe manuellBrevmottaker1.adresselinje1
        brevMottakerInDb.adresselinje2 shouldBe manuellBrevmottaker1.adresselinje2
        brevMottakerInDb.postnummer shouldBe manuellBrevmottaker1.postnummer
        brevMottakerInDb.poststed shouldBe manuellBrevmottaker1.poststed
        brevMottakerInDb.landkode shouldBe manuellBrevmottaker1.landkode
        brevMottakerInDb.vergetype shouldBe manuellBrevmottaker1.vergetype
        brevMottakerInDb.type shouldBe manuellBrevmottaker1.type
    }
}
