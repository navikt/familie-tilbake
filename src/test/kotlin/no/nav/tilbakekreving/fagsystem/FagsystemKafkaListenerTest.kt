package no.nav.tilbakekreving.fagsystem

import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.data.Testdata
import no.nav.tilbakekreving.api.v2.MottakerDto
import no.nav.tilbakekreving.api.v2.PeriodeDto
import no.nav.tilbakekreving.api.v2.fagsystem.svar.FagsysteminfoSvarHendelse
import no.nav.tilbakekreving.januar
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class FagsystemKafkaListenerTest {
    val oppsamler = FagsystemIntegrasjonServiceMock()
    val listener = FagsystemKafkaListener(oppsamler)

    @Test
    fun `dekoder svar på fagsysteminfo behov`() {
        listener.håndterMelding(
            ytelse = Ytelse.Tilleggsstønad,
            melding = """
                        {
                          "hendelsestype": "fagsysteminfo_svar",
                          "versjon": 1,
                          "eksternFagsakId": "123456",
                          "hendelseOpprettet": "2025-01-13T12:30:45.00000",
                          "mottaker": {
                            "type": "PERSON",
                            "ident": "${Testdata.STANDARD_BRUKERIDENT}"
                          },
                          "revurdering": {
                            "behandlingId": "654321",
                            "årsak": "NYE_OPPLYSNINGER",
                            "årsakTilFeilutbetaling": "Bruker sluttet på tiltaket",
                            "vedtaksdato": "2025-01-12",
                            "utvidPerioder": [
                              {
                                "kravgrunnlagPeriode": {
                                  "fom": "2023-01-01",
                                  "tom": "2023-01-01"
                                },
                                "vedtaksperiode": {
                                  "fom": "2023-01-01",
                                  "tom": "2023-01-31"
                                }
                              }
                            ]
                          }
                        }
            """.trimIndent(),
        )

        oppsamler.finnHendelser("123456") shouldBe listOf(
            FagsysteminfoSvarHendelse(
                eksternFagsakId = "123456",
                hendelseOpprettet = LocalDateTime.of(2025, 1, 13, 12, 30, 45, 0),
                mottaker = MottakerDto(
                    ident = Testdata.STANDARD_BRUKERIDENT,
                    type = MottakerDto.MottakerType.PERSON,
                ),
                revurdering = FagsysteminfoSvarHendelse.RevurderingDto(
                    behandlingId = "654321",
                    årsak = FagsysteminfoSvarHendelse.RevurderingDto.Årsak.NYE_OPPLYSNINGER,
                    årsakTilFeilutbetaling = "Bruker sluttet på tiltaket",
                    vedtaksdato = LocalDate.of(2025, 1, 12),
                    utvidPerioder = listOf(
                        FagsysteminfoSvarHendelse.UtvidetPeriodeDto(
                            kravgrunnlagPeriode = PeriodeDto(1.januar(2023), 1.januar(2023)),
                            vedtaksperiode = PeriodeDto(1.januar(2023), 31.januar(2023)),
                        ),
                    ),
                ),
            ),
        )
    }
}
