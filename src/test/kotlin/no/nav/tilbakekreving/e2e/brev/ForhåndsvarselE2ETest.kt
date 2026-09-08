package no.nav.tilbakekreving.e2e.brev

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
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
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselErSendtDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselUnntakDto
import no.nav.tilbakekreving.kontrakter.frontend.models.IkkeVurdertDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SendForhaandsvarselDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UnntakDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UpdateUttalelsesfristDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelseVurderingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VarslingsunntakDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.Period
import java.util.UUID

class ForhåndsvarselE2ETest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Autowired
    private lateinit var documentController: DokumentController

    @Test
    fun `forhåndsvarsel sendes og lagres riktig i DB`() {
        val behandlingId = hentBehandlingId()
        val tilbakekrevingFørForhåndsvarsel = tilbakekreving(behandlingId)
        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            documentController.bestillBrev(
                BestillBrevDto(
                    behandlingId = behandlingId,
                    brevmalkode = Dokumentmalstype.VARSEL,
                    fritekst = "Tekst fra saksbehandler",
                ),
            )
        }
        val tilbakekrevingEtterForhåndsvarsel = tilbakekreving(behandlingId)

        tilbakekrevingFørForhåndsvarsel.brevHistorikk.sisteVarselbrev().shouldBeNull()
        tilbakekrevingEtterForhåndsvarsel.brevHistorikk.sisteVarselbrev().shouldNotBeNull {
            tilForhåndsvarselDto().tekstFraSaksbehandler shouldBe "Tekst fra saksbehandler"
        }
    }

    private fun hentBehandlingId(): UUID {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))
        return behandlingIdFor(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
    }

    @Test
    fun `sende forhåndsvarsel skal oppdatere varselbrevet i brevhistorikk med tid og journlaførtId`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()
        tilbakekreving.brevHistorikk.sisteVarselbrev() shouldBe null

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingSendVarselbrev(tilbakekreving.nåværendeBehandlingId(), SendForhaandsvarselDto("Tekst fra saksbehandler"))
        }
        val tilbakekrevingEtterVarselbrev = tilbakekreving(FagsystemDTO.TS, tilbakekreving.eksternFagsak.eksternId)
        tilbakekrevingEtterVarselbrev!!.brevHistorikk.sisteVarselbrev() shouldNotBeNull {
            journalpostId shouldBe "-1"
            tilForhåndsvarselDto().tekstFraSaksbehandler shouldBe "Tekst fra saksbehandler"
        }
    }

    @Test
    fun `forhåndsvarsel response er IKKE_STARTET når forhåndsvarselsteget ikke er behandlet`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            val forhåndsvarselResponse = behandlingApiController.behandlingForhandsvarsel(tilbakekreving.nåværendeBehandlingId()).body

            forhåndsvarselResponse!!.forhaandsvarselSteg.shouldBeInstanceOf<IkkeVurdertDto>()
        }
    }

    @Test
    fun `forhåndsvarsel response er ForhaandsvarselErSendtDto når forhåndsvarsel er sendt`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingSendVarselbrev(tilbakekreving.nåværendeBehandlingId(), SendForhaandsvarselDto("Tekst fra saksbehandler"))

            val forhåndsvarselResponse = behandlingApiController.behandlingForhandsvarsel(tilbakekreving.nåværendeBehandlingId()).body
            forhåndsvarselResponse.shouldNotBeNull().forhaandsvarselSteg.shouldBeInstanceOf<ForhaandsvarselErSendtDto> {
                it.forhåndsvarselInfo.tekstFraSaksbehandler shouldBe "Tekst fra saksbehandler"
            }
        }
    }

    @Test
    fun `brukeruttalelse er IKKE_STARTET når forhåndsvarsel er sendt men uttalelse ikke er behandlet`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingSendVarselbrev(tilbakekreving.nåværendeBehandlingId(), SendForhaandsvarselDto("Tekst fra saksbehandler"))

            val forhåndsvarselResponse = behandlingApiController.behandlingForhandsvarsel(tilbakekreving.nåværendeBehandlingId()).body
            forhåndsvarselResponse.shouldNotBeNull().forhaandsvarselSteg.shouldBeInstanceOf<ForhaandsvarselErSendtDto> {
                it.uttalelsesfrist.opprinneligFrist shouldBe LocalDate.now().plus(Period.ofWeeks(3))
                it.uttalelsesfrist.nyFrist shouldBe null
                it.uttalelsesfrist.begrunnelse shouldBe null
            }
            forhåndsvarselResponse.brukeruttalelse!!.harBrukerUttaltSeg shouldBe UttalelseVurderingDto.IKKE_VURDERT
        }
    }

    @Test
    fun `brukeruttalelse lagres`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingSendVarselbrev(tilbakekreving.nåværendeBehandlingId(), SendForhaandsvarselDto("Tekst fra saksbehandler"))

            behandlingApiController.behandlingLagreBrukersuttalelse(
                behandlingId = tilbakekreving.nåværendeBehandlingId(),
                uttalelseDto = UttalelseDto(
                    harBrukerUttaltSeg = UttalelseVurderingDto.NEI_ETTER_FORHÅNDSVARSEL,
                    beskrivelse = "Gadd ikke si noe",
                ),
            )

            val forhåndsvarselResponse = behandlingApiController.behandlingForhandsvarsel(tilbakekreving.nåværendeBehandlingId()).body.shouldNotBeNull()
            forhåndsvarselResponse.forhaandsvarselSteg.shouldBeInstanceOf<ForhaandsvarselErSendtDto>()
            forhåndsvarselResponse.brukeruttalelse.shouldNotBeNull {
                harBrukerUttaltSeg shouldBe UttalelseVurderingDto.NEI_ETTER_FORHÅNDSVARSEL
                beskrivelse shouldBe "Gadd ikke si noe"
            }
        }
    }

    @Test
    fun `unntak uten uttalelse lagres`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingLagreForhaandsvarselUnntak(
                tilbakekreving.nåværendeBehandlingId(),
                unntakDto = UnntakDto(
                    begrunnelseForUnntak = VarslingsunntakDto.IKKE_PRAKTISK_MULIG,
                    beskrivelse = "Ikke mulig å forhåndsvarsle",
                ),
            )
            behandlingApiController.behandlingForhandsvarsel(tilbakekreving.nåværendeBehandlingId()).body.shouldNotBeNull()
                .forhaandsvarselSteg.shouldBeInstanceOf<ForhaandsvarselUnntakDto> {
                    it.begrunnelseForUnntak shouldBe VarslingsunntakDto.IKKE_PRAKTISK_MULIG
                    it.beskrivelse shouldBe "Ikke mulig å forhåndsvarsle"
                }
        }
    }

    @Test
    fun `unntak med uttalelse lagres`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingLagreForhaandsvarselUnntak(
                tilbakekreving.nåværendeBehandlingId(),
                unntakDto = UnntakDto(
                    begrunnelseForUnntak = VarslingsunntakDto.ÅPENBART_UNØDVENDIG,
                    beskrivelse = "Allerede uttalet seg",
                ),
            )

            behandlingApiController.behandlingLagreBrukersuttalelse(
                tilbakekreving.nåværendeBehandlingId(),
                UttalelseDto(
                    harBrukerUttaltSeg = UttalelseVurderingDto.UNNTAK_ALLEREDE_UTTALT_SEG,
                    uttalelsesdato = LocalDate.of(2021, 1, 1),
                    hvorBrukerenUttalteSeg = "Reddit",
                    beskrivelse = "Typisk reddit kommentar",
                ),
            )

            behandlingApiController.behandlingForhandsvarsel(tilbakekreving.nåværendeBehandlingId()).body.shouldNotBeNull {
                forhaandsvarselSteg.shouldBeInstanceOf<ForhaandsvarselUnntakDto> {
                    it.begrunnelseForUnntak shouldBe VarslingsunntakDto.ÅPENBART_UNØDVENDIG
                    it.beskrivelse shouldBe "Allerede uttalet seg"
                }
                brukeruttalelse.shouldNotBeNull {
                    harBrukerUttaltSeg shouldBe UttalelseVurderingDto.UNNTAK_ALLEREDE_UTTALT_SEG
                }
            }
        }
    }

    @Test
    fun `uttalelsesfrist lagres,`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()

        somSaksbehandler(SAKSBEHANDLER_IDENT) {
            behandlingApiController.behandlingSendVarselbrev(tilbakekreving.nåværendeBehandlingId(), SendForhaandsvarselDto("Tekst fra saksbehandler"))
            behandlingApiController.behandlingUtsettUttalelsesfrist(tilbakekreving.nåværendeBehandlingId(), UpdateUttalelsesfristDto(LocalDate.of(2027, 1, 1), "begrunnelse"))
            val response = behandlingApiController.behandlingForhandsvarsel(tilbakekreving.nåværendeBehandlingId()).body.shouldNotBeNull()
            response.forhaandsvarselSteg.shouldBeInstanceOf<ForhaandsvarselErSendtDto> {
                it.uttalelsesfrist.shouldNotBeNull()
                it.uttalelsesfrist.opprinneligFrist shouldBe LocalDate.now().plus(Period.ofWeeks(3))
                it.uttalelsesfrist.nyFrist shouldBe LocalDate.of(2027, 1, 1)
            }
            response.brukeruttalelse!!.harBrukerUttaltSeg shouldBe UttalelseVurderingDto.IKKE_VURDERT
            response.brukeruttalelse!!.harBrukerUttaltSeg shouldBe UttalelseVurderingDto.IKKE_VURDERT
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
