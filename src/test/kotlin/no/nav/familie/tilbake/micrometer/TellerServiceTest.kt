package no.nav.familie.tilbake.micrometer

import io.kotest.matchers.shouldBe
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.micrometer.domain.MeldingstellingRepository
import no.nav.familie.tilbake.micrometer.domain.Meldingstype
import no.nav.familie.tilbake.micrometer.domain.Mottaksstatus
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class TellerServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var tellerService: TellerService

    @Autowired
    private lateinit var meldingstellingRepository: MeldingstellingRepository

    @Test
    fun `tellKobletKravgrunnlag oppretter ny forekomst ved dagnes første telling`() {
        tellerService.tellKobletKravgrunnlag(ytelsestype = Ytelsestype.OVERGANGSSTØNAD)

        val meldingstelling = meldingstellingRepository.findByYtelsestypeAndAndTypeAndStatusAndDato(Ytelsestype.OVERGANGSSTØNAD,
                                                                                                    Meldingstype.KRAVGRUNNLAG,
                                                                                                    Mottaksstatus.KOBLET)

        meldingstelling!!.antall shouldBe 1
    }

    @Test
    fun `tellUkobletKravgrunnlag oppretter ny forekomst ved dagnes første telling`() {
        tellerService.tellUkobletKravgrunnlag(ytelsestype = Ytelsestype.OVERGANGSSTØNAD)

        val meldingstelling = meldingstellingRepository.findByYtelsestypeAndAndTypeAndStatusAndDato(Ytelsestype.OVERGANGSSTØNAD,
                                                                                                    Meldingstype.KRAVGRUNNLAG,
                                                                                                    Mottaksstatus.UKOBLET)

        meldingstelling!!.antall shouldBe 1
    }

    @Test
    fun `tellKobletStatusmelding oppretter ny forekomst ved dagnes første telling`() {
        tellerService.tellKobletStatusmelding(ytelsestype = Ytelsestype.OVERGANGSSTØNAD)

        val meldingstelling = meldingstellingRepository.findByYtelsestypeAndAndTypeAndStatusAndDato(Ytelsestype.OVERGANGSSTØNAD,
                                                                                                    Meldingstype.STATUSMELDING,
                                                                                                    Mottaksstatus.KOBLET)

        meldingstelling!!.antall shouldBe 1
    }

    @Test
    fun `tellUkobletStatusmelding oppretter ny forekomst ved dagnes første telling`() {
        tellerService.tellUkobletStatusmelding(ytelsestype = Ytelsestype.OVERGANGSSTØNAD)

        val meldingstelling = meldingstellingRepository.findByYtelsestypeAndAndTypeAndStatusAndDato(Ytelsestype.OVERGANGSSTØNAD,
                                                                                                    Meldingstype.STATUSMELDING,
                                                                                                    Mottaksstatus.UKOBLET)

        meldingstelling!!.antall shouldBe 1
    }

    @Test
    fun `tellKobletKravgrunnlag oppdaterer eksisterende teller ved påfølgende tellinger`() {
        tellerService.tellKobletKravgrunnlag(ytelsestype = Ytelsestype.OVERGANGSSTØNAD)
        tellerService.tellKobletKravgrunnlag(ytelsestype = Ytelsestype.OVERGANGSSTØNAD)

        val meldingstelling = meldingstellingRepository.findByYtelsestypeAndAndTypeAndStatusAndDato(Ytelsestype.OVERGANGSSTØNAD,
                                                                                                    Meldingstype.KRAVGRUNNLAG,
                                                                                                    Mottaksstatus.KOBLET)

        meldingstelling!!.antall shouldBe 2
    }

    @Test
    fun `tellUkobletKravgrunnlag oppdaterer eksisterende teller ved påfølgende tellinger`() {
        tellerService.tellUkobletKravgrunnlag(ytelsestype = Ytelsestype.OVERGANGSSTØNAD)
        tellerService.tellUkobletKravgrunnlag(ytelsestype = Ytelsestype.OVERGANGSSTØNAD)

        val meldingstelling = meldingstellingRepository.findByYtelsestypeAndAndTypeAndStatusAndDato(Ytelsestype.OVERGANGSSTØNAD,
                                                                                                    Meldingstype.KRAVGRUNNLAG,
                                                                                                    Mottaksstatus.UKOBLET)

        meldingstelling!!.antall shouldBe 2
    }

    @Test
    fun `tellKobletStatusmelding oppdaterer eksisterende teller ved påfølgende tellinger`() {
        tellerService.tellKobletStatusmelding(ytelsestype = Ytelsestype.OVERGANGSSTØNAD)
        tellerService.tellKobletStatusmelding(ytelsestype = Ytelsestype.OVERGANGSSTØNAD)

        val meldingstelling = meldingstellingRepository.findByYtelsestypeAndAndTypeAndStatusAndDato(Ytelsestype.OVERGANGSSTØNAD,
                                                                                                    Meldingstype.STATUSMELDING,
                                                                                                    Mottaksstatus.KOBLET)

        meldingstelling!!.antall shouldBe 2
    }

    @Test
    fun `tellUkobletStatusmelding oppdaterer eksisterende teller ved påfølgende tellinger`() {
        tellerService.tellUkobletStatusmelding(ytelsestype = Ytelsestype.OVERGANGSSTØNAD)
        tellerService.tellUkobletStatusmelding(ytelsestype = Ytelsestype.OVERGANGSSTØNAD)

        val meldingstelling = meldingstellingRepository.findByYtelsestypeAndAndTypeAndStatusAndDato(Ytelsestype.OVERGANGSSTØNAD,
                                                                                                    Meldingstype.STATUSMELDING,
                                                                                                    Mottaksstatus.UKOBLET)

        meldingstelling!!.antall shouldBe 2
    }
}