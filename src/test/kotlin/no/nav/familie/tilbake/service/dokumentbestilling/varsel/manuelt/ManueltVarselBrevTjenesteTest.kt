package no.nav.familie.tilbake.service.dokumentbestilling.varsel.manuelt

import io.mockk.every
import io.mockk.excludeRecords
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.domain.tbd.Brevtype
import no.nav.familie.tilbake.integration.pdl.internal.PersonInfo
import no.nav.familie.tilbake.service.FaktaFeilutbetalingTjeneste
import no.nav.familie.tilbake.service.dokumentbestilling.brevmaler.Dokumentmalstype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevMottaker
import no.nav.familie.tilbake.service.dokumentbestilling.felles.EksternDataForBrevTjeneste
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.PdfBrevTjeneste
import no.nav.familie.tilbake.service.modell.BehandlingFeilutbetalingFakta
import no.nav.familie.tilbake.service.modell.LogiskPeriodeMedFaktaDto
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

class ManueltVarselBrevTjenesteTest : OppslagSpringRunnerTest() {

    private val korrigertVarselTekst = "Sender korrigert varselbrev"
    private val varselTekst = "Sender manuelt varselbrev"

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val mockEksternDataForBrevTjeneste: EksternDataForBrevTjeneste = mockk()
    private val mockFeilutbetalingTjeneste: FaktaFeilutbetalingTjeneste = mockk()
    private val mockPdfBrevTjeneste: PdfBrevTjeneste = mockk(relaxed = true)
    private lateinit var manueltVarselBrevTjeneste: ManueltVarselBrevTjeneste
    private var behandling = Testdata.behandling
    private var fagsak = Testdata.fagsak

    @BeforeEach
    fun setup() {
        manueltVarselBrevTjeneste = ManueltVarselBrevTjeneste(behandlingRepository,
                                                              fagsakRepository,
                                                              mockEksternDataForBrevTjeneste,
                                                              mockPdfBrevTjeneste,
                                                              mockFeilutbetalingTjeneste)

        every { mockFeilutbetalingTjeneste.hentBehandlingFeilutbetalingFakta(any()) }
                .returns(lagFeilutbetalingFakta())
        val personinfo = PersonInfo("DUMMY_FØDSELSNUMMER", LocalDate.now(), "Fiona")
        val ident: String = Testdata.fagsak.bruker.ident
        every { mockEksternDataForBrevTjeneste.hentPerson(ident, any()) }.returns(personinfo)
        every {
            mockEksternDataForBrevTjeneste.hentAdresse(any(), any(), any(), any())
        }.returns(Adresseinfo("Test", "12345678901"))

        fagsak = fagsakRepository.insert(fagsak)
        behandling = behandlingRepository.insert(behandling)
    }

    @Test
    fun `skal sende manuelt varselbrev`() {
        manueltVarselBrevTjeneste.sendManueltVarselBrev(behandling.id, varselTekst, BrevMottaker.BRUKER)
        verify {
            mockPdfBrevTjeneste.sendBrev(eq(behandling),
                                         eq(fagsak),
                                         eq(Brevtype.VARSEL),
                                         any(),
                                         eq(9000L),
                                         any())
        }
    }

    @Test
    fun `skal sende korrigert varselbrev`() {
        excludeRecords { mockPdfBrevTjeneste.sendBrev(eq(behandling), eq(fagsak), eq(Brevtype.VARSEL), any(), any(), any()) }
        //arrange
        manueltVarselBrevTjeneste.sendManueltVarselBrev(behandling.id, varselTekst, BrevMottaker.BRUKER)
        val behandlingCopy = behandling.copy(varsler = setOf(Varsel(varseltekst = varselTekst,
                                                                    varselbeløp = 100L)))
        val behandling = behandlingRepository.update(behandlingCopy)

        //act
        manueltVarselBrevTjeneste.sendKorrigertVarselBrev(behandling.id, korrigertVarselTekst, BrevMottaker.BRUKER)

        //assert
        verify {
            mockPdfBrevTjeneste.sendBrev(eq(behandling),
                                         eq(fagsak),
                                         eq(Brevtype.KORRIGERT_VARSEL),
                                         any(),
                                         eq(9000L),
                                         any())
        }
    }

    @Test
    fun `skal sende korrigert varselbrev med verge`() {
        excludeRecords { mockPdfBrevTjeneste.sendBrev(eq(behandling), eq(fagsak), eq(Brevtype.VARSEL), any(), any(), any()) }
        //arrange
        manueltVarselBrevTjeneste.sendManueltVarselBrev(behandling.id, varselTekst, BrevMottaker.BRUKER)
        val behandlingCopy = behandling.copy(varsler = setOf(Varsel(varseltekst = varselTekst,
                                                                    varselbeløp = 100L)),
                                             verger = setOf(Testdata.verge))
        val behandling = behandlingRepository.update(behandlingCopy)

        //act
        manueltVarselBrevTjeneste.sendKorrigertVarselBrev(behandling.id, varselTekst, BrevMottaker.VERGE)

        //assert
        verify {
            mockPdfBrevTjeneste.sendBrev(eq(behandling),
                                         eq(fagsak),
                                         eq(Brevtype.KORRIGERT_VARSEL),
                                         any(),
                                         eq(9000L),
                                         any())
        }
    }

    @Test
    fun `skal forhåndsvise manuelt varselbrev`() {
        every { mockPdfBrevTjeneste.genererForhåndsvisning(any()) }
                .returns(varselTekst.toByteArray())

        val data = manueltVarselBrevTjeneste.hentForhåndsvisningManueltVarselbrev(behandling.id,
                                                                                  Dokumentmalstype.VARSEL,
                                                                                  varselTekst)
        Assertions.assertThat(data).isNotEmpty
    }

    @Test
    fun `skal forhåndsvise korrigert varselbrev`() {
        every { mockPdfBrevTjeneste.genererForhåndsvisning(any()) }
                .returns(
                        varselTekst.toByteArray())
        val behandlingCopy = behandling.copy(varsler = setOf(Varsel(varseltekst = varselTekst,
                                                                    varselbeløp = 100L)))
        behandlingRepository.update(behandlingCopy)

        val data = manueltVarselBrevTjeneste.hentForhåndsvisningManueltVarselbrev(behandling.id,
                                                                                  Dokumentmalstype.KORRIGERT_VARSEL,
                                                                                  varselTekst)
        Assertions.assertThat(data).isNotEmpty
    }

    private fun lagFeilutbetalingFakta(): BehandlingFeilutbetalingFakta {
        val periode = Periode(LocalDate.of(2019, 10, 1),
                              LocalDate.of(2019, 10, 30))

        return BehandlingFeilutbetalingFakta(aktuellFeilUtbetaltBeløp = BigDecimal.valueOf(9000),
                                             perioder = listOf(LogiskPeriodeMedFaktaDto(periode,
                                                                                        BigDecimal.valueOf(9000))),
                                             datoForRevurderingsvedtak = (LocalDate.now()),
                                             totalPeriode = periode)
    }


}