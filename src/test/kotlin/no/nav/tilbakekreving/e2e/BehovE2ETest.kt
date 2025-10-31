package no.nav.tilbakekreving.e2e

import io.kotest.inspectors.forExactly
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.data.Testdata
import no.nav.tilbakekreving.api.v2.MottakerDto
import no.nav.tilbakekreving.api.v2.PeriodeDto
import no.nav.tilbakekreving.api.v2.fagsystem.behov.FagsysteminfoBehovHendelse
import no.nav.tilbakekreving.api.v2.fagsystem.svar.FagsysteminfoSvarHendelse
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.NyKlassekode
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsbeløp
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.entity.FieldConverter
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.hendelser.PåminnelseMediator
import no.nav.tilbakekreving.integrasjoner.KafkaProducerStub
import no.nav.tilbakekreving.integrasjoner.KafkaProducerStub.Companion.finnKafkamelding
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.query
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class BehovE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var påminnelseMediator: PåminnelseMediator

    @Autowired
    private lateinit var kafkaProducer: KafkaProducerStub

    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Test
    fun `sender behov om fagsysteminfo på kafka`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        val fagsystemBehandling = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
                referanse = fagsystemBehandling,
            ),
        )

        val hendelser = kafkaProducer.finnKafkamelding(fagsystemId, FagsysteminfoBehovHendelse.METADATA)
        hendelser.size shouldBe 1

        hendelser[0].eksternFagsakId shouldBe fagsystemId
        hendelser[0].kravgrunnlagReferanse shouldBe fagsystemBehandling
    }

    @Test
    fun `leser svar på behov fra kafka`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        val fagsystemBehandling = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
                referanse = fagsystemBehandling,
                perioder = listOf(
                    KravgrunnlagGenerator.Tilbakekrevingsperiode(
                        periode = 1.januar(2021) til 1.januar(2021),
                        beløpSkattMnd = 1000.kroner,
                        tilbakekrevingsbeløp = listOf(
                            Tilbakekrevingsbeløp.forKlassekode(
                                klassekode = NyKlassekode.TSTBASISP4_OP,
                                beløpTilbakekreves = 2000.kroner,
                                beløpOpprinneligUtbetalt = 20000.kroner,
                            ),
                        ),
                    ),
                ),
            ),
        )

        fagsystemIntegrasjonService.håndter(
            ytelse = Ytelse.Tilleggsstønad,
            fagsysteminfo = FagsysteminfoSvarHendelse(
                eksternFagsakId = fagsystemId,
                hendelseOpprettet = LocalDateTime.now(),
                mottaker = MottakerDto(
                    Testdata.STANDARD_BRUKERIDENT,
                    MottakerDto.MottakerType.PERSON,
                ),
                revurdering = FagsysteminfoSvarHendelse.RevurderingDto(
                    behandlingId = UUID.randomUUID().toString(),
                    årsak = FagsysteminfoSvarHendelse.RevurderingDto.Årsak.NYE_OPPLYSNINGER,
                    årsakTilFeilutbetaling = "Heisann",
                    vedtaksdato = LocalDate.now(),
                ),
                utvidPerioder = listOf(
                    FagsysteminfoSvarHendelse.UtvidetPeriodeDto(
                        kravgrunnlagPeriode = PeriodeDto(1.januar(2021), 1.januar(2021)),
                        vedtaksperiode = PeriodeDto(1.januar(2021), 31.januar(2021)),
                    ),
                ),
            ),
        )
    }

    @Test
    fun `sender ut nytt behov ved påminnelse`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        val fødselsnummer = "feil${KravgrunnlagGenerator.nextPaddedId(7)}"
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(fagsystemId = fagsystemId, fødselsnummer = fødselsnummer),
        )

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
}
