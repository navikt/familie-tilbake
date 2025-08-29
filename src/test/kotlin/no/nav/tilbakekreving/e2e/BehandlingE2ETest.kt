package no.nav.tilbakekreving.e2e

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.integrasjoner.KafkaProducerStub
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BehandlingE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var kafkaProducer: KafkaProducerStub

    private val ansvarligSaksbehandler = "Z999999"
    private val ansvarligBeslutter = "Z111111"

    @Test
    fun `endringer i behandling skal føre til kafka-meldinger til dvh`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )

        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(),
        )

        val dvhHendelser = kafkaProducer.finnSaksdata(behandlingId)
        dvhHendelser.size shouldBe 2

        dvhHendelser[0].ansvarligSaksbehandler shouldBe "VL"
        dvhHendelser[0].ansvarligBeslutter shouldBe null
        dvhHendelser[0].ansvarligEnhet shouldBe "Ukjent"
        dvhHendelser[0].behandlingsstatus shouldBe Behandlingsstatus.OPPRETTET

        dvhHendelser[1].ansvarligSaksbehandler shouldBe ansvarligSaksbehandler
        dvhHendelser[1].ansvarligBeslutter shouldBe null
        dvhHendelser[1].ansvarligEnhet shouldBe "Ukjent"
        dvhHendelser[1].behandlingsstatus shouldBe Behandlingsstatus.UTREDES

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(),
        )
        dvhHendelser.size shouldBe 3
        dvhHendelser.last().ansvarligSaksbehandler shouldBe ansvarligSaksbehandler
        dvhHendelser.last().ansvarligBeslutter shouldBe null
        dvhHendelser.last().ansvarligEnhet shouldBe "Ukjent"
        dvhHendelser.last().behandlingsstatus shouldBe Behandlingsstatus.UTREDES

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingFullTilbakekreving(),
        )
        dvhHendelser.size shouldBe 4
        dvhHendelser.last().ansvarligSaksbehandler shouldBe ansvarligSaksbehandler
        dvhHendelser.last().ansvarligBeslutter shouldBe null
        dvhHendelser.last().ansvarligEnhet shouldBe "Ukjent"
        dvhHendelser.last().behandlingsstatus shouldBe Behandlingsstatus.UTREDES

        utførSteg(
            ident = ansvarligSaksbehandler,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagForeslåVedtakVurdering(),
        )
        dvhHendelser.size shouldBe 5
        dvhHendelser.last().ansvarligSaksbehandler shouldBe ansvarligSaksbehandler
        dvhHendelser.last().ansvarligBeslutter shouldBe null
        dvhHendelser.last().ansvarligEnhet shouldBe "Ukjent"
        dvhHendelser.last().behandlingsstatus shouldBe Behandlingsstatus.FATTER_VEDTAK

        utførSteg(
            ident = ansvarligBeslutter,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagGodkjennVedtakVurdering(),
        )
        dvhHendelser.size shouldBe 7
        dvhHendelser[5].ansvarligSaksbehandler shouldBe ansvarligSaksbehandler
        dvhHendelser[5].ansvarligBeslutter shouldBe ansvarligBeslutter
        dvhHendelser[5].ansvarligEnhet shouldBe "Ukjent"
        dvhHendelser[5].behandlingsstatus shouldBe Behandlingsstatus.IVERKSETTER_VEDTAK

        dvhHendelser[6].ansvarligSaksbehandler shouldBe ansvarligSaksbehandler
        dvhHendelser[6].ansvarligBeslutter shouldBe ansvarligBeslutter
        dvhHendelser[6].ansvarligEnhet shouldBe "Ukjent"
        dvhHendelser[6].behandlingsstatus shouldBe Behandlingsstatus.AVSLUTTET
    }
}
