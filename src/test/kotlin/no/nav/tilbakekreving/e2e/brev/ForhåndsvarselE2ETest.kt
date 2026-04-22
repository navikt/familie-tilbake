package no.nav.tilbakekreving.e2e.brev

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.familie.tilbake.api.DokumentController
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.api.v1.dto.BestillBrevDto
import no.nav.tilbakekreving.api.v1.dto.BrukeruttalelseDto
import no.nav.tilbakekreving.api.v1.dto.ForhåndsvarselUnntakDto
import no.nav.tilbakekreving.api.v1.dto.HarBrukerUttaltSeg
import no.nav.tilbakekreving.api.v1.dto.Uttalelsesdetaljer
import no.nav.tilbakekreving.api.v1.dto.VarslingsUnntak
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
import no.nav.tilbakekreving.kontrakter.frontend.models.UpdateUttalelsesfristDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelseVurderingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VarslingsUnntakDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.BeforeEach
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

    @BeforeEach
    fun cleanup() {
        jdbcTemplate.update("DELETE FROM tilbakekreving_uttalelse_informasjon")
        jdbcTemplate.update("DELETE FROM tilbakekreving_forhåndsvarsel_unntak")
        jdbcTemplate.update("DELETE FROM tilbakekreving_uttalelsesfrist")
        jdbcTemplate.update("DELETE FROM tilbakekreving_brukeruttalelse")
    }

    @Test
    fun `forhåndsvarsel sendes og lagres riktig i DB`() {
        val behandlingId = hentBehandlingId()
        val tilbakekrevingFørForhåndsvarsel = tilbakekreving(behandlingId)
        documentController.bestillBrev(
            BestillBrevDto(
                behandlingId = behandlingId,
                brevmalkode = Dokumentmalstype.VARSEL,
                fritekst = "Tekst fra saksbehandler",
            ),
        )
        val tilbakekrevingEtterForhåndsvarsel = tilbakekreving(behandlingId)

        tilbakekrevingFørForhåndsvarsel.brevHistorikk.sisteVarselbrev().shouldBeNull()
        tilbakekrevingEtterForhåndsvarsel.brevHistorikk.sisteVarselbrev().shouldNotBeNull {
            tekstFraSaksbehandler shouldBe "Tekst fra saksbehandler"
        }
    }

    @Test
    fun `uttalelse kan redigeres`() {
        val behandlingId = hentBehandlingId()
        somSaksbehandler("Z999999") {
            documentController.lagreBrukeruttalelse(behandlingId, hentBrukerUttalelseDto(HarBrukerUttaltSeg.JA_ETTER_FORHÅNDSVARSEL, LocalDate.of(2026, 1, 2)))
        }

        documentController.hentForhåndsvarselinfo(behandlingId).data.shouldNotBeNull {
            brukeruttalelse.shouldNotBeNull {
                harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.JA_ETTER_FORHÅNDSVARSEL
                uttalelsesdetaljer.shouldNotBeNull {
                    size shouldBe 1
                    get(0).uttalelsesdato shouldBe LocalDate.of(2026, 1, 2)
                }
            }
        }

        somSaksbehandler("Z999999") {
            documentController.lagreBrukeruttalelse(behandlingId, hentBrukerUttalelseDto(HarBrukerUttaltSeg.JA_ETTER_FORHÅNDSVARSEL, LocalDate.of(2025, 12, 2)))
        }
        documentController.hentForhåndsvarselinfo(behandlingId).data.shouldNotBeNull {
            brukeruttalelse.shouldNotBeNull {
                harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.JA_ETTER_FORHÅNDSVARSEL
                uttalelsesdetaljer.shouldNotBeNull {
                    size shouldBe 1
                    get(0).uttalelsesdato shouldBe LocalDate.of(2025, 12, 2)
                }
            }
        }
    }

    @Test
    fun `unntak kan redigeres`() {
        val behandlingId = hentBehandlingId()
        somSaksbehandler("Z999999") {
            documentController.forhåndsvarselUnntak(
                behandlingId,
                ForhåndsvarselUnntakDto(
                    begrunnelseForUnntak = VarslingsUnntak.IKKE_PRAKTISK_MULIG,
                    beskrivelse = "Ikke mulig",
                ),
            )
        }

        documentController.hentForhåndsvarselinfo(behandlingId).data.shouldNotBeNull {
            forhåndsvarselUnntak.shouldNotBeNull {
                begrunnelseForUnntak shouldBe VarslingsUnntak.IKKE_PRAKTISK_MULIG
                beskrivelse shouldBe "Ikke mulig"
            }
        }
        somSaksbehandler("Z999999") {
            documentController.forhåndsvarselUnntak(
                behandlingId,
                ForhåndsvarselUnntakDto(
                    begrunnelseForUnntak = VarslingsUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING,
                    beskrivelse = "Ukjent adresse",
                ),
            )
        }

        documentController.hentForhåndsvarselinfo(behandlingId).data.shouldNotBeNull {
            forhåndsvarselUnntak.shouldNotBeNull {
                begrunnelseForUnntak shouldBe VarslingsUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING
                beskrivelse shouldBe "Ukjent adresse"
            }
        }
    }

    @Test
    fun `unntak med alternativ ÅPENBART_UNØDVENDIG og registrert uttalelse kan redigeres`() {
        val behandlingId = hentBehandlingId()

        somSaksbehandler("2222222") {
            documentController.forhåndsvarselUnntak(
                behandlingId,
                ForhåndsvarselUnntakDto(
                    begrunnelseForUnntak = VarslingsUnntak.ÅPENBART_UNØDVENDIG,
                    beskrivelse = "allerede uttalet seg",
                ),
            )
        }
        somSaksbehandler("Z999999") {
            documentController.lagreBrukeruttalelse(behandlingId, hentBrukerUttalelseDto(HarBrukerUttaltSeg.UNNTAK_ALLEREDE_UTTALT_SEG, LocalDate.of(2026, 1, 2)))
        }
        documentController.hentForhåndsvarselinfo(behandlingId).data.shouldNotBeNull {
            forhåndsvarselUnntak.shouldNotBeNull {
                begrunnelseForUnntak shouldBe VarslingsUnntak.ÅPENBART_UNØDVENDIG
                beskrivelse shouldBe "allerede uttalet seg"
            }
            brukeruttalelse.shouldNotBeNull {
                harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.UNNTAK_ALLEREDE_UTTALT_SEG
                uttalelsesdetaljer.shouldNotBeNull {
                    get(0).uttalelsesdato shouldBe LocalDate.of(2026, 1, 2)
                }
            }
        }
        somSaksbehandler("Z999999") {
            documentController.lagreBrukeruttalelse(behandlingId, hentBrukerUttalelseDto(HarBrukerUttaltSeg.UNNTAK_ALLEREDE_UTTALT_SEG, LocalDate.of(2025, 12, 2)))
        }
        documentController.hentForhåndsvarselinfo(behandlingId).data.shouldNotBeNull {
            forhåndsvarselUnntak.shouldNotBeNull()
            brukeruttalelse.shouldNotBeNull {
                harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.UNNTAK_ALLEREDE_UTTALT_SEG
                uttalelsesdetaljer.shouldNotBeNull {
                    get(0).uttalelsesdato shouldBe LocalDate.of(2025, 12, 2)
                }
            }
        }

        documentController.bestillBrev(BestillBrevDto(behandlingId, Dokumentmalstype.VARSEL, "TEST"))
        somSaksbehandler("Z999999") {
            documentController.lagreBrukeruttalelse(behandlingId, hentBrukerUttalelseDto(HarBrukerUttaltSeg.JA_ETTER_FORHÅNDSVARSEL, LocalDate.of(2025, 12, 2)))
        }
        documentController.hentForhåndsvarselinfo(behandlingId).data.shouldNotBeNull {
            forhåndsvarselUnntak.shouldBeNull()
            brukeruttalelse.shouldNotBeNull {
                harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.JA_ETTER_FORHÅNDSVARSEL
                uttalelsesdetaljer.shouldNotBeNull {
                    get(0).uttalelsesdato shouldBe LocalDate.of(2025, 12, 2)
                }
            }
        }
        somSaksbehandler("Z999999") {
            documentController.lagreBrukeruttalelse(
                behandlingId,
                BrukeruttalelseDto(
                    harBrukerUttaltSeg = HarBrukerUttaltSeg.NEI_ETTER_FORHÅNDSVARSEL,
                    uttalelsesdetaljer = null,
                    kommentar = "har ikke uttalet seg",
                ),
            )
        }
        documentController.hentForhåndsvarselinfo(behandlingId).data.shouldNotBeNull {
            forhåndsvarselUnntak.shouldBeNull()
            brukeruttalelse.shouldNotBeNull {
                harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.NEI_ETTER_FORHÅNDSVARSEL
                uttalelsesdetaljer.shouldBeNull()
                kommentar shouldBe "har ikke uttalet seg"
            }
        }

        somSaksbehandler("Z999999") {
            documentController.forhåndsvarselUnntak(
                behandlingId,
                ForhåndsvarselUnntakDto(
                    begrunnelseForUnntak = VarslingsUnntak.IKKE_PRAKTISK_MULIG,
                    beskrivelse = "Unntak",
                ),
            )
        }

        documentController.hentForhåndsvarselinfo(behandlingId).data.shouldNotBeNull {
            forhåndsvarselUnntak.shouldNotBeNull {
                begrunnelseForUnntak shouldBe VarslingsUnntak.IKKE_PRAKTISK_MULIG
                beskrivelse shouldBe "Unntak"
            }
            brukeruttalelse.shouldBeNull()
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
        return behandlingIdFor(fagsystemId, FagsystemDTO.TS).shouldNotBeNull()
    }

    private fun hentBrukerUttalelseDto(harBrukerUttaltSeg: HarBrukerUttaltSeg, uttalelsesdato: LocalDate) = BrukeruttalelseDto(
        harBrukerUttaltSeg = harBrukerUttaltSeg,
        uttalelsesdetaljer = listOf(
            Uttalelsesdetaljer(
                uttalelsesdato = uttalelsesdato,
                hvorBrukerenUttalteSeg = "Godsys",
                uttalelseBeskrivelse = "uttalelse",
            ),
        ),
        kommentar = null,
    )

    @Test
    fun `forhåndsvarsel detaljene er null når varselbrev ikke er sendt`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry

        val antall = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tilbakekreving_brukeruttalelse WHERE behandling_ref = ?",
            Int::class.java,
            behandling.id,
        )
        antall shouldBe 0

        val antallUttalelseinfo = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tilbakekreving_uttalelse_informasjon",
            Int::class.java,
        )
        antallUttalelseinfo shouldBe 0

        val forhåndsvarselDto = tilbakekreving.nyHentForhåndsvarselFrontendDto()
        forhåndsvarselDto.forhaandsvarselsteg.shouldBeInstanceOf<IkkeVurdertDto>()
    }

    @Test
    fun `sende forhåndsvarsel skal oppdatere varselbrevet i brevhistorikk med tid og journlaførtId`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()
        val behandlingId = tilbakekreving.behandlingHistorikk.nåværende().entry.id
        tilbakekreving.brevHistorikk.sisteVarselbrev() shouldBe null

        behandlingApiController.behandlingSendVarselbrev(behandlingId, SendForhaandsvarselDto("Tekst fra saksbehandler"))
        val tilbakekrevingEtterVarselbrev = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, tilbakekreving.eksternFagsak.eksternId)
        tilbakekrevingEtterVarselbrev!!.brevHistorikk.sisteVarselbrev() shouldNotBeNull {
            journalpostId shouldBe "-1"
            tekstFraSaksbehandler shouldBe "Tekst fra saksbehandler"
        }
    }

    @Test
    fun `forhåndsvarsel response er IKKE_STARTET når forhåndsvarselsteget ikke er behandlet`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()
        val behandlingId = tilbakekreving.behandlingHistorikk.nåværende().entry.id

        val forhåndsvarselResponse = behandlingApiController.behandlingForhandsvarsel(behandlingId).body

        forhåndsvarselResponse!!.forhaandsvarselsteg.shouldBeInstanceOf<IkkeVurdertDto>()
    }

    @Test
    fun `forhåndsvarsel response er ForhaandsvarselErSendtDto når forhåndsvarsel er sendt`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()
        val behandlingId = tilbakekreving.behandlingHistorikk.nåværende().entry.id

        behandlingApiController.behandlingSendVarselbrev(behandlingId, SendForhaandsvarselDto("Tekst fra saksbehandler"))

        val forhåndsvarselResponse = behandlingApiController.behandlingForhandsvarsel(behandlingId).body
        forhåndsvarselResponse.shouldNotBeNull().forhaandsvarselsteg.shouldBeInstanceOf<ForhaandsvarselErSendtDto> {
            it.forhåndsvarselinfo.tekstFraSaksbehandler shouldBe "Tekst fra saksbehandler"
        }
    }

    @Test
    fun `brukeruttalelse er IKKE_STARTET når forhåndsvarsel er sendt men uttalelse ikke er behandlet`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()
        val behandlingId = tilbakekreving.behandlingHistorikk.nåværende().entry.id

        behandlingApiController.behandlingSendVarselbrev(behandlingId, SendForhaandsvarselDto("Tekst fra saksbehandler"))

        val forhåndsvarselResponse = behandlingApiController.behandlingForhandsvarsel(behandlingId).body
        forhåndsvarselResponse.shouldNotBeNull().forhaandsvarselsteg.shouldBeInstanceOf<ForhaandsvarselErSendtDto> {
            it.uttalelsesfrist.opprinneligFrist shouldBe LocalDate.now().plus(Period.ofWeeks(3))
            it.uttalelsesfrist.nyFrist shouldBe null
            it.uttalelsesfrist.begrunnelse shouldBe null
        }
        forhåndsvarselResponse.brukeruttalelse!!.harBrukerUttaltSeg shouldBe UttalelseVurderingDto.IKKE_VURDERT
    }

    @Test
    fun `brukeruttalelse lagres, oppdateres og redigeres`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()
        val behandlingId = tilbakekreving.behandlingHistorikk.nåværende().entry.id

        behandlingApiController.behandlingSendVarselbrev(behandlingId, SendForhaandsvarselDto("Tekst fra saksbehandler"))

        somSaksbehandler("Z9999999") {
            behandlingApiController.behandlingLagreBrukersuttalelse(
                behandlingId = behandlingId,
                uttalelseDto = UttalelseDto(
                    harBrukerUttaltSeg = UttalelseVurderingDto.NEI_ETTER_FORHÅNDSVARSEL,
                    beskrivelse = "Gadd ikke si noe",
                ),
            )
        }

        var forhåndsvarselResponse = behandlingApiController.behandlingForhandsvarsel(behandlingId).body
        forhåndsvarselResponse.shouldNotBeNull().forhaandsvarselsteg.shouldBeInstanceOf<ForhaandsvarselErSendtDto>()
        forhåndsvarselResponse.brukeruttalelse!!.harBrukerUttaltSeg shouldBe UttalelseVurderingDto.NEI_ETTER_FORHÅNDSVARSEL
        forhåndsvarselResponse.brukeruttalelse!!.uttalelsesdato shouldBe null
        forhåndsvarselResponse.brukeruttalelse!!.beskrivelse shouldBe "Gadd ikke si noe"

        somSaksbehandler("Z9999999") {
            behandlingApiController.behandlingLagreBrukersuttalelse(
                behandlingId = behandlingId,
                uttalelseDto = UttalelseDto(
                    harBrukerUttaltSeg = UttalelseVurderingDto.JA_ETTER_FORHÅNDSVARSEL,
                    uttalelsesdato = LocalDate.of(2021, 1, 1),
                    hvorBrukerenUttalteSeg = "Reddit",
                    beskrivelse = "Typisk reddit kommentar",
                ),
            )
        }

        forhåndsvarselResponse = behandlingApiController.behandlingForhandsvarsel(behandlingId).body.shouldNotBeNull()
        forhåndsvarselResponse.forhaandsvarselsteg.shouldBeInstanceOf<ForhaandsvarselErSendtDto>()
        forhåndsvarselResponse.brukeruttalelse!!.harBrukerUttaltSeg shouldBe UttalelseVurderingDto.JA_ETTER_FORHÅNDSVARSEL
        forhåndsvarselResponse.brukeruttalelse!!.uttalelsesdato shouldBe LocalDate.of(2021, 1, 1)
        forhåndsvarselResponse.brukeruttalelse!!.hvorBrukerenUttalteSeg shouldBe "Reddit"
        somSaksbehandler("Z9999999") {
            behandlingApiController.behandlingLagreBrukersuttalelse(
                behandlingId = behandlingId,
                uttalelseDto = UttalelseDto(
                    harBrukerUttaltSeg = UttalelseVurderingDto.JA_ETTER_FORHÅNDSVARSEL,
                    uttalelsesdato = LocalDate.of(2022, 1, 1),
                    hvorBrukerenUttalteSeg = "Snap",
                    beskrivelse = "Den nye snap kanalen",
                ),
            )
        }
        forhåndsvarselResponse = behandlingApiController.behandlingForhandsvarsel(behandlingId).body.shouldNotBeNull()
        forhåndsvarselResponse.forhaandsvarselsteg.shouldBeInstanceOf<ForhaandsvarselErSendtDto>()
        forhåndsvarselResponse.brukeruttalelse!!.harBrukerUttaltSeg shouldBe UttalelseVurderingDto.JA_ETTER_FORHÅNDSVARSEL
        forhåndsvarselResponse.brukeruttalelse!!.uttalelsesdato shouldBe LocalDate.of(2022, 1, 1)
    }

    @Test
    fun `unntak lagres, oppdateres og redigeres`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()
        val behandlingId = tilbakekreving.behandlingHistorikk.nåværende().entry.id
        somSaksbehandler("Z9999999") {
            behandlingApiController.behandlingLagreForhaandsvarselUnntak(
                behandlingId,
                unntakDto = ForhaandsvarselUnntakDto(
                    begrunnelseForUnntak = VarslingsUnntakDto.IKKE_PRAKTISK_MULIG,
                    beskrivelse = "Ikke mulig å forhåndsvarsle",
                ),
            )
        }
        behandlingApiController.behandlingForhandsvarsel(behandlingId).body.shouldNotBeNull()
            .forhaandsvarselsteg.shouldBeInstanceOf<ForhaandsvarselUnntakDto> {
                it.begrunnelseForUnntak shouldBe VarslingsUnntakDto.IKKE_PRAKTISK_MULIG
                it.beskrivelse shouldBe "Ikke mulig å forhåndsvarsle"
            }
        somSaksbehandler("Z9999999") {
            behandlingApiController.behandlingLagreForhaandsvarselUnntak(
                behandlingId,
                unntakDto = ForhaandsvarselUnntakDto(
                    begrunnelseForUnntak = VarslingsUnntakDto.ÅPENBART_UNØDVENDIG,
                    beskrivelse = "Unntak ingen uttalelse",
                ),
            )
        }

        somSaksbehandler("Z9999999") {
            behandlingApiController.behandlingLagreBrukersuttalelse(
                behandlingId,
                UttalelseDto(
                    harBrukerUttaltSeg = UttalelseVurderingDto.UNNTAK_INGEN_UTTALELSE,
                    beskrivelse = "Bruker har ikke uttalt seg",
                ),
            )
        }

        behandlingApiController.behandlingForhandsvarsel(behandlingId).body.shouldNotBeNull()
            .forhaandsvarselsteg.shouldBeInstanceOf<ForhaandsvarselUnntakDto> {
                it.begrunnelseForUnntak shouldBe VarslingsUnntakDto.ÅPENBART_UNØDVENDIG
                it.beskrivelse shouldBe "Unntak ingen uttalelse"
            }

        somSaksbehandler("Z9999999") {
            behandlingApiController.behandlingLagreForhaandsvarselUnntak(
                behandlingId,
                unntakDto = ForhaandsvarselUnntakDto(
                    begrunnelseForUnntak = VarslingsUnntakDto.ÅPENBART_UNØDVENDIG,
                    beskrivelse = "Allerede uttalet seg",
                ),
            )

            behandlingApiController.behandlingLagreBrukersuttalelse(
                behandlingId,
                UttalelseDto(
                    harBrukerUttaltSeg = UttalelseVurderingDto.UNNTAK_ALLEREDE_UTTALT_SEG,
                    uttalelsesdato = LocalDate.of(2021, 1, 1),
                    hvorBrukerenUttalteSeg = "Reddit",
                    beskrivelse = "Typisk reddit kommentar",
                ),
            )
        }

        behandlingApiController.behandlingForhandsvarsel(behandlingId).body.shouldNotBeNull()
            .forhaandsvarselsteg.shouldBeInstanceOf<ForhaandsvarselUnntakDto> {
                it.begrunnelseForUnntak shouldBe VarslingsUnntakDto.ÅPENBART_UNØDVENDIG
                it.beskrivelse shouldBe "Allerede uttalet seg"
            }

        somSaksbehandler("Z9999999") {
            behandlingApiController.behandlingLagreForhaandsvarselUnntak(
                behandlingId,
                unntakDto = ForhaandsvarselUnntakDto(
                    begrunnelseForUnntak = VarslingsUnntakDto.ÅPENBART_UNØDVENDIG,
                    beskrivelse = "Allerede uttalet seg",
                ),
            )

            behandlingApiController.behandlingLagreBrukersuttalelse(
                behandlingId,
                UttalelseDto(
                    harBrukerUttaltSeg = UttalelseVurderingDto.UNNTAK_ALLEREDE_UTTALT_SEG,
                    uttalelsesdato = LocalDate.of(2022, 1, 1),
                    hvorBrukerenUttalteSeg = "Snap",
                    beskrivelse = "beskrivelse",
                ),
            )
        }

        behandlingApiController.behandlingForhandsvarsel(behandlingId).body.shouldNotBeNull()
            .forhaandsvarselsteg.shouldBeInstanceOf<ForhaandsvarselUnntakDto> {
                it.begrunnelseForUnntak shouldBe VarslingsUnntakDto.ÅPENBART_UNØDVENDIG
                it.beskrivelse shouldBe "Allerede uttalet seg"
            }
    }

    @Test
    fun `uttalelsesfrist lagres, oppdateres og redigeres`() {
        val tilbakekreving = opprettTilbakekrevingOgHentFagsystemId()
        val behandlingId = tilbakekreving.behandlingHistorikk.nåværende().entry.id

        behandlingApiController.behandlingSendVarselbrev(behandlingId, SendForhaandsvarselDto("Tekst fra saksbehandler"))

        var response = behandlingApiController.behandlingForhandsvarsel(behandlingId).body.shouldNotBeNull()
        response.forhaandsvarselsteg.shouldBeInstanceOf<ForhaandsvarselErSendtDto> {
            it.uttalelsesfrist.shouldNotBeNull()
            it.uttalelsesfrist.opprinneligFrist shouldBe LocalDate.now().plus(Period.ofWeeks(3))
        }
        response.brukeruttalelse!!.harBrukerUttaltSeg shouldBe UttalelseVurderingDto.IKKE_VURDERT
        somSaksbehandler("Z9999999") {
            behandlingApiController.behandlingUtsettUttalelsesfrist(
                behandlingId,
                utsettFristDto = UpdateUttalelsesfristDto(
                    nyFrist = LocalDate.of(2027, 1, 1),
                    begrunnelse = "Advokat vil ha mer tid",
                ),
            )
        }

        response = behandlingApiController.behandlingForhandsvarsel(behandlingId).body.shouldNotBeNull()
        response.forhaandsvarselsteg.shouldBeInstanceOf<ForhaandsvarselErSendtDto> {
            it.uttalelsesfrist.opprinneligFrist shouldBe LocalDate.now().plus(Period.ofWeeks(3))
            it.uttalelsesfrist.nyFrist shouldBe LocalDate.of(2027, 1, 1)
            it.uttalelsesfrist.begrunnelse shouldBe "Advokat vil ha mer tid"
        }
        response.brukeruttalelse!!.harBrukerUttaltSeg shouldBe UttalelseVurderingDto.IKKE_VURDERT
        somSaksbehandler("Z9999999") {
            behandlingApiController.behandlingUtsettUttalelsesfrist(
                behandlingId,
                utsettFristDto = UpdateUttalelsesfristDto(
                    nyFrist = LocalDate.of(2028, 1, 1),
                    begrunnelse = "Advokat vil ha enda mer tid",
                ),
            )
        }

        response = behandlingApiController.behandlingForhandsvarsel(behandlingId).body.shouldNotBeNull()
        response.shouldNotBeNull().forhaandsvarselsteg.shouldBeInstanceOf<ForhaandsvarselErSendtDto> {
            it.uttalelsesfrist.opprinneligFrist shouldBe LocalDate.now().plus(Period.ofWeeks(3))
            it.uttalelsesfrist.nyFrist shouldBe LocalDate.of(2028, 1, 1)
            it.uttalelsesfrist.begrunnelse shouldBe "Advokat vil ha enda mer tid"
        }
        response.brukeruttalelse!!.harBrukerUttaltSeg shouldBe UttalelseVurderingDto.IKKE_VURDERT
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
        return tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
    }
}
