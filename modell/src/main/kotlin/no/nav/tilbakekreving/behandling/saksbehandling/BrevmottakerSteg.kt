package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.api.v1.dto.ManuellBrevmottakerResponsDto
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import java.util.UUID

class BrevmottakerSteg(
    private var aktivert: Boolean,
    private val defaultMottaker: RegistrertBrevmottaker,
) : Saksbehandlingsteg<List<ManuellBrevmottakerResponsDto>> {
    override val type = Behandlingssteg.BREVMOTTAKER
    var registrertBrevmottaker: RegistrertBrevmottaker = defaultMottaker

    override fun erFullstending(): Boolean {
        return true
    }

    internal fun håndter(nyBrevmottaker: RegistrertBrevmottaker) {
        if (!aktivert) {
            throw Exception("BrevmottakerSteg er ikke aktivert.")
        }
        registrertBrevmottaker = when (nyBrevmottaker) {
            is RegistrertBrevmottaker.DødsboMottaker -> registrertBrevmottaker.kombiner(nyBrevmottaker)
            is RegistrertBrevmottaker.FullmektigMottaker -> registrertBrevmottaker.kombiner(nyBrevmottaker)
            is RegistrertBrevmottaker.UtenlandskAdresseMottaker -> registrertBrevmottaker.kombiner(nyBrevmottaker)
            is RegistrertBrevmottaker.VergeMottaker -> registrertBrevmottaker.kombiner(nyBrevmottaker)
            else -> throw Exception("Kan ikke motta brevmottaker av type ${nyBrevmottaker.javaClass.simpleName}.")
        }
    }

    override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
        return registrertBrevmottaker.tilFrontendDto()
    }

    fun aktiverSteg() {
        aktivert = true
    }

    fun deaktiverSteg() {
        registrertBrevmottaker = defaultMottaker
        aktivert = false
    }

    fun erStegetAktivert() = aktivert

    fun hentRegistrertBrevmottaker() = registrertBrevmottaker

    fun fjernManuellBrevmottaker(brevmottakerId: UUID) {
        if (!aktivert) {
            throw Exception("BrevmottakerSteg er ikke aktivert.")
        }

        if (registrertBrevmottaker == defaultMottaker) {
            throw Exception("Kan ikke fjerne defaultMotatker.")
        }

        registrertBrevmottaker = registrertBrevmottaker.fjernBrevmottaker(brevmottakerId, defaultMottaker)
    }

    companion object {
        fun opprett(
            navn: String,
            ident: String,
        ) = BrevmottakerSteg(
            aktivert = false,
            RegistrertBrevmottaker.DefaultMottaker(navn = navn, personIdent = ident),
        )
    }
}
