package no.nav.tilbakekreving.e2e

import io.kotest.inspectors.forOne
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.behandlingslogg.Behandlingsloggstype
import no.nav.tilbakekreving.brev.Informasjonsbrev
import no.nav.tilbakekreving.brev.Informasjonsbrevtype
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.fagsystem.events.BehandlingEndretEventDto
import no.nav.tilbakekreving.fagsystem.events.BehandlingsstatusEventDto
import no.nav.tilbakekreving.integrasjoner.KafkaProducerStub
import no.nav.tilbakekreving.integrasjoner.KafkaProducerStub.Companion.finnHendelse
import no.nav.tilbakekreving.kontrakter.frontend.models.SendForhaandsvarselDto
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.repository.NyBehandlingsloggRepository
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource

@TestPropertySource(
    properties = ["tilbakekreving.toggles.nyModell.KravBortfalt=true"],
)
class InformasjonsbrevBortfaltKravE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    lateinit var behandlingsloggRepository: NyBehandlingsloggRepository

    @Autowired
    lateinit var kafkaProducer: KafkaProducerStub

    @Test
    fun `bortfalt krav etter sendt forhåndsvarsel sender informasjonsbrev og avslutter saken`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(KravgrunnlagGenerator.forTilleggsstønader(fagsystemId = fagsystemId))
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))
        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingSendVarselbrev(behandlingId, SendForhaandsvarselDto("Tekst fra saksbehandler"))
        }

        sendStatusmelding(statusmelding = StatusmeldingGenerator.forFagområdet(fagsystemId = fagsystemId, kodeStatusKrav = "AVSL", fagområde = "TILLST"))

        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(TilbakekrevingFilter.behandling(behandlingId), validerScope = false).shouldNotBeNull()
        tilbakekreving.tilEntity().nåværendeTilstand shouldBe TilbakekrevingTilstand.AVSLUTTET_UTEN_VEDTAK
        tilbakekreving.brevHistorikk.nåværende().entry.shouldBeInstanceOf<Informasjonsbrev> {
            it.type shouldBe Informasjonsbrevtype.BORTFALT_KRAV
            it.journalpostId shouldBe "-1"
            it.dokumentInfoId shouldBe "-2"
        }
        val behandlingslogg = behandlingsloggRepository.hentBehandlingslogg(tilbakekreving.id)
        behandlingslogg.forOne { it.behandlingsloggstype shouldBe Behandlingsloggstype.INFORMASJONSBREV_JOURNALFØRT }
        behandlingslogg.forOne { it.behandlingsloggstype shouldBe Behandlingsloggstype.INFORMASJONSBREV_SENDT }

        val behandlingEndret = kafkaProducer.finnHendelse<BehandlingEndretEventDto>(fagsystemId).lastOrNull().shouldNotBeNull()
        behandlingEndret.tilbakekreving.behandlingsstatus shouldBe BehandlingsstatusEventDto.AVSLUTTET
    }
}
