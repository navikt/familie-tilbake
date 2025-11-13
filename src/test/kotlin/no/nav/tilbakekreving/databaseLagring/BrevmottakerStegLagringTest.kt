package no.nav.tilbakekreving.databaseLagring

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Testdata
import no.nav.tilbakekreving.behandling.saksbehandling.RegistrertBrevmottaker
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.e2e.TilbakekrevingE2EBase
import no.nav.tilbakekreving.e2e.ytelser.TilleggsstønaderE2ETest.Companion.TILLEGGSSTØNADER_KØ_NAVN
import no.nav.tilbakekreving.entities.MottakerType
import no.nav.tilbakekreving.fagsystem.FagsystemIntegrasjonService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.feil.Sporing
import no.nav.tilbakekreving.integrasjoner.KafkaProducerStub
import no.nav.tilbakekreving.kontrakter.brev.ManuellAdresseInfo
import no.nav.tilbakekreving.kontrakter.verge.Vergetype
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class BrevmottakerStegLagringTest : TilbakekrevingE2EBase() {
    @Autowired
    private lateinit var fagsystemIntegrasjonService: FagsystemIntegrasjonService

    @Autowired
    private lateinit var kafkaProducer: KafkaProducerStub

    private val ansvarligSaksbehandler = "Z999999"
    private val ansvarligBeslutter = "Z111111"

    @Test
    fun `brevmottakerSteg med kun default mottaker skal lagres i db`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))
        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS)
        val tilbakekrevingId = tilbakekreving(behandlingId!!).id
        val behandling = behandling(behandlingId)

        behandlingRepository.lagreBehandlinger(listOf(behandling.tilEntity(tilbakekrevingId)))
        val brevmottakerEntity = behandling.brevmottakerSteg!!.tilEntity(behandlingId)
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.id shouldBe brevmottakerEntity.id
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.aktivert shouldBe false
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.defaultMottakerEntity.mottakerType shouldBe MottakerType.DEFAULT_MOTTAKER
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.registrertBrevmottakerEntity.mottakerType shouldBe MottakerType.DEFAULT_MOTTAKER
    }

    @Test
    fun `brevmottakerSteg med utenlandskadresse skal lagres i og hentes fra db`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))
        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS)
        val behandling = behandling(behandlingId!!)

        val utenlandskAdresseId = UUID.randomUUID()
        val utenlandskAdresseMottaker = RegistrertBrevmottaker.UtenlandskAdresseMottaker(
            id = utenlandskAdresseId,
            navn = "testbruker",
            manuellAdresseInfo = ManuellAdresseInfo(
                adresselinje1 = "adresselinje1",
                adresselinje2 = "adresselinje2",
                postnummer = "postnummer",
                poststed = "poststed",
                landkode = "landkode",
            ),
        )

        behandling.brevmottakerSteg!!.registrertBrevmottaker = utenlandskAdresseMottaker

        val brevmottakerEntity = behandling.brevmottakerSteg!!.tilEntity(behandlingId)
        brevmottakerRepository.lagre(brevmottakerEntity)

        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.id shouldBe brevmottakerEntity.id
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.defaultMottakerEntity.mottakerType shouldBe MottakerType.DEFAULT_MOTTAKER
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.registrertBrevmottakerEntity.mottakerType shouldBe MottakerType.UTENLANDSK_ADRESSE_MOTTAKER
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.registrertBrevmottakerEntity.id shouldBe utenlandskAdresseId
    }

    @Test
    fun `brevmottakerSteg med utenlandskadresse og verge skal lagres i og hentes fra db`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))
        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS)
        val behandling = behandling(behandlingId!!)

        val utenlandskAdresseId = UUID.randomUUID()
        val utenlandskAdresseMottaker = RegistrertBrevmottaker.UtenlandskAdresseMottaker(
            id = utenlandskAdresseId,
            navn = "testbruker",
            manuellAdresseInfo = ManuellAdresseInfo(
                adresselinje1 = "adresselinje1",
                adresselinje2 = "adresselinje2",
                postnummer = "postnummer",
                poststed = "poststed",
                landkode = "landkode",
            ),
        )

        val vergeId = UUID.randomUUID()
        val vergeMottaker = RegistrertBrevmottaker.VergeMottaker(
            id = vergeId,
            navn = "testbruker",
            vergeType = Vergetype.VERGE_FOR_BARN,
            personIdent = "1111",
            manuellAdresseInfo = ManuellAdresseInfo(
                adresselinje1 = "adresselinje1",
                adresselinje2 = "adresselinje2",
                postnummer = "postnummer",
                poststed = "poststed",
                landkode = "landkode",
            ),
        )

        val utenlandskOgVergeMottakerId = UUID.randomUUID()
        behandling.brevmottakerSteg!!.registrertBrevmottaker = RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker(
            id = utenlandskOgVergeMottakerId,
            utenlandskAdresse = utenlandskAdresseMottaker,
            verge = vergeMottaker,
        )

        val brevmottakerEntity = behandling.brevmottakerSteg!!.tilEntity(behandlingId)
        brevmottakerRepository.lagre(brevmottakerEntity)

        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.id shouldBe brevmottakerEntity.id
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.defaultMottakerEntity.mottakerType shouldBe MottakerType.DEFAULT_MOTTAKER
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.registrertBrevmottakerEntity.mottakerType shouldBe MottakerType.UTENLANDSK_ADRESSE_OG_VERGE_MOTTAKER
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.registrertBrevmottakerEntity.utenlandskAdresse!!.id shouldBe utenlandskAdresseId
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.registrertBrevmottakerEntity.verge!!.id shouldBe vergeId
    }

    @Test
    fun `registrert brevmottaker er default etter at utenlandskadresse er slettet`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))
        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS)
        val tilbakekrevingId = tilbakekreving(behandlingId!!).id
        val behandling = behandling(behandlingId)

        val utenlandskAdresseId = UUID.randomUUID()
        val utenlandskAdresseMottaker = RegistrertBrevmottaker.UtenlandskAdresseMottaker(
            id = utenlandskAdresseId,
            navn = "testbruker",
            manuellAdresseInfo = ManuellAdresseInfo(
                adresselinje1 = "adresselinje1",
                adresselinje2 = "adresselinje2",
                postnummer = "postnummer",
                poststed = "poststed",
                landkode = "landkode",
            ),
        )

        behandling.brevmottakerSteg!!.registrertBrevmottaker = utenlandskAdresseMottaker

        val brevmottakerEntity = behandling.brevmottakerSteg!!.tilEntity(behandlingId)
        brevmottakerRepository.lagre(brevmottakerEntity)

        behandling.brevmottakerSteg!!.aktiverSteg()
        behandling.brevmottakerSteg!!.fjernManuellBrevmottaker(utenlandskAdresseId, Sporing(fagsystemId, behandlingId.toString()))
        behandlingRepository.lagreBehandlinger(listOf(behandling.tilEntity(tilbakekrevingId)))

        val antall = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tilbakekreving_registrert_brevmottaker WHERE id = ?",
            Int::class.java,
            utenlandskAdresseId,
        )
        antall shouldBe 0

        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.id shouldBe brevmottakerEntity.id
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.defaultMottakerEntity.mottakerType shouldBe MottakerType.DEFAULT_MOTTAKER
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.registrertBrevmottakerEntity.mottakerType shouldBe MottakerType.DEFAULT_MOTTAKER
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.registrertBrevmottakerEntity.id shouldBe brevmottakerEntity.defaultMottakerEntity.id
    }

    @Test
    fun `registrert brevmottaker er defaultOgVerge etter at utenlandskadresse er slettet`() {
        val fagsystemId = KravgrunnlagGenerator.nextPaddedId(6)
        sendKravgrunnlagOgAvventLesing(
            queueName = TILLEGGSSTØNADER_KØ_NAVN,
            kravgrunnlag = KravgrunnlagGenerator.forTilleggsstønader(
                fagsystemId = fagsystemId,
            ),
        )
        fagsystemIntegrasjonService.håndter(Ytelse.Tilleggsstønad, Testdata.fagsysteminfoSvar(fagsystemId))
        val behandlingId = behandlingIdFor(fagsystemId, FagsystemDTO.TS)
        val tilbakekrevingId = tilbakekreving(behandlingId!!).id
        val behandling = behandling(behandlingId)

        val utenlandskAdresseId = UUID.randomUUID()
        val utenlandskAdresseMottaker = RegistrertBrevmottaker.UtenlandskAdresseMottaker(
            id = utenlandskAdresseId,
            navn = "testbruker",
            manuellAdresseInfo = ManuellAdresseInfo(
                adresselinje1 = "adresselinje1",
                adresselinje2 = "adresselinje2",
                postnummer = "postnummer",
                poststed = "poststed",
                landkode = "landkode",
            ),
        )

        val vergeId = UUID.randomUUID()
        val vergeMottaker = RegistrertBrevmottaker.VergeMottaker(
            id = vergeId,
            navn = "testbruker",
            vergeType = Vergetype.VERGE_FOR_BARN,
            personIdent = "1111",
            manuellAdresseInfo = ManuellAdresseInfo(
                adresselinje1 = "adresselinje1",
                adresselinje2 = "adresselinje2",
                postnummer = "postnummer",
                poststed = "poststed",
                landkode = "landkode",
            ),
        )

        val utenlandskOgVergeMottakerId = UUID.randomUUID()
        behandling.brevmottakerSteg!!.registrertBrevmottaker = RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker(
            id = utenlandskOgVergeMottakerId,
            utenlandskAdresse = utenlandskAdresseMottaker,
            verge = vergeMottaker,
        )

        val brevmottakerEntity = behandling.brevmottakerSteg!!.tilEntity(behandlingId)
        brevmottakerRepository.lagre(brevmottakerEntity)

        behandling.brevmottakerSteg!!.aktiverSteg()
        behandling.brevmottakerSteg!!.fjernManuellBrevmottaker(utenlandskAdresseId, Sporing(fagsystemId, behandlingId.toString()))
        behandlingRepository.lagreBehandlinger(listOf(behandling.tilEntity(tilbakekrevingId)))

        val antallUtenlandskAdresseId = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tilbakekreving_registrert_brevmottaker WHERE id = ?",
            Int::class.java,
            utenlandskAdresseId,
        )

        val antallVergeId = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tilbakekreving_registrert_brevmottaker WHERE id = ?",
            Int::class.java,
            vergeId,
        )

        val antallUtenlandskOgVergeMottakerId = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tilbakekreving_registrert_brevmottaker WHERE id = ?",
            Int::class.java,
            utenlandskOgVergeMottakerId,
        )

        antallVergeId shouldBe 1
        antallUtenlandskAdresseId shouldBe 0
        antallUtenlandskOgVergeMottakerId shouldBe 0
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.id shouldBe brevmottakerEntity.id
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.defaultMottakerEntity.mottakerType shouldBe MottakerType.DEFAULT_MOTTAKER
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.registrertBrevmottakerEntity.id shouldBe vergeId
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.registrertBrevmottakerEntity.mottakerType shouldBe MottakerType.VERGE_MOTTAKER
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.registrertBrevmottakerEntity.verge shouldBe null
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.registrertBrevmottakerEntity.utenlandskAdresse shouldBe null
        brevmottakerRepository.hentBrevmottaker(behandlingId)!!.registrertBrevmottakerEntity.fullmektig shouldBe null
    }
}
