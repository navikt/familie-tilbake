package no.nav.tilbakekreving.e2e

import io.kotest.inspectors.forExactly
import io.kotest.inspectors.forNone
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.entity.FieldConverter
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.fagsystem.events.BehandlingEndretEventDto
import no.nav.tilbakekreving.fagsystem.events.BehandlingsstatusEventDto
import no.nav.tilbakekreving.hendelser.PåminnelseMediator
import no.nav.tilbakekreving.integrasjoner.KafkaProducerStub
import no.nav.tilbakekreving.integrasjoner.KafkaProducerStub.Companion.finnHendelse
import no.nav.tilbakekreving.integrasjoner.KafkaProducerStub.Companion.vedHendelse
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.repository.TilbakekrevingFilter
import no.nav.tilbakekreving.repository.TilbakekrevingRepository
import no.nav.tilbakekreving.test.FellesTestdata.BESLUTTER_IDENT
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.query
import java.time.LocalDateTime

class PåminnelseE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Autowired
    private lateinit var tilbakekrevingRepository: TilbakekrevingRepository

    @Autowired
    private lateinit var påminnelseMediator: PåminnelseMediator

    @Autowired
    private lateinit var kafkaProducer: KafkaProducerStub

    @Test
    fun `sender ut nytt behov ved påminnelse`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        val fødselsnummer = "feil${KravgrunnlagGenerator.nextPaddedId(7)}"
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(fagsystemId = fagsystemId, fødselsnummer = fødselsnummer),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))

        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val tilbakekrevingId = tilbakekreving(behandlingId).id

        pdlClient.hentPersoninfoHits.forExactly(1) {
            it.fagsystem shouldBe FagsystemDTO.TS
            it.ident shouldBe fødselsnummer
        }

        påminnelseMediator.påminnSaker()

        pdlClient.hentPersoninfoHits.forExactly(1) {
            it.fagsystem shouldBe FagsystemDTO.TS
            it.ident shouldBe fødselsnummer
        }

        val nestePåminnelse = jdbcTemplate.query("SELECT neste_påminnelse FROM tilbakekreving WHERE id=?;", tilbakekrevingId) { resultSet, _ ->
            FieldConverter.LocalDateTimeConverter.convert(resultSet, "neste_påminnelse")
        }.singleOrNull().shouldNotBeNull()
        nestePåminnelse shouldBeGreaterThan LocalDateTime.now().plusMinutes(59)

        jdbcTemplate.update(
            "UPDATE tilbakekreving SET neste_påminnelse=? WHERE id=?;",
            FieldConverter.LocalDateTimeConverter.convert(LocalDateTime.now().minusSeconds(1)),
            tilbakekrevingId,
        )

        påminnelseMediator.påminnSaker()

        pdlClient.hentPersoninfoHits.forExactly(2) {
            it.fagsystem shouldBe FagsystemDTO.TS
            it.ident shouldBe fødselsnummer
        }

        påminnelseMediator.påminnSaker()

        pdlClient.hentPersoninfoHits.forExactly(2) {
            it.fagsystem shouldBe FagsystemDTO.TS
            it.ident shouldBe fødselsnummer
        }
    }

    @Test
    fun `perioder som er avsluttet skal ikke bli påminnet`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId, utvidPerioder = emptyList()))

        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        lagreUttalelse(behandlingId)

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }

        utførSteg(
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(1.januar(2021) til 1.januar(2021)),
        )

        utførSteg(
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingFullTilbakekreving(1.januar(2021) til 1.januar(2021)),
        )

        utførSteg(behandlingId, BehandlingsstegGenerator.lagForeslåVedtakVurdering())

        utførSteg(
            ident = BESLUTTER_IDENT,
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagGodkjennVedtakVurdering(),
        )

        val tilbakekreving = tilbakekreving(behandlingId)
        tilbakekreving.tilEntity().nåværendeTilstand shouldBe TilbakekrevingTilstand.AVSLUTTET
        tilbakekrevingRepository.hentTilbakekrevinger(TilbakekrevingFilter.trengerPåminnelse())
            .forNone {
                it.id shouldBe tilbakekreving.id
            }
    }

    @Test
    fun `sender ikke flere påminnelser for tilstander som ikke feiler`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(fagsystemId = fagsystemId),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))

        val behandlingId = behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val tilbakekrevingId = tilbakekreving(behandlingId).id

        kafkaProducer.finnHendelse<BehandlingEndretEventDto>(fagsystemId)
            .forExactly(1) {
                it.tilbakekreving.behandlingsstatus shouldBe BehandlingsstatusEventDto.TIL_BEHANDLING
            }

        jdbcTemplate.update(
            "UPDATE tilbakekreving SET neste_påminnelse=? WHERE id=?;",
            FieldConverter.LocalDateTimeConverter.convert(LocalDateTime.now().minusSeconds(1)),
            tilbakekrevingId,
        )

        påminnelseMediator.påminnSaker()
        påminnelseMediator.påminnSaker()

        kafkaProducer.finnHendelse<BehandlingEndretEventDto>(fagsystemId)
            .forExactly(2) {
                it.tilbakekreving.behandlingsstatus shouldBe BehandlingsstatusEventDto.TIL_BEHANDLING
            }
    }

    @Test
    fun `påminnelser som feiler skal ikke blokkere andre saker`() {
        val fagsystemId1 = KravgrunnlagGenerator.nextPaddedId(6)
        val fagsystemId2 = KravgrunnlagGenerator.nextPaddedId(6)

        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(fagsystemId = fagsystemId1),
        )
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(fagsystemId = fagsystemId2),
        )

        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId1))
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId2))

        kafkaProducer
            .finnHendelse<BehandlingEndretEventDto>(fagsystemId1)
            .filter { it.tilbakekreving.behandlingsstatus == BehandlingsstatusEventDto.TIL_BEHANDLING }
            .shouldHaveSize(1)
        kafkaProducer
            .finnHendelse<BehandlingEndretEventDto>(fagsystemId2)
            .filter { it.tilbakekreving.behandlingsstatus == BehandlingsstatusEventDto.TIL_BEHANDLING }
            .shouldHaveSize(1)

        jdbcTemplate.update(
            "UPDATE tilbakekreving SET neste_påminnelse=? WHERE id IN(?, ?);",
            FieldConverter.LocalDateTimeConverter.convert(LocalDateTime.now().minusSeconds(1)),
            tilbakekreving(FagsystemDTO.TS, fagsystemId1).shouldNotBeNull().id,
            tilbakekreving(FagsystemDTO.TS, fagsystemId2).shouldNotBeNull().id,
        )

        kafkaProducer.vedHendelse<BehandlingEndretEventDto>(fagsystemId1) { error("Tvungen feil ved test") }
        påminnelseMediator.påminnSaker()

        kafkaProducer
            .finnHendelse<BehandlingEndretEventDto>(fagsystemId1)
            .filter { it.tilbakekreving.behandlingsstatus == BehandlingsstatusEventDto.TIL_BEHANDLING }
            .shouldHaveSize(1)
        kafkaProducer
            .finnHendelse<BehandlingEndretEventDto>(fagsystemId2)
            .filter { it.tilbakekreving.behandlingsstatus == BehandlingsstatusEventDto.TIL_BEHANDLING }
            .shouldHaveSize(2)
    }
}
