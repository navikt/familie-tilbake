package no.nav.familie.tilbake.service.dokumentbestilling.varsel.manuelt

import io.mockk.every
import io.mockk.excludeRecords
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.kontrakter.felles.tilbakekreving.Faktainfo
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingDto
import no.nav.familie.tilbake.api.dto.FeilutbetalingsperiodeDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.domain.tbd.Brevtype
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.integration.pdl.internal.Personinfo
import no.nav.familie.tilbake.service.dokumentbestilling.brevmaler.Dokumentmalstype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Adresseinfo
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.service.dokumentbestilling.felles.EksterneDataForBrevService
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.PdfBrevService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate

class ManueltVarselbrevServiceTest : OppslagSpringRunnerTest() {

    private val korrigertVarseltekst = "Sender korrigert varselbrev"
    private val varseltekst = "Sender manuelt varselbrev"

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    private val mockEksterneDataForBrevService: EksterneDataForBrevService = mockk()
    private val mockFeilutbetalingService: FaktaFeilutbetalingService = mockk()
    private val mockPdfBrevService: PdfBrevService = mockk(relaxed = true)
    private lateinit var manueltVarselbrevService: ManueltVarselbrevService
    private var behandling = Testdata.behandling
    private var fagsak = Testdata.fagsak

    @BeforeEach
    fun setup() {
        manueltVarselbrevService = ManueltVarselbrevService(behandlingRepository,
                                                            fagsakRepository,
                                                            mockEksterneDataForBrevService,
                                                            mockPdfBrevService,
                                                            mockFeilutbetalingService)

        every { mockFeilutbetalingService.hentFaktaomfeilutbetaling(any()) }
                .returns(lagFeilutbetaling())
        val personinfo = Personinfo("DUMMY_FØDSELSNUMMER", LocalDate.now(), "Fiona")
        val ident: String = Testdata.fagsak.bruker.ident
        every { mockEksterneDataForBrevService.hentPerson(ident, any()) }.returns(personinfo)
        every {
            mockEksterneDataForBrevService.hentAdresse(any(), any(), any(), any())
        }.returns(Adresseinfo("Test", "12345678901"))

        fagsak = fagsakRepository.insert(fagsak)
        behandling = behandlingRepository.insert(behandling)
    }

    @Test
    fun `sendManueltVarselBrev skal sende manuelt varselbrev`() {
        manueltVarselbrevService.sendManueltVarselBrev(behandling.id, varseltekst, Brevmottager.BRUKER)
        verify {
            mockPdfBrevService.sendBrev(eq(behandling),
                                         eq(fagsak),
                                         eq(Brevtype.VARSEL),
                                         any(),
                                         eq(9000L),
                                         any())
        }
    }

    @Test
    fun `sendKorrigertVarselBrev skal sende korrigert varselbrev`() {
        excludeRecords { mockPdfBrevService.sendBrev(eq(behandling), eq(fagsak), eq(Brevtype.VARSEL), any(), any(), any()) }
        //arrange
        manueltVarselbrevService.sendManueltVarselBrev(behandling.id, varseltekst, Brevmottager.BRUKER)
        val behandlingCopy = behandling.copy(varsler = setOf(Varsel(varseltekst = varseltekst,
                                                                    varselbeløp = 100L)))
        val behandling = behandlingRepository.update(behandlingCopy)

        //act
        manueltVarselbrevService.sendKorrigertVarselBrev(behandling.id, korrigertVarseltekst, Brevmottager.BRUKER)

        //assert
        verify {
            mockPdfBrevService.sendBrev(eq(behandling),
                                         eq(fagsak),
                                         eq(Brevtype.KORRIGERT_VARSEL),
                                         any(),
                                         eq(9000L),
                                         any())
        }
    }

    @Test
    fun `sendKorrigertVarselBrev skal sende korrigert varselbrev med verge`() {
        excludeRecords { mockPdfBrevService.sendBrev(eq(behandling), eq(fagsak), eq(Brevtype.VARSEL), any(), any(), any()) }
        //arrange
        manueltVarselbrevService.sendManueltVarselBrev(behandling.id, varseltekst, Brevmottager.BRUKER)
        val behandlingCopy = behandling.copy(varsler = setOf(Varsel(varseltekst = varseltekst,
                                                                    varselbeløp = 100L)),
                                             verger = setOf(Testdata.verge))
        val behandling = behandlingRepository.update(behandlingCopy)

        //act
        manueltVarselbrevService.sendKorrigertVarselBrev(behandling.id, varseltekst, Brevmottager.VERGE)

        //assert
        verify {
            mockPdfBrevService.sendBrev(eq(behandling),
                                         eq(fagsak),
                                         eq(Brevtype.KORRIGERT_VARSEL),
                                         any(),
                                         eq(9000L),
                                         any())
        }
    }

    @Test
    fun `hentForhåndsvisningManueltVarselbrev skal forhåndsvise manuelt varselbrev`() {
        every { mockPdfBrevService.genererForhåndsvisning(any()) }
                .returns(varseltekst.toByteArray())

        val data = manueltVarselbrevService.hentForhåndsvisningManueltVarselbrev(behandling.id,
                                                                                 Dokumentmalstype.VARSEL,
                                                                                 varseltekst)
        Assertions.assertThat(data).isNotEmpty
    }

    @Test
    fun `hentForhåndsvisningManueltVarselbrev skal forhåndsvise korrigert varselbrev`() {
        every { mockPdfBrevService.genererForhåndsvisning(any()) }
                .returns(
                        varseltekst.toByteArray())
        val behandlingCopy = behandling.copy(varsler = setOf(Varsel(varseltekst = varseltekst,
                                                                    varselbeløp = 100L)))
        behandlingRepository.update(behandlingCopy)

        val data = manueltVarselbrevService.hentForhåndsvisningManueltVarselbrev(behandling.id,
                                                                                 Dokumentmalstype.KORRIGERT_VARSEL,
                                                                                 varseltekst)
        Assertions.assertThat(data).isNotEmpty
    }

    private fun lagFeilutbetaling(): FaktaFeilutbetalingDto {
        val periode = Periode(LocalDate.of(2019, 10, 1),
                              LocalDate.of(2019, 10, 30))

        return FaktaFeilutbetalingDto(totaltFeilutbetaltBeløp = BigDecimal(9000),
                                      totalFeilutbetaltPeriode = periode,
                                      feilutbetaltePerioder = setOf(
                                              FeilutbetalingsperiodeDto(periode = periode,
                                                                        feilutbetaltBeløp = BigDecimal(9000))),
                                      revurderingsvedtaksdato = LocalDate.now().minusDays(1),
                                      begrunnelse = "",
                                      faktainfo = Faktainfo(revurderingsårsak = "testverdi",
                                                            revurderingsresultat = "testverdi",
                                                            tilbakekrevingsvalg =
                                                            Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL))

    }


}
