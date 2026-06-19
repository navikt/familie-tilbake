package no.nav.familie.tilbake.micrometer

import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.familie.tilbake.micrometer.domain.MeldingstellingRepository
import no.nav.familie.tilbake.micrometer.domain.Meldingstype
import no.nav.familie.tilbake.micrometer.domain.Mottaksstatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

// TODO: Fortsatt en blocker for parallelle tester
internal class TellerServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var tellerService: TellerService

    @Autowired
    private lateinit var meldingstellingRepository: MeldingstellingRepository

    private fun antall(
        fagsystem: Fagsystem,
        type: Meldingstype,
        status: Mottaksstatus,
    ): Int = meldingstellingRepository.findByFagsystemAndTypeAndStatusAndDato(fagsystem, type, status)?.antall ?: 0

    @Test
    fun `tellKobletKravgrunnlag oppretter ny forekomst ved dagnes første telling`() {
        val initial = antall(Fagsystem.EF, Meldingstype.KRAVGRUNNLAG, Mottaksstatus.KOBLET)
        tellerService.tellKobletKravgrunnlag(fagsystem = Fagsystem.EF)

        val meldingstelling = meldingstellingRepository.findByFagsystemAndTypeAndStatusAndDato(
            Fagsystem.EF,
            Meldingstype.KRAVGRUNNLAG,
            Mottaksstatus.KOBLET,
        )

        meldingstelling!!.antall shouldBe initial + 1
    }

    @Test
    fun `tellUkobletKravgrunnlag oppretter ny forekomst ved dagnes første telling`() {
        val initial = antall(Fagsystem.EF, Meldingstype.KRAVGRUNNLAG, Mottaksstatus.UKOBLET)
        tellerService.tellUkobletKravgrunnlag(fagsystem = Fagsystem.EF)

        val meldingstelling = meldingstellingRepository.findByFagsystemAndTypeAndStatusAndDato(
            Fagsystem.EF,
            Meldingstype.KRAVGRUNNLAG,
            Mottaksstatus.UKOBLET,
        )

        meldingstelling!!.antall shouldBe initial + 1
    }

    @Test
    fun `tellKobletStatusmelding oppretter ny forekomst ved dagnes første telling`() {
        val initial = antall(Fagsystem.EF, Meldingstype.STATUSMELDING, Mottaksstatus.KOBLET)
        tellerService.tellKobletStatusmelding(fagsystem = Fagsystem.EF)

        val meldingstelling = meldingstellingRepository.findByFagsystemAndTypeAndStatusAndDato(
            Fagsystem.EF,
            Meldingstype.STATUSMELDING,
            Mottaksstatus.KOBLET,
        )

        meldingstelling!!.antall shouldBe initial + 1
    }

    @Test
    fun `tellUkobletStatusmelding oppretter ny forekomst ved dagnes første telling`() {
        val initial = antall(Fagsystem.EF, Meldingstype.STATUSMELDING, Mottaksstatus.UKOBLET)
        tellerService.tellUkobletStatusmelding(fagsystem = Fagsystem.EF)

        val meldingstelling = meldingstellingRepository.findByFagsystemAndTypeAndStatusAndDato(
            Fagsystem.EF,
            Meldingstype.STATUSMELDING,
            Mottaksstatus.UKOBLET,
        )

        meldingstelling!!.antall shouldBe initial + 1
    }

    @Test
    fun `tellKobletKravgrunnlag oppdaterer eksisterende teller ved påfølgende tellinger`() {
        val initial = antall(Fagsystem.EF, Meldingstype.KRAVGRUNNLAG, Mottaksstatus.KOBLET)
        tellerService.tellKobletKravgrunnlag(fagsystem = Fagsystem.EF)
        tellerService.tellKobletKravgrunnlag(fagsystem = Fagsystem.EF)

        val meldingstelling = meldingstellingRepository.findByFagsystemAndTypeAndStatusAndDato(
            Fagsystem.EF,
            Meldingstype.KRAVGRUNNLAG,
            Mottaksstatus.KOBLET,
        )

        meldingstelling!!.antall shouldBe initial + 2
    }

    @Test
    fun `tellUkobletKravgrunnlag oppdaterer eksisterende teller ved påfølgende tellinger`() {
        val initial = antall(Fagsystem.EF, Meldingstype.KRAVGRUNNLAG, Mottaksstatus.UKOBLET)
        tellerService.tellUkobletKravgrunnlag(fagsystem = Fagsystem.EF)
        tellerService.tellUkobletKravgrunnlag(fagsystem = Fagsystem.EF)

        val meldingstelling = meldingstellingRepository.findByFagsystemAndTypeAndStatusAndDato(
            Fagsystem.EF,
            Meldingstype.KRAVGRUNNLAG,
            Mottaksstatus.UKOBLET,
        )

        meldingstelling!!.antall shouldBe initial + 2
    }

    @Test
    fun `tellKobletStatusmelding oppdaterer eksisterende teller ved påfølgende tellinger`() {
        val initial = antall(Fagsystem.EF, Meldingstype.STATUSMELDING, Mottaksstatus.KOBLET)
        tellerService.tellKobletStatusmelding(fagsystem = Fagsystem.EF)
        tellerService.tellKobletStatusmelding(fagsystem = Fagsystem.EF)

        val meldingstelling = meldingstellingRepository.findByFagsystemAndTypeAndStatusAndDato(
            Fagsystem.EF,
            Meldingstype.STATUSMELDING,
            Mottaksstatus.KOBLET,
        )

        meldingstelling!!.antall shouldBe initial + 2
    }

    @Test
    fun `tellUkobletStatusmelding oppdaterer eksisterende teller ved påfølgende tellinger`() {
        val initial = antall(Fagsystem.EF, Meldingstype.STATUSMELDING, Mottaksstatus.UKOBLET)
        tellerService.tellUkobletStatusmelding(fagsystem = Fagsystem.EF)
        tellerService.tellUkobletStatusmelding(fagsystem = Fagsystem.EF)

        val meldingstelling = meldingstellingRepository.findByFagsystemAndTypeAndStatusAndDato(
            Fagsystem.EF,
            Meldingstype.STATUSMELDING,
            Mottaksstatus.UKOBLET,
        )

        meldingstelling!!.antall shouldBe initial + 2
    }
}
