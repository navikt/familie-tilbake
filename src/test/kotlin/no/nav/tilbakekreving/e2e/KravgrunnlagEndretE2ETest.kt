package no.nav.tilbakekreving.e2e

import io.kotest.inspectors.forOne
import io.kotest.matchers.collections.shouldBeSingle
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.e2e.KravgrunnlagE2ETest.Companion.QUEUE_NAME
import no.nav.tilbakekreving.entities.FaktastegEntity
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagSammenligning
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.util.UUID

@TestPropertySource(
    properties = ["tilbakekreving.toggles.nyModell.EndretKravgrunnlagVisning=true"],
)
class KravgrunnlagEndretE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Test
    fun `endring i kravgrunnlag for fakta periode`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        val context = opprettBehandling(periode)
        sendKravgrunnlagOgAvventLesing(
            QUEUE_NAME,
            KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = context.fagsystemId,
                vedtakId = context.vedtakId,
                kravgrunnlagId = context.kravgrunnlagId,
                kontrollfelt = "2025-12-24-11.12.13.234567",
                kravStatusKode = "ENDR",
                perioder = listOf(KravgrunnlagGenerator.standardPeriode(periode, feilutbetaltBeløp = 1000.kroner)),
            ),
        )

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingBenyttNyesteKravgrunnlag(context.behandlingId)
        }
        val behandlingEntity = behandlingRepository.hentBehandlinger(tilbakekreving(context.behandlingId).id)
            .single { it.id == context.behandlingId }

        behandlingEntity.faktastegEntity.perioder.single().endringIKravgrunnlag.shouldNotBeNull {
            originalPeriode?.fraEntity() shouldBe periode
            endringIBeløp shouldBe (-2000.00).kroner
        }
    }

    @Test
    fun `endring i kravgrunnlag for vilkårsvurderingsperiode`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        val context = opprettBehandling(periode)
        sendKravgrunnlagOgAvventLesing(
            QUEUE_NAME,
            KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = context.fagsystemId,
                vedtakId = context.vedtakId,
                kravgrunnlagId = context.kravgrunnlagId,
                kontrollfelt = "2025-12-24-11.12.13.234567",
                kravStatusKode = "ENDR",
                perioder = listOf(KravgrunnlagGenerator.standardPeriode(periode, feilutbetaltBeløp = 1000.kroner)),
            ),
        )

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingBenyttNyesteKravgrunnlag(context.behandlingId)
        }
        val behandlingEntity = behandlingRepository.hentBehandlinger(tilbakekreving(context.behandlingId).id)
            .single { it.id == context.behandlingId }

        behandlingEntity.vilkårsvurderingstegEntity.vurderinger.single().endringIKravgrunnlag.shouldNotBeNull {
            originalPeriode?.fraEntity() shouldBe periode
            endringIBeløp shouldBe (-2000.00).kroner
        }
    }

    @Test
    fun `fakta periode holder på siste endring i kravgrunnlag`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        val context = opprettBehandling(periode)
        sendKravgrunnlagOgAvventLesing(
            QUEUE_NAME,
            KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = context.fagsystemId,
                vedtakId = context.vedtakId,
                kravgrunnlagId = context.kravgrunnlagId,
                kontrollfelt = "2025-12-24-11.12.13.234567",
                kravStatusKode = "ENDR",
                perioder = listOf(KravgrunnlagGenerator.standardPeriode(periode, feilutbetaltBeløp = 1000.kroner)),
            ),
        )
        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingBenyttNyesteKravgrunnlag(context.behandlingId)
        }
        sendKravgrunnlagOgAvventLesing(
            QUEUE_NAME,
            KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = context.fagsystemId,
                vedtakId = context.vedtakId,
                kravgrunnlagId = context.kravgrunnlagId,
                kontrollfelt = "2025-12-24-11.12.13.345678",
                kravStatusKode = "ENDR",
                perioder = listOf(KravgrunnlagGenerator.standardPeriode(periode, feilutbetaltBeløp = 2500.kroner)),
            ),
        )

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingBenyttNyesteKravgrunnlag(context.behandlingId)
        }
        val behandlingEntity = behandlingRepository.hentBehandlinger(tilbakekreving(context.behandlingId).id)
            .single { it.id == context.behandlingId }

        behandlingEntity.faktastegEntity.perioder.single().endringIKravgrunnlag.shouldNotBeNull {
            originalPeriode?.fraEntity() shouldBe periode
            endringIBeløp shouldBe (1500.00).kroner
        }
    }

    @Test
    fun `vilkårsvurderingsperiode holder på siste endring i kravgrunnlag`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        val context = opprettBehandling(periode)
        sendKravgrunnlagOgAvventLesing(
            QUEUE_NAME,
            KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = context.fagsystemId,
                vedtakId = context.vedtakId,
                kravgrunnlagId = context.kravgrunnlagId,
                kontrollfelt = "2025-12-24-11.12.13.234567",
                kravStatusKode = "ENDR",
                perioder = listOf(KravgrunnlagGenerator.standardPeriode(periode, feilutbetaltBeløp = 1000.kroner)),
            ),
        )
        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingBenyttNyesteKravgrunnlag(context.behandlingId)
        }
        sendKravgrunnlagOgAvventLesing(
            QUEUE_NAME,
            KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = context.fagsystemId,
                vedtakId = context.vedtakId,
                kravgrunnlagId = context.kravgrunnlagId,
                kontrollfelt = "2025-12-24-11.12.13.345678",
                kravStatusKode = "ENDR",
                perioder = listOf(KravgrunnlagGenerator.standardPeriode(periode, feilutbetaltBeløp = 2500.kroner)),
            ),
        )

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingBenyttNyesteKravgrunnlag(context.behandlingId)
        }
        val behandlingEntity = behandlingRepository.hentBehandlinger(tilbakekreving(context.behandlingId).id)
            .single { it.id == context.behandlingId }

        behandlingEntity.vilkårsvurderingstegEntity.vurderinger.single().endringIKravgrunnlag.shouldNotBeNull {
            originalPeriode?.fraEntity() shouldBe periode
            endringIBeløp shouldBe (1500.00).kroner
        }
    }

    @Test
    fun `fakta periode fjerner endring i kravgrunnlag når den settes til null`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        val context = opprettBehandling(periode)
        sendKravgrunnlagOgAvventLesing(
            QUEUE_NAME,
            KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = context.fagsystemId,
                vedtakId = context.vedtakId,
                kravgrunnlagId = context.kravgrunnlagId,
                kontrollfelt = "2025-12-24-11.12.13.234567",
                kravStatusKode = "ENDR",
                perioder = listOf(KravgrunnlagGenerator.standardPeriode(periode, feilutbetaltBeløp = 1000.kroner)),
            ),
        )

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingBenyttNyesteKravgrunnlag(context.behandlingId)
        }
        val behandlingEntity = behandlingRepository.hentBehandlinger(tilbakekreving(context.behandlingId).id)
            .single { it.id == context.behandlingId }

        val faktaPeriode = behandlingEntity.faktastegEntity.perioder.single()
        val faktaPeriodeUtenEndring = FaktastegEntity.FaktaPeriodeEntity(
            id = faktaPeriode.id,
            faktavurderingRef = faktaPeriode.faktavurderingRef,
            periode = faktaPeriode.periode,
            rettsligGrunnlag = faktaPeriode.rettsligGrunnlag,
            rettsligGrunnlagUnderkategori = faktaPeriode.rettsligGrunnlagUnderkategori,
            endringIKravgrunnlag = null,
        )
        val behandlingUtenEndring = behandlingEntity.copy(
            faktastegEntity = behandlingEntity.faktastegEntity.copy(
                perioder = listOf(faktaPeriodeUtenEndring),
            ),
        )
        behandlingRepository.lagreBehandlinger(listOf(behandlingUtenEndring))

        val lagretBehandling = behandlingRepository.hentBehandlinger(tilbakekreving(context.behandlingId).id)
            .single { it.id == context.behandlingId }

        lagretBehandling.faktastegEntity.perioder.single().endringIKravgrunnlag.shouldBeNull()
    }

    @Test
    fun `vilkårsvurderingsperiode fjerner endret av kravgrunnlag når den settes til null`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        val context = opprettBehandling(periode)
        sendKravgrunnlagOgAvventLesing(
            QUEUE_NAME,
            KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = context.fagsystemId,
                vedtakId = context.vedtakId,
                kravgrunnlagId = context.kravgrunnlagId,
                kontrollfelt = "2025-12-24-11.12.13.234567",
                kravStatusKode = "ENDR",
                perioder = listOf(KravgrunnlagGenerator.standardPeriode(periode, feilutbetaltBeløp = 1000.kroner)),
            ),
        )

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingBenyttNyesteKravgrunnlag(context.behandlingId)
        }
        val behandlingEntity = behandlingRepository.hentBehandlinger(tilbakekreving(context.behandlingId).id)
            .single { it.id == context.behandlingId }

        val vurderingsperiode = behandlingEntity.vilkårsvurderingstegEntity.vurderinger.single()
        behandlingRepository.lagreBehandlinger(
            listOf(
                behandlingEntity.copy(
                    vilkårsvurderingstegEntity = behandlingEntity.vilkårsvurderingstegEntity.copy(
                        vurderinger = listOf(
                            vurderingsperiode.copy(
                                endringIKravgrunnlag = null,
                            ),
                        ),
                    ),
                ),
            ),
        )

        val lagretBehandling = behandlingRepository.hentBehandlinger(tilbakekreving(context.behandlingId).id)
            .single { it.id == context.behandlingId }

        lagretBehandling.vilkårsvurderingstegEntity.vurderinger.single().endringIKravgrunnlag.shouldBeNull()
    }

    @Test
    fun `ny periode i kravgrunnlag lagres i faktasteg, foreldelse og vilkårsvurdering`() {
        val periode = 1.januar(2021) til 31.januar(2021)
        val nyPeriode = 1.februar(2021) til 28.februar(2021)
        val context = opprettBehandling(periode)
        sendKravgrunnlagOgAvventLesing(
            QUEUE_NAME,
            KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = context.fagsystemId,
                vedtakId = context.vedtakId,
                kravgrunnlagId = context.kravgrunnlagId,
                kontrollfelt = "2025-12-24-11.12.13.234567",
                kravStatusKode = "ENDR",
                perioder = listOf(
                    KravgrunnlagGenerator.standardPeriode(periode, feilutbetaltBeløp = 3000.kroner),
                    KravgrunnlagGenerator.standardPeriode(nyPeriode, feilutbetaltBeløp = 3000.kroner),
                ),
            ),
        )

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingBenyttNyesteKravgrunnlag(context.behandlingId)
        }

        val lagretBehandling = behandlingRepository.hentBehandlinger(tilbakekreving(context.behandlingId).id)
            .shouldBeSingle()

        lagretBehandling.faktastegEntity.perioder.forOne {
            it.endringIKravgrunnlag?.nyPeriode?.fraEntity() shouldBe nyPeriode
            it.endringIKravgrunnlag?.type shouldBe KravgrunnlagSammenligning.ForskjellType.NyPeriode
        }

        val foreldelsesperioder = lagretBehandling.foreldelsestegEntity.vurdertePerioder
        foreldelsesperioder.forOne {
            it.endringIKravgrunnlag?.nyPeriode?.fraEntity() shouldBe nyPeriode
            it.endringIKravgrunnlag?.type shouldBe KravgrunnlagSammenligning.ForskjellType.NyPeriode
        }

        lagretBehandling.vilkårsvurderingstegEntity.vurderinger.forOne {
            it.endringIKravgrunnlag?.nyPeriode?.fraEntity() shouldBe nyPeriode
            it.endringIKravgrunnlag?.type shouldBe KravgrunnlagSammenligning.ForskjellType.NyPeriode
        }
    }

    private fun opprettBehandling(periode: Datoperiode): KravgrunnlagContext {
        val vedtakId = KravgrunnlagGenerator.nextPaddedId(6)
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        val kravgrunnlagId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            QUEUE_NAME,
            KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
                vedtakId = vedtakId,
                kravgrunnlagId = kravgrunnlagId,
                perioder = listOf(KravgrunnlagGenerator.standardPeriode(periode, feilutbetaltBeløp = 3000.kroner)),
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))
        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        lagreUttalelse(behandlingId)
        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId.toString(),
                BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }
        utførSteg(behandlingId, BehandlingsstegGenerator.lagIkkeForeldetVurdering(periode))
        utførSteg(behandlingId, BehandlingsstegGenerator.lagVilkårsvurderingFullTilbakekreving(periode))

        return KravgrunnlagContext(
            behandlingId = behandlingId,
            fagsystemId = fagsystemId,
            vedtakId = vedtakId,
            kravgrunnlagId = kravgrunnlagId,
        )
    }

    private data class KravgrunnlagContext(
        val behandlingId: UUID,
        val fagsystemId: String,
        val vedtakId: String,
        val kravgrunnlagId: String,
    )
}
