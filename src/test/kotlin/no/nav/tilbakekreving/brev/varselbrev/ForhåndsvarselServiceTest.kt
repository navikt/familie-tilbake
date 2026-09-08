package no.nav.tilbakekreving.brev.varselbrev

import io.kotest.inspectors.forOne
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.familie.tilbake.api.DokumentController
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v1.dto.BestillBrevDto
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsbeløp.Companion.medFeilutbetaling
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.brev.Dokumentmalstype
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ForhåndsvarselServiceTest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Autowired
    protected lateinit var dokumentController: DokumentController

    @BeforeEach
    fun cleanup() {
        jdbcTemplate.update("DELETE FROM tilbakekreving_uttalelse_informasjon")
        jdbcTemplate.update("DELETE FROM tilbakekreving_forhåndsvarsel_unntak")
        jdbcTemplate.update("DELETE FROM tilbakekreving_uttalelsesfrist")
        jdbcTemplate.update("DELETE FROM tilbakekreving_brukeruttalelse")
    }

    @Test
    fun `henter ny varselbrev tekster`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()
        val tekster = somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingHentVarselbrevTekster(tilbakekreving.nåværendeBehandlingId())
        }

        tekster.body.shouldNotBeNull {
            avsnitter.shouldNotBeEmpty()
            overskrift shouldContain "Nav vurderer om du må betale tilbake"
            avsnitter.forOne {
                it.tittel shouldBe ""
                it.body.size shouldBe 2
                it.body[0] shouldContain "Du har fått 2000 kroner for mye utbetalt i tilleggsstønad"
                it.body[1] shouldContain "Dette er et varsel om at vi vurderer om du må betale tilbake beløpet."
            }
            avsnitter.forOne {
                it.tittel shouldBe "Årsak til feilutbetaling"
                it.body.size shouldBe 1
                it.body[0] shouldBe ""
            }
            avsnitter.forOne {
                it.tittel shouldBe "Dette legger vi vekt på i vurderingen vår"
                it.body.size shouldBe 4
                it.body[0] shouldContain "For å avgjøre om vi kan kreve tilbake, tar vi først stilling til"
                it.body[1] shouldContain "Hvis resultatet blir at vi kan kreve tilbake, vurderer vi om du skal betale tilbake hele eller deler av beløpet."
                it.body[2] shouldContain "Hvis du må betale tilbake, og du har gitt oss feil eller mangelfull informasjon,"
                it.body[3] shouldContain "Dette går fram av folketrygdloven §§ 22-15 og 22-17a."
            }
            avsnitter.forOne {
                it.tittel shouldBe "Vår foreløpige vurdering av saken din"
                it.body.size shouldBe 1
                it.body[0] shouldContain "Vi understreker at denne vurderingen ikke er endelig."
            }
            avsnitter.forOne {
                it.tittel shouldBe "Slik uttaler du deg"
                it.body.size shouldBe 1
                it.body[0] shouldContain "Du kan sende uttalelsen din ved å logge deg inn på nav.no/skriv-til-oss og velge «Send beskjed til Nav»."
            }
            avsnitter.forOne {
                it.tittel shouldBe "Du har rett til innsyn"
                it.body.size shouldBe 1
                it.body[0] shouldContain "Du har rett til å se dokumentene i saken din. Dette følger av forvaltningsloven § 18."
            }
            avsnitter.forOne {
                it.tittel shouldBe "Du har rettigheter knyttet til personopplysningene dine"
                it.body.size shouldBe 1
                it.body[0] shouldContain "Du finner informasjon om hvordan Nav behandler personopplysningene dine,"
            }
            avsnitter.forOne {
                it.tittel shouldBe "Har du spørsmål?"
                it.body.size shouldBe 1
                it.body[0] shouldContain "Du finner mer informasjon på nav.no/tilleggsstonader."
            }
        }
    }

    @Test
    fun `sende forhåndsvarsel skal oppdatere varselbrevet i brevhistorikk med tid og journlaførtId`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()
        val bestillBrevDto = BestillBrevDto(
            behandlingId = tilbakekreving.nåværendeBehandlingId(),
            brevmalkode = Dokumentmalstype.VARSEL,
            fritekst = "Tekst fra saksbehandler",
        )

        tilbakekreving.brevHistorikk.sisteVarselbrev() shouldBe null
        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            dokumentController.bestillBrev(bestillBrevDto)
        }

        val tilbakekrevingEtterVarselbrev = tilbakekreving(FagsystemDTO.TS, tilbakekreving.eksternFagsak.eksternId)
        tilbakekrevingEtterVarselbrev!!.brevHistorikk.sisteVarselbrev() shouldNotBeNull {
            journalpostId shouldBe "-1"
            dokumentInfoId shouldBe "-2"
        }
    }

    private fun opprettTilbakekrevingOgHentFagsystemId(): Tilbakekreving {
        val fnr = "12312312311"
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        val vedtakId = KravgrunnlagGenerator.nextPaddedId(6)
        val ansvarligEnhet = KravgrunnlagGenerator.nextPaddedId(4)
        sendKravgrunnlagOgAvventLesing(
            TILLEGGSSTØNADER_KØ_NAVN,
            KravgrunnlagGenerator.forTilleggsstønader(
                fødselsnummer = fnr,
                fagsystemId = fagsystemId,
                vedtakId = vedtakId,
                ansvarligEnhet = ansvarligEnhet,
                perioder = listOf(
                    KravgrunnlagGenerator.Tilbakekrevingsperiode(
                        1.januar(2021) til 1.januar(2021),
                        tilbakekrevingsbeløp = listOf(
                            KravgrunnlagGenerator.Tilbakekrevingsbeløp.forKlassekode(
                                klassekode = KravgrunnlagGenerator.NyKlassekode.TSTBASISP4_OP,
                                beløpTilbakekreves = 2000.kroner,
                                beløpOpprinneligUtbetalt = 20000.kroner,
                            ),
                        ).medFeilutbetaling(KravgrunnlagGenerator.NyKlassekode.KL_KODE_FEIL_ARBYT),
                    ),
                ),
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))
        return tilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
    }
}
