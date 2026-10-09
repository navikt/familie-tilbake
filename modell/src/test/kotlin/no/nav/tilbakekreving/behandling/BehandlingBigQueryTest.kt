package no.nav.tilbakekreving.behandling

import io.kotest.inspectors.forSingle
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.KlokkeStub
import no.nav.tilbakekreving.ModellTestdata.forårsaketAvNav
import no.nav.tilbakekreving.SideeffektContext
import no.nav.tilbakekreving.api.v1.dto.BigQueryBehandlingDataDto
import no.nav.tilbakekreving.api.v1.dto.BigQueryVilkårsvurderingDataDto
import no.nav.tilbakekreving.api.v1.dto.RettsligGrunnlag
import no.nav.tilbakekreving.behandling
import no.nav.tilbakekreving.behandling.saksbehandling.FatteVedtakSteg
import no.nav.tilbakekreving.beslutterContext
import no.nav.tilbakekreving.bigquery.BigQueryService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.faktastegVurdering
import no.nav.tilbakekreving.fatteVedtakVurdering
import no.nav.tilbakekreving.foreldelseVurdering
import no.nav.tilbakekreving.godkjenning
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.saksbehandlerContext
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.tilstand.TilBehandling
import org.junit.jupiter.api.Test

class BehandlingBigQueryTest {
    @Test
    fun `godkjent vedtak etter saksbehandling og senere oppdatering`() {
        val klokke = KlokkeStub(1.januar(2022))
        val behandling = behandling(klokke = klokke)
        val bigQuery = BigQueryOppsamler()
        val saksbehandler = saksbehandlerContext(klokke = klokke).medBigQuery(bigQuery)
        val beslutter = beslutterContext(klokke = klokke).medBigQuery(bigQuery)
        val observatør = BehandlingObservatørOppsamler()
        val periode = 1.januar(2021) til 31.januar(2021)

        behandling.utførEndring({ TilBehandling }, saksbehandler, observatør, Ytelse.Barnetrygd, behandling.id.toString()) {
            medSaksbehandling(saksbehandler) {
                lagreForhåndsvarselUnntak(BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG, "Unntak i testen")
                lagreUttalelse(UttalelseVurdering.JA, null, null)
                vurderFakta(faktastegVurdering())
                vurderForeldelse(periode, foreldelseVurdering())
                vurderVilkår(periode, forårsaketAvNav().godTro())
                foreslåVedtak()
            }
        }
        bigQuery.vurderinger.shouldBeEmpty()
        val periodeId = behandling.hentVilkårsvurderingsperioder().single().periodeId

        behandling.utførEndring({ TilBehandling }, beslutter, observatør, Ytelse.Barnetrygd, behandling.id.toString()) {
            medSaksbehandling(beslutter) {
                fatteVedtak(godkjenning().filter { it.first != Behandlingssteg.FORESLÅ_VEDTAK })
            }
        }
        bigQuery.vurderinger.shouldBeEmpty()

        behandling.utførEndring({ TilBehandling }, beslutter, observatør, Ytelse.Barnetrygd, behandling.id.toString()) {
            medSaksbehandling(beslutter) {
                fatteVedtak(godkjenning().filter { it.first == Behandlingssteg.FORESLÅ_VEDTAK })
            }
        }

        bigQuery.vurderinger.forSingle { data ->
            data.behandlingId shouldBe behandling.id.toString()
            data.ytelse shouldBe "Barnetrygd"
            data.perioder.forSingle {
                it.periodeId shouldBe periodeId
                it.periode shouldBe periode
                it.rettsligGrunnlag shouldBe RettsligGrunnlag.GOD_TRO
            }
        }

        behandling.utførEndring({ TilBehandling }, saksbehandler, observatør, Ytelse.Barnetrygd, behandling.id.toString()) {
            oppdaterBehandlendeEnhet("0425")
        }
        bigQuery.vurderinger.shouldHaveSize(1)
        bigQuery.behandlinger.shouldHaveSize(4)
    }

    @Test
    fun `underkjent vedtak etter fullført saksbehandling`() {
        val klokke = KlokkeStub(1.januar(2022))
        val behandling = behandling(klokke = klokke)
        val bigQuery = BigQueryOppsamler()
        val saksbehandler = saksbehandlerContext(klokke = klokke).medBigQuery(bigQuery)
        val beslutter = beslutterContext(klokke = klokke).medBigQuery(bigQuery)
        val observatør = BehandlingObservatørOppsamler()
        behandling.medSaksbehandling(saksbehandler) {
            lagreForhåndsvarselUnntak(BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG, "Unntak i testen")
            lagreUttalelse(UttalelseVurdering.JA, null, null)
            vurderFakta(faktastegVurdering())
            vurderForeldelse(1.januar(2021) til 31.januar(2021), foreldelseVurdering())
            vurderVilkår(1.januar(2021) til 31.januar(2021), forårsaketAvNav().godTro())
            foreslåVedtak()
        }

        behandling.utførEndring({ TilBehandling }, beslutter, observatør, Ytelse.Barnetrygd, behandling.id.toString()) {
            medSaksbehandling(beslutter) {
                fatteVedtak(fatteVedtakVurdering(Behandlingssteg.FAKTA to FatteVedtakSteg.Vurdering.Underkjent("Ny vurdering")))
            }
        }
        bigQuery.vurderinger.shouldBeEmpty()
        bigQuery.behandlinger.shouldHaveSize(1)
    }

    private fun SideeffektContext.medBigQuery(bigQuery: BigQueryService) = SideeffektContext(
        behandler,
        endringObservatør,
        behovObservatør,
        bigQuery,
        features,
        klokke,
        behandlingslogg,
    )

    private class BigQueryOppsamler : BigQueryService {
        val vurderinger = mutableListOf<BigQueryVilkårsvurderingDataDto>()
        val behandlinger = mutableListOf<BigQueryBehandlingDataDto>()

        override fun lagreVilkårsvurdering(bigqueryData: BigQueryVilkårsvurderingDataDto) {
            vurderinger.add(bigqueryData)
        }

        override fun oppdaterBehandling(bigqueryData: BigQueryBehandlingDataDto) {
            behandlinger.add(bigqueryData)
        }
    }
}
