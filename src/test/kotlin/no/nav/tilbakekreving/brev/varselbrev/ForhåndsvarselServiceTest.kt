package no.nav.tilbakekreving.brev.varselbrev

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forOne
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import no.nav.familie.tilbake.api.DokumentController
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.api.v1.dto.BestillBrevDto
import no.nav.tilbakekreving.api.v1.dto.BrukeruttalelseDto
import no.nav.tilbakekreving.api.v1.dto.ForhåndsvarselUnntakDto
import no.nav.tilbakekreving.api.v1.dto.FristUtsettelse
import no.nav.tilbakekreving.api.v1.dto.HarBrukerUttaltSeg
import no.nav.tilbakekreving.api.v1.dto.Uttalelsesdetaljer
import no.nav.tilbakekreving.api.v1.dto.VarslingsUnntak
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
        jdbcTemplate.update("DELETE FROM tilbakekreving_utsett_uttalelse")
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
        forhåndsvarselDto.forhåndsvarselUnntak shouldBe null
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
            kommentar = null,
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
            utsettFrist.shouldBeEmpty()
            kommentar == null
            uttalelsesdetaljer.shouldNotBeNull().size shouldBe 1
            uttalelsesdetaljer.shouldNotBeNull()[0].uttalelsesdato shouldBe LocalDate.of(2025, 10, 15)
            uttalelsesdetaljer.shouldNotBeNull()[0].hvorBrukerenUttalteSeg shouldBe "Modia"
            uttalelsesdetaljer.shouldNotBeNull()[0].uttalelseBeskrivelse shouldBe "Bruker har uttalet seg"
        }
        forhåndsvarselDto.forhåndsvarselUnntak shouldBe null
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
            kommentar = null,
        )

        dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseDto)

        val tilbakekrevingEtterUttalelse = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val forhåndsvarselDto = tilbakekrevingEtterUttalelse.hentForhåndsvarselFrontendDto()

        forhåndsvarselDto.varselbrevDto shouldNotBeNull {
            varselbrevSendtTid shouldNotBe null
        }
        forhåndsvarselDto.brukeruttalelse.shouldNotBeNull {
            harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.JA
            utsettFrist.shouldBeEmpty()
            kommentar == null
            uttalelsesdetaljer.shouldNotBeNull().size shouldBe 1
            uttalelsesdetaljer.shouldNotBeNull()[0].uttalelsesdato shouldBe LocalDate.of(2025, 9, 15)
            uttalelsesdetaljer.shouldNotBeNull()[0].hvorBrukerenUttalteSeg shouldBe "Tlf"
            uttalelsesdetaljer.shouldNotBeNull()[0].uttalelseBeskrivelse shouldBe "Bruker har sagt ..."
        }
        forhåndsvarselDto.forhåndsvarselUnntak shouldBe null
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
            kommentar = null,
        )

        dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseDto)

        val tilbakekrevingEtterUttalelse = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val forhåndsvarselDto = tilbakekrevingEtterUttalelse.hentForhåndsvarselFrontendDto()

        forhåndsvarselDto.brukeruttalelse.shouldNotBeNull {
            harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.JA
            utsettFrist.shouldBeEmpty()
            kommentar == null
            uttalelsesdetaljer.shouldNotBeNull().size shouldBe 2
            uttalelsesdetaljer.shouldNotBeNull()[0].uttalelsesdato shouldBe LocalDate.of(2025, 9, 15)
            uttalelsesdetaljer.shouldNotBeNull()[0].hvorBrukerenUttalteSeg shouldBe "Tlf"
            uttalelsesdetaljer.shouldNotBeNull()[0].uttalelseBeskrivelse shouldBe "Bruker har sagt ..."
            uttalelsesdetaljer.shouldNotBeNull()[1].uttalelsesdato shouldBe LocalDate.of(2025, 9, 17)
            uttalelsesdetaljer.shouldNotBeNull()[1].hvorBrukerenUttalteSeg shouldBe "Modia"
            uttalelsesdetaljer.shouldNotBeNull()[1].uttalelseBeskrivelse shouldBe "Bruker har skrevet ..."
        }
        forhåndsvarselDto.forhåndsvarselUnntak shouldBe null
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
            kommentar = null,
        )

        shouldThrow<Exception> {
            dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseDto)
        }.message shouldBe "Det kreves uttalelsedetaljer når brukeren har uttalet seg. uttalelsedetaljer var null"

        val brukeruttalelseTomList = BrukeruttalelseDto(
            HarBrukerUttaltSeg.JA,
            uttalelsesdetaljer = listOf(),
            utsettFrist = null,
            kommentar = null,
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
            kommentar = "Ville ikke",
        )

        dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseDto)

        val tilbakekrevingEtterUttalelse = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val forhåndsvarselDto = tilbakekrevingEtterUttalelse.hentForhåndsvarselFrontendDto()

        forhåndsvarselDto.brukeruttalelse.shouldNotBeNull {
            harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.NEI
            utsettFrist.shouldBeEmpty()
            kommentar == "Ville ikke"
            uttalelsesdetaljer.shouldBeEmpty()
        }
        forhåndsvarselDto.forhåndsvarselUnntak shouldBe null
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
            utsettFrist = listOf(FristUtsettelse(LocalDate.of(2025, 11, 15), "Advokat vil ha mer tid")),
            kommentar = null,
        )

        dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseDto)

        val tilbakekrevingEtterUttalelse = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val forhåndsvarselDto = tilbakekrevingEtterUttalelse.hentForhåndsvarselFrontendDto()

        forhåndsvarselDto.brukeruttalelse.shouldNotBeNull {
            harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.UTTSETT_FRIST
            utsettFrist!!.size shouldBe 1
            uttalelsesdetaljer.shouldBeEmpty()
        }
        forhåndsvarselDto.brukeruttalelse!!.utsettFrist!![0].nyFrist shouldBe LocalDate.of(2025, 11, 15)
        forhåndsvarselDto.brukeruttalelse!!.utsettFrist!![0].begrunnelse shouldBe "Advokat vil ha mer tid"
        forhåndsvarselDto.forhåndsvarselUnntak shouldBe null
    }

    @Test
    fun `flere utsatt frist skal lagres og hentes riktig`() {
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
            utsettFrist = listOf(
                FristUtsettelse(LocalDate.of(2025, 11, 15), "Advokat vil ha mer tid"),
                FristUtsettelse(LocalDate.of(2025, 11, 25), "Advokat vil ha enda mer tid"),
            ),
            kommentar = null,
        )

        dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseDto)

        val tilbakekrevingEtterUttalelse = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val forhåndsvarselDto = tilbakekrevingEtterUttalelse.hentForhåndsvarselFrontendDto()

        forhåndsvarselDto.brukeruttalelse.shouldNotBeNull {
            harBrukerUttaltSeg shouldBe HarBrukerUttaltSeg.UTTSETT_FRIST
            utsettFrist!!.size shouldBe 2
            uttalelsesdetaljer.shouldBeEmpty()
        }
        forhåndsvarselDto.brukeruttalelse!!.utsettFrist!![0].nyFrist shouldBe LocalDate.of(2025, 11, 15)
        forhåndsvarselDto.brukeruttalelse!!.utsettFrist!![0].begrunnelse shouldBe "Advokat vil ha mer tid"
        forhåndsvarselDto.brukeruttalelse!!.utsettFrist!![1].nyFrist shouldBe LocalDate.of(2025, 11, 25)
        forhåndsvarselDto.brukeruttalelse!!.utsettFrist!![1].begrunnelse shouldBe "Advokat vil ha enda mer tid"
        forhåndsvarselDto.forhåndsvarselUnntak shouldBe null
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
            kommentar = null,
        )

        shouldThrow<Exception> {
            dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseDto)
        }.message shouldBe "Det kreves en kommentar når brukeren ikke uttaler seg. Kommentar var null"

        val brukeruttalelseTomList = BrukeruttalelseDto(
            HarBrukerUttaltSeg.NEI,
            uttalelsesdetaljer = null,
            utsettFrist = null,
            kommentar = "",
        )

        shouldThrow<Exception> {
            dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseTomList)
        }.message shouldBe "Det kreves en kommentar når brukeren ikke uttaler seg. Kommentar var tøm"
    }

    @Test
    fun `Skal feile når uttalelse er UTSETT_FRIST uten ny dato`() {
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
            utsettFrist = null,
            kommentar = null,
        )

        shouldThrow<Exception> {
            dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseDto)
        }.message shouldBe "Det kreves en ny dato når fristen er utsatt"

        val brukeruttalelseDtoTomListe = BrukeruttalelseDto(
            HarBrukerUttaltSeg.UTTSETT_FRIST,
            uttalelsesdetaljer = null,
            utsettFrist = listOf(),
            kommentar = null,
        )

        shouldThrow<Exception> {
            dokumentController.lagreBrukeruttalelse(behandling.id, brukeruttalelseDtoTomListe)
        }.message shouldBe "Det kreves en ny dato når fristen er utsatt"
    }

    @Test
    fun `skal ikke sende forhåndsvarsel ved å velge NEI og PRAKTISK_IKKE_MULIG`() {
        val fagsystemId = opprettTilbakekrevingOgHentFagsystemId()
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry

        val forhåndsvarselUnntakDto = ForhåndsvarselUnntakDto(
            behandlingId = behandling.id,
            begrunnelseForUnntak = VarslingsUnntak.IKKE_PRAKTISK_MULIG,
            beskrivelse = "Ikke praktisk mulig",
            uttalelsesdetaljer = null,
        )

        dokumentController.forhåndsvarselUnntak(forhåndsvarselUnntakDto)

        val tilbakekrevingEtterForrespørsel = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val forhåndsvarselInfo = tilbakekrevingEtterForrespørsel.hentForhåndsvarselFrontendDto()

        forhåndsvarselInfo.shouldNotBeNull {
            varselbrevDto.shouldBeNull()
            brukeruttalelse.shouldBeNull()
            forhåndsvarselUnntak.shouldNotBeNull {
                begrunnelseForUnntak shouldBe VarslingsUnntak.IKKE_PRAKTISK_MULIG
                beskrivelse shouldBe "Ikke praktisk mulig"
                uttalelsesdetaljer.shouldBeNull()
            }
        }
    }

    @Test
    fun `skal ikke sende forhåndsvarsel ved å velge NEI og UKJENT_ADRESSE`() {
        val fagsystemId = opprettTilbakekrevingOgHentFagsystemId()
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry

        val forhåndsvarselUnntakDto = ForhåndsvarselUnntakDto(
            behandlingId = behandling.id,
            begrunnelseForUnntak = VarslingsUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING,
            beskrivelse = "Ukjent adresse",
            uttalelsesdetaljer = null,
        )

        dokumentController.forhåndsvarselUnntak(forhåndsvarselUnntakDto)

        val tilbakekrevingEtterForrespørsel = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val forhåndsvarselInfo = tilbakekrevingEtterForrespørsel.hentForhåndsvarselFrontendDto()

        forhåndsvarselInfo.shouldNotBeNull {
            varselbrevDto.shouldBeNull()
            brukeruttalelse.shouldBeNull()
            forhåndsvarselUnntak.shouldNotBeNull {
                begrunnelseForUnntak shouldBe VarslingsUnntak.UKJENT_ADRESSE_ELLER_URIMELIG_ETTERSPORING
                beskrivelse shouldBe "Ukjent adresse"
                uttalelsesdetaljer.shouldBeNull()
            }
        }
    }

    @Test
    fun `skal feile når det er forhåndsvarsel unntak, ALLEREDE_UTTALET_SEG, men ingen uttalelse er oppgitt `() {
        val fagsystemId = opprettTilbakekrevingOgHentFagsystemId()
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry

        val forhåndsvarselUnntakDto = ForhåndsvarselUnntakDto(
            behandlingId = behandling.id,
            begrunnelseForUnntak = VarslingsUnntak.ALLEREDE_UTTALET_SEG,
            beskrivelse = "Allerede Uttalet seg",
            uttalelsesdetaljer = null,
        )

        shouldThrow<Exception> {
            dokumentController.forhåndsvarselUnntak(forhåndsvarselUnntakDto)
        }.message shouldBe "Det kreves uttalelsedetaljer når brukeren har allerede uttalet seg. uttalelsedetaljer var null"

        val forhåndsvarselUnntakDtoTomListe = ForhåndsvarselUnntakDto(
            behandlingId = behandling.id,
            begrunnelseForUnntak = VarslingsUnntak.ALLEREDE_UTTALET_SEG,
            beskrivelse = "Allerede Uttalet seg",
            uttalelsesdetaljer = listOf(),
        )

        shouldThrow<Exception> {
            dokumentController.forhåndsvarselUnntak(forhåndsvarselUnntakDtoTomListe)
        }.message shouldBe "Det kreves uttalelsedetaljer når brukeren har allerede uttalet seg. uttalelsedetaljer var tøm"
    }

    @Test
    fun `skal ikke sende forhåndsvarsel ved å velge NEI og ÅPENBART_UNØDVENDIG`() {
        val fagsystemId = opprettTilbakekrevingOgHentFagsystemId()
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry

        val forhåndsvarselUnntakDto = ForhåndsvarselUnntakDto(
            behandlingId = behandling.id,
            begrunnelseForUnntak = VarslingsUnntak.ÅPENBART_UNØDVENDIG,
            beskrivelse = "Unødvendig",
            uttalelsesdetaljer = null,
        )

        dokumentController.forhåndsvarselUnntak(forhåndsvarselUnntakDto)

        val tilbakekrevingEtterForrespørsel = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val forhåndsvarselInfo = tilbakekrevingEtterForrespørsel.hentForhåndsvarselFrontendDto()

        forhåndsvarselInfo.shouldNotBeNull {
            varselbrevDto.shouldBeNull()
            brukeruttalelse.shouldBeNull()
            forhåndsvarselUnntak.shouldNotBeNull {
                begrunnelseForUnntak shouldBe VarslingsUnntak.ÅPENBART_UNØDVENDIG
                beskrivelse shouldBe "Unødvendig"
                uttalelsesdetaljer.shouldBeNull()
            }
        }
    }

    @Test
    fun `skal ikke sende forhåndsvarsel ved å velge NEI og ALLEREDE_UTTALET`() {
        val fagsystemId = opprettTilbakekrevingOgHentFagsystemId()
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry

        val forhåndsvarselUnntakDto = ForhåndsvarselUnntakDto(
            behandlingId = behandling.id,
            begrunnelseForUnntak = VarslingsUnntak.ALLEREDE_UTTALET_SEG,
            beskrivelse = "Brukeren allerede uttalet seg",
            uttalelsesdetaljer = listOf(
                Uttalelsesdetaljer(
                    hvorBrukerenUttalteSeg = "Modia",
                    uttalelsesdato = LocalDate.of(2025, 10, 15),
                    uttalelseBeskrivelse = "Bruker har uttalet seg",
                ),
            ),
        )

        dokumentController.forhåndsvarselUnntak(forhåndsvarselUnntakDto)

        val tilbakekrevingEtterForrespørsel = tilbakekrevingService.hentTilbakekreving(FagsystemDTO.TS, fagsystemId).shouldNotBeNull()
        val forhåndsvarselInfo = tilbakekrevingEtterForrespørsel.hentForhåndsvarselFrontendDto()

        forhåndsvarselInfo.shouldNotBeNull {
            varselbrevDto.shouldBeNull()
            brukeruttalelse.shouldBeNull()
            forhåndsvarselUnntak.shouldNotBeNull {
                begrunnelseForUnntak shouldBe VarslingsUnntak.ALLEREDE_UTTALET_SEG
                beskrivelse shouldBe "Brukeren allerede uttalet seg"
                uttalelsesdetaljer!!.size shouldBe 1
                uttalelsesdetaljer!![0].hvorBrukerenUttalteSeg shouldBe "Modia"
                uttalelsesdetaljer!![0].uttalelsesdato shouldBe LocalDate.of(2025, 10, 15)
                uttalelsesdetaljer!![0].uttalelseBeskrivelse shouldBe "Bruker har uttalet seg"
            }
        }
    }

    private fun opprettTilbakekrevingOgHentFagsystemId(): String {
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
