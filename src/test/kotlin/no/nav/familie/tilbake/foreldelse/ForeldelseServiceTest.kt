package no.nav.familie.tilbake.foreldelse

import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.api.dto.BehandlingsstegForeldelseDto
import no.nav.familie.tilbake.api.dto.ForeldelsesperiodeDto
import no.nav.familie.tilbake.api.dto.PeriodeDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class ForeldelseServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    @Autowired
    private lateinit var foreldelsesRepository: VurdertForeldelseRepository

    @Autowired
    private lateinit var foreldelseService: ForeldelseService

    private var behandling = Testdata.behandling


    @BeforeEach
    fun init() {
        fagsakRepository.insert(Testdata.fagsak)
        behandling = behandlingRepository.insert(Testdata.behandling)

        val kravgrunnlag431 = Testdata.kravgrunnlag431
        val feilkravgrunnlagsbeløp = Testdata.feilKravgrunnlagsbeløp433
        val yteseskravgrunnlagsbeløp = Testdata.ytelKravgrunnlagsbeløp433
        val førsteKravgrunnlagsperiode = Testdata.kravgrunnlagsperiode432
                .copy(periode = Periode(YearMonth.of(2017, 1), YearMonth.of(2017, 1)),
                      beløp = setOf(feilkravgrunnlagsbeløp.copy(id = UUID.randomUUID()),
                                    yteseskravgrunnlagsbeløp.copy(id = UUID.randomUUID())))
        val andreKravgrunnlagsperiode = Testdata.kravgrunnlagsperiode432
                .copy(id = UUID.randomUUID(),
                      periode = Periode(YearMonth.of(2017, 2), YearMonth.of(2017, 2)),
                      beløp = setOf(feilkravgrunnlagsbeløp.copy(id = UUID.randomUUID()),
                                    yteseskravgrunnlagsbeløp.copy(id = UUID.randomUUID())))
        kravgrunnlagRepository.insert(kravgrunnlag431.copy(perioder = setOf(førsteKravgrunnlagsperiode,
                                                                            andreKravgrunnlagsperiode)))
    }

    @Test
    fun `hentVurdertForeldelse skal returnere foreldelse data som skal vurderes`() {
        val vurdertForeldelseDto = foreldelseService.hentVurdertForeldelse(behandling.id)

        assertEquals(1, vurdertForeldelseDto.foreldetPerioder.size)
        val foreldetPeriode = vurdertForeldelseDto.foreldetPerioder[0]
        assertEquals(LocalDate.of(2017, 1, 1), foreldetPeriode.periode.fom)
        assertEquals(LocalDate.of(2017, 2, 28), foreldetPeriode.periode.tom)
        //feilutbetaltBeløp er 10000.00 i Testdata for hver periode
        assertEquals(BigDecimal("20000.00"), foreldetPeriode.feilutbetaltBeløp)
        assertNull(foreldetPeriode.foreldelsesvurderingstype)
        assertNull(foreldetPeriode.begrunnelse)
        assertNull(foreldetPeriode.foreldelsesfrist)
        assertNull(foreldetPeriode.oppdagelsesdato)
    }

    @Test
    fun `lagreVurdertForeldelse skal lagre foreldelses data for en gitt behandling`() {
        foreldelseService
                .lagreVurdertForeldelse(behandling.id,
                                        BehandlingsstegForeldelseDto(
                                                listOf(lagForeldelsesperiode(LocalDate.of(2017, 1, 1),
                                                                             LocalDate.of(2017, 1, 31)))))

        val vurdertForeldelse = foreldelsesRepository.findByBehandlingIdAndAktivIsTrue(behandling.id)
        assertNotNull(vurdertForeldelse)
        assertEquals(1, vurdertForeldelse.foreldelsesperioder.size)
        val vurdertForeldelsesperiode = vurdertForeldelse.foreldelsesperioder.toList()[0]
        assertEquals("foreldelses begrunnelse", vurdertForeldelsesperiode.begrunnelse)
        assertEquals(Foreldelsesvurderingstype.FORELDET, vurdertForeldelsesperiode.foreldelsesvurderingstype)
        assertEquals(LocalDate.of(2017, 2, 28), vurdertForeldelsesperiode.foreldelsesfrist)
        assertNull(vurdertForeldelsesperiode.oppdagelsesdato)
        assertEquals(Periode(YearMonth.of(2017, 1), YearMonth.of(2017, 1)), vurdertForeldelsesperiode.periode)
    }

    @Test
    fun `lagreVurdertForeldelse skal ikke lagre foreldelses data når periode ikke starter med første dato`() {
        val foreldelsesperiode = lagForeldelsesperiode(LocalDate.of(2017, 1, 10),
                                                       LocalDate.of(2017, 1, 31))
        val exception = assertFailsWith<RuntimeException> {
            foreldelseService
                    .lagreVurdertForeldelse(behandling.id,
                                            BehandlingsstegForeldelseDto(
                                                    listOf(foreldelsesperiode)))
        }
        assertEquals("Periode med ${foreldelsesperiode.periode} er mindre enn en måned", exception.message)
    }

    @Test
    fun `lagreVurdertForeldelse skal ikke lagre foreldelses data når periode ikke slutter med siste dato`() {
        val foreldelsesperiode = lagForeldelsesperiode(LocalDate.of(2017, 1, 1),
                                                       LocalDate.of(2017, 1, 27))
        val exception = assertFailsWith<RuntimeException> {
            foreldelseService
                    .lagreVurdertForeldelse(behandling.id,
                                            BehandlingsstegForeldelseDto(
                                                    listOf(foreldelsesperiode)))
        }
        assertEquals("Periode med ${foreldelsesperiode.periode} er mindre enn en måned", exception.message)
    }

    @Test
    fun `beregnBeløp skal beregne feilutbetaltBeløp når saksbehandler deler opp periode`() {
        val beregnetPerioderDto = foreldelseService.beregnBeløp(behandlingId = behandling.id,
                                                                perioder = listOf(PeriodeDto(LocalDate.of(2017, 1, 1),
                                                                                             LocalDate.of(2017, 1, 31)),
                                                                                  PeriodeDto(LocalDate.of(2017, 2, 1),
                                                                                             LocalDate.of(2017, 2, 28))))
        assertEquals(2, beregnetPerioderDto.beregnetPerioder.size)
        assertEquals(PeriodeDto(LocalDate.of(2017, 1, 1),
                                LocalDate.of(2017, 1, 31)), beregnetPerioderDto.beregnetPerioder[0].periode)
        assertEquals(BigDecimal("10000"), beregnetPerioderDto.beregnetPerioder[0].feilutbetaltBeløp)
        assertEquals(PeriodeDto(LocalDate.of(2017, 2, 1),
                                LocalDate.of(2017, 2, 28)), beregnetPerioderDto.beregnetPerioder[1].periode)
        assertEquals(BigDecimal("10000"), beregnetPerioderDto.beregnetPerioder[1].feilutbetaltBeløp)
    }

    @Test
    fun `beregnBeløp skal ikke beregne feilutbetaltBeløp når saksbehandler deler opp periode som ikke starter første dato`() {
        val exception = assertFailsWith<RuntimeException> {
            foreldelseService.beregnBeløp(behandlingId = behandling.id,
                                          perioder = listOf(PeriodeDto(LocalDate.of(2017, 1, 1),
                                                                       LocalDate.of(2017, 1, 31)),
                                                            PeriodeDto(LocalDate.of(2017, 2, 16),
                                                                       LocalDate.of(2017, 2, 28))))
        }
        assertEquals("Periode med ${
            PeriodeDto(LocalDate.of(2017, 2, 16),
                       LocalDate.of(2017, 2, 28))
        } er mindre enn en måned", exception.message)
    }

    @Test
    fun `beregnBeløp skal ikke beregne feilutbetaltBeløp når saksbehandler deler opp periode som ikke slutter siste dato`() {
        val exception = assertFailsWith<RuntimeException> {
            foreldelseService.beregnBeløp(behandlingId = behandling.id,
                                          perioder = listOf(PeriodeDto(LocalDate.of(2017, 1, 1),
                                                                       LocalDate.of(2017, 1, 27)),
                                                            PeriodeDto(LocalDate.of(2017, 2, 1),
                                                                       LocalDate.of(2017, 2, 28))))
        }
        assertEquals("Periode med ${
            PeriodeDto(LocalDate.of(2017, 1, 1),
                       LocalDate.of(2017, 1, 27))
        } er mindre enn en måned", exception.message)
    }


    private fun lagForeldelsesperiode(fom: LocalDate, tom: LocalDate): ForeldelsesperiodeDto {
        return ForeldelsesperiodeDto(
                periode = PeriodeDto(fom, tom),
                begrunnelse = "foreldelses begrunnelse",
                foreldelsesvurderingstype = Foreldelsesvurderingstype.FORELDET,
                foreldelsesfrist = LocalDate.of(2017, 2, 28))
    }

}
