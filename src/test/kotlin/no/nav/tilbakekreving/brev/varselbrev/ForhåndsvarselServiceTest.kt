package no.nav.tilbakekreving.brev.varselbrev

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forOne
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import no.nav.familie.tilbake.api.DokumentController
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.api.v1.dto.BestillBrevDto
import no.nav.tilbakekreving.api.v1.dto.BrukeruttalelseDto
import no.nav.tilbakekreving.api.v1.dto.HarBrukerUttaltSeg
import no.nav.tilbakekreving.api.v1.dto.Uttalelsesdetaljer
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator.Tilbakekrevingsbeløp.Companion.medFeilutbetaling
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.brev.Dokumentmalstype
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class ForhåndsvarselServiceTest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Autowired
    protected lateinit var dokumentController: DokumentController

    @Autowired
    protected lateinit var forhåndsvarselService: ForhåndsvarselService

    @BeforeEach
    fun cleanup() {
        jdbcTemplate.update("DELETE FROM tilbakekreving_uttalelse_informasjon")
        jdbcTemplate.update("DELETE FROM tilbakekreving_brukeruttalelse")
    }

    @Test
    fun `henter tekster til varselbrev`() {
        val fagsystemId = opprettTilbakekrevingOgHentFagsystemId()
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()

        val tekster = forhåndsvarselService.hentVarselbrevTekster(tilbakekreving)
        tekster.shouldNotBeNull()
        tekster.avsnitter.shouldNotBeEmpty()
        tekster.overskrift shouldContain "Nav vurderer om du må betale tilbake"
        tekster.avsnitter.forOne {
            it.title shouldBe ""
            it.body shouldContain "Før vi avgjør om du skal betale tilbake,"
        }
        tekster.avsnitter.forOne {
            it.title shouldBe "Dette har skjedd"
            it.body shouldContain "og endringen har ført til at du har fått utbetalt for mye."
        }
        tekster.avsnitter.forOne {
            it.title shouldBe "Dette legger vi vekt på i vurderingen vår"
            it.body shouldContain "For å avgjøre om vi kan kreve tilbake,"
        }
        tekster.avsnitter.forOne {
            it.title shouldBe "Slik uttaler du deg"
            it.body shouldContain "Du kan sende uttalelsen din ved å logge deg inn på"
        }
        tekster.avsnitter.forOne {
            it.title shouldBe "Har du spørsmål?"
            it.body shouldContain "Du finner mer informasjon på nav.no/tilleggsstonad."
        }
        tekster.avsnitter.forOne {
            it.title shouldBe "Du har rett til innsyn"
            it.body shouldContain "På nav.no/dittnav kan du se dokumentene i saken din"
        }
    }

    @Test
    fun `sende forhåndsvarsel skal oppdatere varselbrevet i brevhistorikk med tid og journlaførtId`() {
        val fagsystemId = opprettTilbakekrevingOgHentFagsystemId()
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry
        val bestillBrevDto = BestillBrevDto(
            behandlingId = behandling.id,
            brevmalkode = Dokumentmalstype.VARSEL,
            fritekst = "Tekst fra saksbehandler",
        )

        tilbakekreving.brevHistorikk.sisteVarselbrev() shouldBe null
        dokumentController.bestillBrev(bestillBrevDto)

        val tilbakekrevingEtterVarselbrev = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId)
        tilbakekrevingEtterVarselbrev!!.brevHistorikk.sisteVarselbrev() shouldNotBeNull {
            journalpostId shouldBe "-1"
        }
    }

    @Test
    fun `brukersuttalelse er en tom liste når brukeren ikke har uttalet seg`() {
        val fagsystemId = opprettTilbakekrevingOgHentFagsystemId()
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
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

        val forhåndsvarselDto = tilbakekreving.hentForhåndsvarselFrontendDto()

        forhåndsvarselDto.varselbrevDto shouldBe null
        forhåndsvarselDto.brukeruttalelse shouldBe null
    }

    @Test
    fun `brukersuttalelse og varselinfo skal lagres og hentes riktig når ingen varsel er sendt`() {
        val fagsystemId = opprettTilbakekrevingOgHentFagsystemId()
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry
        val brukeruttalelseDto = BrukeruttalelseDto(
            HarBrukerUttaltSeg.JA,
            uttalelsesdetaljer = listOf(
                Uttalelsesdetaljer(
                    hvorBrukerenUttalteSeg = "Modia",
                    uttalelsesdato = LocalDate.of(2025, 10, 15),
                    uttalelseBeskrivelse = "Bruker har uttalet seg",
                ),
            ),
            utsettFrist = null,
            beskrivelseVedNeiEllerUtsettFrist = null,
        )

        dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseDto)

        val antallBrukeruttalelser = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM tilbakekreving_brukeruttalelse WHERE behandling_ref = ?",
            Int::class.java,
            behandling.id,
        )
        antallBrukeruttalelser shouldBe 1

        val antallUttalelseinfo = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tilbakekreving_uttalelse_informasjon",
            Int::class.java,
        )
        antallUttalelseinfo shouldBe 1

        val tilbakekrevingEtterUttalelse = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val forhåndsvarselDto = tilbakekrevingEtterUttalelse.hentForhåndsvarselFrontendDto()

        forhåndsvarselDto.varselbrevDto shouldBe null
        forhåndsvarselDto.brukeruttalelse.shouldNotBeNull {
            harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.JA
            utsettFrist shouldBe null
            beskrivelseVedNeiEllerUtsettFrist == null
            uttalelsesdetaljer.shouldNotBeNull().size shouldBe 1
            uttalelsesdetaljer.shouldNotBeNull()[0].uttalelsesdato shouldBe LocalDate.of(2025, 10, 15)
            uttalelsesdetaljer.shouldNotBeNull()[0].hvorBrukerenUttalteSeg shouldBe "Modia"
            uttalelsesdetaljer.shouldNotBeNull()[0].uttalelseBeskrivelse shouldBe "Bruker har uttalet seg"
        }
    }

    @Test
    fun `brukersuttalelse og varselbrevinfo skal lagres og hentes riktig når varselbrev er sendt`() {
        val fagsystemId = opprettTilbakekrevingOgHentFagsystemId()
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry

        val bestillBrevDto = BestillBrevDto(
            behandlingId = behandling.id,
            brevmalkode = Dokumentmalstype.VARSEL,
            fritekst = "Tekst fra saksbehandler",
        )
        dokumentController.bestillBrev(bestillBrevDto)

        val brukeruttalelseDto = BrukeruttalelseDto(
            HarBrukerUttaltSeg.JA,
            uttalelsesdetaljer = listOf(
                Uttalelsesdetaljer(
                    hvorBrukerenUttalteSeg = "Tlf",
                    uttalelsesdato = LocalDate.of(2025, 9, 15),
                    uttalelseBeskrivelse = "Bruker har sagt ...",
                ),
            ),
            utsettFrist = null,
            beskrivelseVedNeiEllerUtsettFrist = null,
        )

        dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseDto)

        val tilbakekrevingEtterUttalelse = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val forhåndsvarselDto = tilbakekrevingEtterUttalelse.hentForhåndsvarselFrontendDto()

        forhåndsvarselDto.varselbrevDto shouldNotBeNull {
            varselbrevSendtTid shouldNotBe null
        }
        forhåndsvarselDto.brukeruttalelse.shouldNotBeNull {
            harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.JA
            utsettFrist shouldBe null
            beskrivelseVedNeiEllerUtsettFrist == null
            uttalelsesdetaljer.shouldNotBeNull().size shouldBe 1
            uttalelsesdetaljer.shouldNotBeNull()[0].uttalelsesdato shouldBe LocalDate.of(2025, 9, 15)
            uttalelsesdetaljer.shouldNotBeNull()[0].hvorBrukerenUttalteSeg shouldBe "Tlf"
            uttalelsesdetaljer.shouldNotBeNull()[0].uttalelseBeskrivelse shouldBe "Bruker har sagt ..."
        }
    }

    @Test
    fun `brukersuttalelse skal lagres og hentes riktig når det er flere uttalelser`() {
        val fagsystemId = opprettTilbakekrevingOgHentFagsystemId()
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry

        val brukeruttalelseDto = BrukeruttalelseDto(
            HarBrukerUttaltSeg.JA,
            uttalelsesdetaljer = listOf(
                Uttalelsesdetaljer(
                    hvorBrukerenUttalteSeg = "Tlf",
                    uttalelsesdato = LocalDate.of(2025, 9, 15),
                    uttalelseBeskrivelse = "Bruker har sagt ...",
                ),
                Uttalelsesdetaljer(
                    hvorBrukerenUttalteSeg = "Modia",
                    uttalelsesdato = LocalDate.of(2025, 9, 17),
                    uttalelseBeskrivelse = "Bruker har skrevet ...",
                ),
            ),
            utsettFrist = null,
            beskrivelseVedNeiEllerUtsettFrist = null,
        )

        dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseDto)

        val tilbakekrevingEtterUttalelse = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val forhåndsvarselDto = tilbakekrevingEtterUttalelse.hentForhåndsvarselFrontendDto()

        forhåndsvarselDto.brukeruttalelse.shouldNotBeNull {
            harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.JA
            utsettFrist shouldBe null
            beskrivelseVedNeiEllerUtsettFrist == null
            uttalelsesdetaljer.shouldNotBeNull().size shouldBe 2
            uttalelsesdetaljer.shouldNotBeNull()[0].uttalelsesdato shouldBe LocalDate.of(2025, 9, 15)
            uttalelsesdetaljer.shouldNotBeNull()[0].hvorBrukerenUttalteSeg shouldBe "Tlf"
            uttalelsesdetaljer.shouldNotBeNull()[0].uttalelseBeskrivelse shouldBe "Bruker har sagt ..."
            uttalelsesdetaljer.shouldNotBeNull()[1].uttalelsesdato shouldBe LocalDate.of(2025, 9, 17)
            uttalelsesdetaljer.shouldNotBeNull()[1].hvorBrukerenUttalteSeg shouldBe "Modia"
            uttalelsesdetaljer.shouldNotBeNull()[1].uttalelseBeskrivelse shouldBe "Bruker har skrevet ..."
        }
    }

    @Test
    fun `Skal feile når uttalelse er JA men det er ingen detaljer`() {
        val fagsystemId = opprettTilbakekrevingOgHentFagsystemId()
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry

        val bestillBrevDto = BestillBrevDto(
            behandlingId = behandling.id,
            brevmalkode = Dokumentmalstype.VARSEL,
            fritekst = "Tekst fra saksbehandler",
        )
        dokumentController.bestillBrev(bestillBrevDto)

        val brukeruttalelseDto = BrukeruttalelseDto(
            HarBrukerUttaltSeg.JA,
            uttalelsesdetaljer = null,
            utsettFrist = null,
            beskrivelseVedNeiEllerUtsettFrist = null,
        )

        shouldThrow<Exception> {
            dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseDto)
        }.message shouldBe "Det kreves uttalelsedetaljer når brukeren har uttalet seg. uttalelsedetaljer var null"

        val brukeruttalelseTomList = BrukeruttalelseDto(
            HarBrukerUttaltSeg.JA,
            uttalelsesdetaljer = listOf(),
            utsettFrist = null,
            beskrivelseVedNeiEllerUtsettFrist = null,
        )

        shouldThrow<Exception> {
            dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseTomList)
        }.message shouldBe "Det kreves uttalelsedetaljer når brukeren har uttalet seg. uttalelsedetaljer var tøm"
    }

    @Test
    fun `uttalelse skal lagres og hentes riktig når brukeren ikke har uttalt seg`() {
        val fagsystemId = opprettTilbakekrevingOgHentFagsystemId()
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry

        val brukeruttalelseDto = BrukeruttalelseDto(
            HarBrukerUttaltSeg.NEI,
            uttalelsesdetaljer = null,
            utsettFrist = null,
            beskrivelseVedNeiEllerUtsettFrist = "Ville ikke",
        )

        dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseDto)

        val tilbakekrevingEtterUttalelse = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val forhåndsvarselDto = tilbakekrevingEtterUttalelse.hentForhåndsvarselFrontendDto()

        forhåndsvarselDto.brukeruttalelse.shouldNotBeNull {
            harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.NEI
            utsettFrist shouldBe null
            beskrivelseVedNeiEllerUtsettFrist == "Ville ikke"
            uttalelsesdetaljer.shouldBeEmpty()
        }
    }

    @Test
    fun `utsatt frist skal lagres og hentes riktig`() {
        val fagsystemId = opprettTilbakekrevingOgHentFagsystemId()
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry

        val bestillBrevDto = BestillBrevDto(
            behandlingId = behandling.id,
            brevmalkode = Dokumentmalstype.VARSEL,
            fritekst = "Tekst fra saksbehandler",
        )
        dokumentController.bestillBrev(bestillBrevDto)

        val brukeruttalelseDto = BrukeruttalelseDto(
            HarBrukerUttaltSeg.UTTSETT_FRIST,
            uttalelsesdetaljer = null,
            utsettFrist = LocalDate.of(2025, 11, 15),
            beskrivelseVedNeiEllerUtsettFrist = "Utsetter bare",
        )

        dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseDto)

        val tilbakekrevingEtterUttalelse = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val forhåndsvarselDto = tilbakekrevingEtterUttalelse.hentForhåndsvarselFrontendDto()

        forhåndsvarselDto.brukeruttalelse.shouldNotBeNull {
            harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.UTTSETT_FRIST
            utsettFrist shouldBe LocalDate.of(2025, 11, 15)
            beskrivelseVedNeiEllerUtsettFrist == "Utsetter bare"
            uttalelsesdetaljer.shouldBeEmpty()
        }
    }

    @Test
    fun `Skal feile når uttalelse er NEI uten beskrivelse`() {
        val fagsystemId = opprettTilbakekrevingOgHentFagsystemId()
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry

        val bestillBrevDto = BestillBrevDto(
            behandlingId = behandling.id,
            brevmalkode = Dokumentmalstype.VARSEL,
            fritekst = "Tekst fra saksbehandler",
        )
        dokumentController.bestillBrev(bestillBrevDto)

        val brukeruttalelseDto = BrukeruttalelseDto(
            HarBrukerUttaltSeg.NEI,
            uttalelsesdetaljer = null,
            utsettFrist = null,
            beskrivelseVedNeiEllerUtsettFrist = null,
        )

        shouldThrow<Exception> {
            dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseDto)
        }.message shouldBe "Det kreves en beskrivelse når brukeren ikke uttaler seg. Beskrivelsen var null"

        val brukeruttalelseTomList = BrukeruttalelseDto(
            HarBrukerUttaltSeg.NEI,
            uttalelsesdetaljer = null,
            utsettFrist = null,
            beskrivelseVedNeiEllerUtsettFrist = "",
        )

        shouldThrow<Exception> {
            dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseTomList)
        }.message shouldBe "Det kreves en beskrivelse når brukeren ikke uttaler seg. Beskrivelsen var tøm"
    }

    @Test
    fun `Skal feile når uttalelse er UTSETT_FRIST uten beskrivelse eller ny dato`() {
        val fagsystemId = opprettTilbakekrevingOgHentFagsystemId()
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry

        val bestillBrevDto = BestillBrevDto(
            behandlingId = behandling.id,
            brevmalkode = Dokumentmalstype.VARSEL,
            fritekst = "Tekst fra saksbehandler",
        )
        dokumentController.bestillBrev(bestillBrevDto)

        val brukeruttalelseDto = BrukeruttalelseDto(
            HarBrukerUttaltSeg.UTTSETT_FRIST,
            uttalelsesdetaljer = null,
            utsettFrist = LocalDate.of(2025, 11, 15),
            beskrivelseVedNeiEllerUtsettFrist = null,
        )

        shouldThrow<Exception> {
            dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseDto)
        }.message shouldBe "Det kreves en beskrivelse når frist er utsatt. Beskrivelsen var null"

        val brukeruttalelseTomList = BrukeruttalelseDto(
            HarBrukerUttaltSeg.UTTSETT_FRIST,
            uttalelsesdetaljer = listOf(),
            utsettFrist = null,
            beskrivelseVedNeiEllerUtsettFrist = "Utsetter",
        )

        shouldThrow<Exception> {
            dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseTomList)
        }.message shouldBe "Det kreves en ny dato når fristen er utsatt"
    }

    fun opprettTilbakekrevingOgHentFagsystemId(): String {
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
        return fagsystemId
    }
}
