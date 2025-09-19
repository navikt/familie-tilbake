package no.nav.tilbakekreving.e2e

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.familie.tilbake.data.Testdata
import no.nav.tilbakekreving.api.v2.MottakerDto
import no.nav.tilbakekreving.api.v2.PeriodeDto
import no.nav.tilbakekreving.api.v2.fagsystem.behov.FagsysteminfoBehovHendelse
import no.nav.tilbakekreving.api.v2.fagsystem.svar.FagsysteminfoSvarHendelse
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.NyKlassekode
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsbeløp
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.integrasjoner.KafkaProducerStub
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class BehovE2ETest : TilbakekrevingE2EBase() {
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

        val hendelser = kafkaProducer.finnKafkamelding(fagsystemId)
        hendelser.size shouldBe 1
        val fagsysteminfoBehov = hendelser.single { it.eksternFagsakId == fagsystemId }
            .shouldBeInstanceOf<FagsysteminfoBehovHendelse>()

        fagsysteminfoBehov.eksternFagsakId shouldBe fagsystemId
        fagsysteminfoBehov.kravgrunnlagReferanse shouldBe fagsystemBehandling
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
                    utvidPerioder = listOf(
                        FagsysteminfoSvarHendelse.UtvidetPeriodeDto(
                            kravgrunnlagPeriode = PeriodeDto(1.januar(2021), 1.januar(2021)),
                            vedtaksperiode = PeriodeDto(1.januar(2021), 31.januar(2021)),
                        ),
                    ),
                ),
            ),
        )
    }
}
