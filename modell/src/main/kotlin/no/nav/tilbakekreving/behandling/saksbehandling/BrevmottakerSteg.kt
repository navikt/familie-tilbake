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
        if (registrertBrevmottaker == defaultMottaker) {
            registrertBrevmottaker = håndterNyMottaker(nyBrevmottaker)
        } else {
            registrertBrevmottaker = oppdaterRegistrertBrevmottaker(registrertBrevmottaker, nyBrevmottaker)
        }
    }

    private fun håndterNyMottaker(nyBrevmottaker: RegistrertBrevmottaker): RegistrertBrevmottaker =
        when (nyBrevmottaker) {
            is RegistrertBrevmottaker.FullmektigMottaker -> RegistrertBrevmottaker.DefaultBrukerAdresseOgFullmektigMottaker(
                id = UUID.randomUUID(),
                defaultMottaker = defaultMottaker as RegistrertBrevmottaker.DefaultMottaker,
                fullmektig = nyBrevmottaker,
            )

            is RegistrertBrevmottaker.VergeMottaker -> RegistrertBrevmottaker.DefaultBrukerAdresseOgVergeMottaker(
                id = UUID.randomUUID(),
                defaultMottaker = defaultMottaker as RegistrertBrevmottaker.DefaultMottaker,
                verge = nyBrevmottaker,
            )

            else -> nyBrevmottaker
        }

    private fun oppdaterRegistrertBrevmottaker(
        eksisterendeBrevmottaker: RegistrertBrevmottaker,
        nyBrevmottaker: RegistrertBrevmottaker,
    ): RegistrertBrevmottaker = when (eksisterendeBrevmottaker) {
        is RegistrertBrevmottaker.UtenlandskAdresseMottaker -> when (nyBrevmottaker) {
            is RegistrertBrevmottaker.VergeMottaker -> RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker(
                UUID.randomUUID(),
                utenlandskAdresse = eksisterendeBrevmottaker,
                verge = nyBrevmottaker,
            )
            is RegistrertBrevmottaker.FullmektigMottaker -> RegistrertBrevmottaker.UtenlandskAdresseOgFullmektigMottaker(
                UUID.randomUUID(),
                utenlandskAdresse = eksisterendeBrevmottaker,
                fullmektig = nyBrevmottaker,
            )
            else -> nyBrevmottaker
        }

        is RegistrertBrevmottaker.DefaultBrukerAdresseOgVergeMottaker -> when (nyBrevmottaker) {
            is RegistrertBrevmottaker.UtenlandskAdresseMottaker -> RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker(
                UUID.randomUUID(),
                utenlandskAdresse = nyBrevmottaker,
                verge = eksisterendeBrevmottaker.verge,
            )
            is RegistrertBrevmottaker.FullmektigMottaker -> RegistrertBrevmottaker.DefaultBrukerAdresseOgFullmektigMottaker(
                UUID.randomUUID(),
                defaultMottaker = eksisterendeBrevmottaker.defaultMottaker,
                fullmektig = nyBrevmottaker,
            )
            else -> nyBrevmottaker
        }

        is RegistrertBrevmottaker.DefaultBrukerAdresseOgFullmektigMottaker -> when (nyBrevmottaker) {
            is RegistrertBrevmottaker.UtenlandskAdresseMottaker -> RegistrertBrevmottaker.UtenlandskAdresseOgFullmektigMottaker(
                UUID.randomUUID(),
                utenlandskAdresse = nyBrevmottaker,
                fullmektig = eksisterendeBrevmottaker.fullmektig,
            )
            is RegistrertBrevmottaker.VergeMottaker -> RegistrertBrevmottaker.DefaultBrukerAdresseOgVergeMottaker(
                UUID.randomUUID(),
                defaultMottaker = eksisterendeBrevmottaker.defaultMottaker,
                verge = nyBrevmottaker,
            )
            else -> nyBrevmottaker
        }

        else -> nyBrevmottaker
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

    fun fjernManuellBrevmottaker(manuellBrevmottakerId: UUID) {
        if (registrertBrevmottaker == defaultMottaker) {
            return
        }

        val eksisterende = registrertBrevmottaker

        registrertBrevmottaker = when (eksisterende) {
            is RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker ->
                when (manuellBrevmottakerId) {
                    eksisterende.utenlandskAdresse.id -> lagDefaultBrevmottakerOgVergeMottaker(eksisterende.verge)
                    eksisterende.verge.id -> eksisterende.utenlandskAdresse
                    else -> throw IllegalArgumentException("Ugyldig brevmottaker-id: $manuellBrevmottakerId")
                }

            is RegistrertBrevmottaker.UtenlandskAdresseOgFullmektigMottaker ->
                when (manuellBrevmottakerId) {
                    eksisterende.utenlandskAdresse.id -> lagDefaultBrevmottakerOgFullmektigMottaker(eksisterende.fullmektig)
                    eksisterende.fullmektig.id -> eksisterende.utenlandskAdresse
                    else -> throw IllegalArgumentException("Ugyldig brevmottaker-id: $manuellBrevmottakerId")
                }
            is RegistrertBrevmottaker.DefaultBrukerAdresseOgVergeMottaker ->
                when (manuellBrevmottakerId) {
                    eksisterende.verge.id -> eksisterende.defaultMottaker
                    else -> throw IllegalArgumentException("Ugyldig brevmottaker-id: $manuellBrevmottakerId")
                }

            is RegistrertBrevmottaker.DefaultBrukerAdresseOgFullmektigMottaker ->
                when (manuellBrevmottakerId) {
                    eksisterende.fullmektig.id -> eksisterende.defaultMottaker
                    else -> throw IllegalArgumentException("Ugyldig brevmottaker-id: $manuellBrevmottakerId")
                }

            else -> defaultMottaker
        }
    }

    private fun lagDefaultBrevmottakerOgVergeMottaker(
        verge: RegistrertBrevmottaker.VergeMottaker,
    ): RegistrertBrevmottaker.DefaultBrukerAdresseOgVergeMottaker {
        return RegistrertBrevmottaker.DefaultBrukerAdresseOgVergeMottaker(
            id = UUID.randomUUID(),
            defaultMottaker = defaultMottaker as RegistrertBrevmottaker.DefaultMottaker,
            verge = verge,
        )
    }

    private fun lagDefaultBrevmottakerOgFullmektigMottaker(
        fullmektig: RegistrertBrevmottaker.FullmektigMottaker,
    ): RegistrertBrevmottaker.DefaultBrukerAdresseOgFullmektigMottaker {
        return RegistrertBrevmottaker.DefaultBrukerAdresseOgFullmektigMottaker(
            id = UUID.randomUUID(),
            defaultMottaker = defaultMottaker as RegistrertBrevmottaker.DefaultMottaker,
            fullmektig = fullmektig,
        )
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
