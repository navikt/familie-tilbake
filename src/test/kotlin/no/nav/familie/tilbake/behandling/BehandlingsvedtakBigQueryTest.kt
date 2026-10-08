package no.nav.familie.tilbake.behandling

import io.kotest.inspectors.forSingle
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Iverksettingsstatus
import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.bigQuery.BigQueryAdapterService
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingRepository
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.foreldelse.VurdertForeldelseRepository
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.micrometer.TellerService
import no.nav.familie.tilbake.micrometer.domain.MeldingstellingRepository
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import no.nav.tilbakekreving.api.v1.dto.RettsligGrunnlag
import no.nav.tilbakekreving.bigquery.BigQueryOppsamler
import no.nav.tilbakekreving.bigquery.grensesnittStub
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode.Companion.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import java.util.Optional

class BehandlingsvedtakBigQueryTest {
    @Test
    fun `opprettelse av vedtak og senere oppdatering av iverksettingsstatus`() {
        val fagsak = Testdata.fagsak()
        var behandling = Testdata.lagBehandling(fagsak.id).copy(resultater = emptySet())
        val periode = Testdata.vilkårsperiode(januar(2021) til januar(2021)).copy(
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
            aktsomhet = null,
            godTro = requireNotNull(Testdata.vilkårsperiode().godTro).copy(beløpErIBehold = false, beløpTilbakekreves = null),
        )
        val vurdering = Testdata.lagVilkårsvurdering(behandling.id, setOf(periode))
        val kravgrunnlag = Testdata.lagKravgrunnlag(
            behandling.id,
            perioder = setOf(Testdata.lagKravgrunnlagsperiode(januar(2021) til januar(2021))),
            fagområdekode = Fagområdekode.BA,
        )
        val oppsamler = BigQueryOppsamler()
        val behandlinger = grensesnittStub<BehandlingRepository> { metode, argumenter ->
            when (metode) {
                "findById" -> {
                    argumenter.single() shouldBe behandling.id
                    Optional.of(behandling)
                }
                "update" -> (argumenter.single() as Behandling).also { behandling = it }
                else -> error("Uventet behandlingskall: $metode")
            }
        }
        val fagsaker = grensesnittStub<FagsakRepository> { metode, argumenter ->
            when (metode) {
                "findById" -> {
                    argumenter.single() shouldBe fagsak.id
                    Optional.of(fagsak)
                }
                "finnFagsakForBehandlingId" -> {
                    argumenter.single() shouldBe behandling.id
                    fagsak
                }
                else -> error("Uventet fagsakkall: $metode")
            }
        }
        val kravgrunnlagRepository = grensesnittStub<KravgrunnlagRepository> { metode, argumenter ->
            argumenter.single() shouldBe behandling.id
            when (metode) {
                "findByBehandlingId" -> listOf(kravgrunnlag)
                "findByBehandlingIdAndAktivIsTrue" -> kravgrunnlag
                else -> error("Uventet kravgrunnlag-kall: $metode")
            }
        }
        val vurderinger = grensesnittStub<VilkårsvurderingRepository> { metode, argumenter ->
            check(metode == "findByBehandlingIdAndAktivIsTrue")
            argumenter.single() shouldBe behandling.id
            listOf(vurdering)
        }
        val foreldelse = grensesnittStub<VurdertForeldelseRepository> { metode, argumenter ->
            check(metode == "findByBehandlingIdAndAktivIsTrue")
            argumenter.single() shouldBe behandling.id
            emptyList<Any>()
        }
        val logService = LogService(fagsaker)
        val fakta = FaktaFeilutbetalingService(
            behandlinger,
            grensesnittStub<FaktaFeilutbetalingRepository> { metode, _ -> error("Uventet faktakall: $metode") },
            kravgrunnlagRepository,
            logService,
        )
        val beregning = TilbakekrevingsberegningService(
            kravgrunnlagRepository,
            foreldelse,
            vurderinger,
            behandlinger,
            fakta,
            logService,
        )
        val adapter = BigQueryAdapterService(kravgrunnlagRepository, oppsamler, fagsaker, vurderinger)
        val teller = TellerService(
            fagsaker,
            grensesnittStub<MeldingstellingRepository> { metode, _ -> error("Uventet meldingskall: $metode") },
        )
        val service = BehandlingsvedtakService(behandlinger, teller, beregning, adapter)

        oppsamler.vurderinger.shouldBeEmpty()
        service.opprettBehandlingsvedtak(behandling.id)
        oppsamler.vurderinger.forSingle { data ->
            data.behandlingId shouldBe behandling.id.toString()
            data.ytelse shouldBe fagsak.fagsystem.navn
            data.perioder.forSingle {
                it.periodeId shouldBe periode.id
                it.periode shouldBe periode.periode.toDatoperiode()
                it.rettsligGrunnlag shouldBe RettsligGrunnlag.GOD_TRO
            }
        }

        service.oppdaterBehandlingsvedtak(behandling.id, Iverksettingsstatus.IVERKSATT)
        oppsamler.vurderinger.shouldHaveSize(1)
        oppsamler.behandlinger.shouldHaveSize(2)
    }
}
