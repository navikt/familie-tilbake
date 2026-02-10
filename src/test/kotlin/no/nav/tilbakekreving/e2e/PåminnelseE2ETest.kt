package no.nav.tilbakekreving.e2e

import io.kotest.inspectors.forExactly
import io.kotest.inspectors.forNone
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.api.v2.fagsystem.BehandlingEndretHendelse
import no.nav.tilbakekreving.api.v2.fagsystem.ForenkletBehandlingsstatus
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.entity.FieldConverter
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.hendelser.PåminnelseMediator
import no.nav.tilbakekreving.integrasjoner.KafkaProducerStub
import no.nav.tilbakekreving.integrasjoner.KafkaProducerStub.Companion.finnKafkamelding
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.repository.TilbakekrevingRepository
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

        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
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

        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        lagreUttalelse(behandlingId)

        somSaksbehandler("Z999999") {
            behandlingApiController.behandlingOppdaterFakta(
                behandlingId = behandlingId.toString(),
                oppdaterFaktaOmFeilutbetalingDto = BehandlingsstegGenerator.lagFaktastegVurderingFritekst(allePeriodeIder(behandlingId)),
            )
        }

        utførSteg(
            ident = "Z999999",
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagIkkeForeldetVurdering(1.januar(2021) til 1.januar(2021)),
        )

        utførSteg(
            ident = "Z999999",
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagVilkårsvurderingFullTilbakekreving(1.januar(2021) til 1.januar(2021)),
        )

        utførSteg(
            ident = "Z999999",
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagForeslåVedtakVurdering(),
        )

        utførSteg(
            ident = "Z111111",
            behandlingId = behandlingId,
            stegData = BehandlingsstegGenerator.lagGodkjennVedtakVurdering(),
        )

        val tilbakekreving = tilbakekreving(behandlingId)
        tilbakekreving.tilEntity().nåværendeTilstand shouldBe TilbakekrevingTilstand.AVSLUTTET
        tilbakekrevingRepository.hentTilbakekrevinger(TilbakekrevingRepository.FindTilbakekrevingStrategy.TrengerPåminnelse)
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

        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
        val tilbakekrevingId = tilbakekreving(behandlingId).id

        kafkaProducer.finnKafkamelding(fagsystemId, BehandlingEndretHendelse.METADATA)
            .forExactly(1) {
                it.tilbakekreving.behandlingsstatus shouldBe ForenkletBehandlingsstatus.TIL_BEHANDLING
            }

        jdbcTemplate.update(
            "UPDATE tilbakekreving SET neste_påminnelse=? WHERE id=?;",
            FieldConverter.LocalDateTimeConverter.convert(LocalDateTime.now().minusSeconds(1)),
            tilbakekrevingId,
        )

        påminnelseMediator.påminnSaker()
        påminnelseMediator.påminnSaker()

        kafkaProducer.finnKafkamelding(fagsystemId, BehandlingEndretHendelse.METADATA)
            .forExactly(2) {
                it.tilbakekreving.behandlingsstatus shouldBe ForenkletBehandlingsstatus.TIL_BEHANDLING
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
            .finnKafkamelding(fagsystemId1, BehandlingEndretHendelse.METADATA)
            .filter { it.tilbakekreving.behandlingsstatus == ForenkletBehandlingsstatus.TIL_BEHANDLING }
            .shouldHaveSize(1)
        kafkaProducer
            .finnKafkamelding(fagsystemId2, BehandlingEndretHendelse.METADATA)
            .filter { it.tilbakekreving.behandlingsstatus == ForenkletBehandlingsstatus.TIL_BEHANDLING }
            .shouldHaveSize(1)

        jdbcTemplate.update(
            "UPDATE tilbakekreving SET neste_påminnelse=? WHERE id IN(?, ?);",
            FieldConverter.LocalDateTimeConverter.convert(LocalDateTime.now().minusSeconds(1)),
            tilbakekreving(FagsystemDTO.TS, fagsystemId1).shouldNotBeNull().id,
            tilbakekreving(FagsystemDTO.TS, fagsystemId2).shouldNotBeNull().id,
        )

        kafkaProducer.vedMelding(BehandlingEndretHendelse.METADATA, fagsystemId1) { error("Tvungen feil ved test") }
        påminnelseMediator.påminnSaker()

        kafkaProducer
            .finnKafkamelding(fagsystemId1, BehandlingEndretHendelse.METADATA)
            .filter { it.tilbakekreving.behandlingsstatus == ForenkletBehandlingsstatus.TIL_BEHANDLING }
            .shouldHaveSize(1)
        kafkaProducer
            .finnKafkamelding(fagsystemId2, BehandlingEndretHendelse.METADATA)
            .filter { it.tilbakekreving.behandlingsstatus == ForenkletBehandlingsstatus.TIL_BEHANDLING }
            .shouldHaveSize(2)
    }
}
