package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.api.v1.dto.ManuellBrevmottakerResponsDto
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.brev.Brevmottaker
import no.nav.tilbakekreving.kontrakter.brev.ManuellAdresseInfo
import no.nav.tilbakekreving.kontrakter.brev.MottakerType
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.BRUKER
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.BRUKER_OG_FULLMEKTIG
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.BRUKER_OG_VERGE
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.DØDSBO
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.FULLMEKTIG
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.UTENLANDSK_ADRESSE_OG_FULLMEKTIG
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.UTENLANDSK_ADRESSE_OG_VERGE
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.VERGE
import no.nav.tilbakekreving.kontrakter.verge.Vergetype
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

    private fun håndterNyMottaker(ny: RegistrertBrevmottaker): RegistrertBrevmottaker =
        when (ny) {
            is RegistrertBrevmottaker.FullmektigMottaker -> RegistrertBrevmottaker.DefaultBrukerAdresseOgFullmektigMottaker(
                id = UUID.randomUUID(),
                defaultMottaker = defaultMottaker as RegistrertBrevmottaker.DefaultMottaker,
                fullmektig = ny,
            )

            is RegistrertBrevmottaker.VergeMottaker -> RegistrertBrevmottaker.DefaultBrukerAdresseOgVergeMottaker(
                id = UUID.randomUUID(),
                defaultMottaker = defaultMottaker as RegistrertBrevmottaker.DefaultMottaker,
                verge = ny,
            )

            else -> ny
        }

    private fun oppdaterRegistrertBrevmottaker(
        eksisterende: RegistrertBrevmottaker,
        ny: RegistrertBrevmottaker,
    ): RegistrertBrevmottaker = when (eksisterende) {
        is RegistrertBrevmottaker.UtenlandskAdresseMottaker -> when (ny) {
            is RegistrertBrevmottaker.VergeMottaker -> RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker(
                UUID.randomUUID(),
                utenlandskAdresse = eksisterende,
                verge = ny,
            )
            is RegistrertBrevmottaker.FullmektigMottaker -> RegistrertBrevmottaker.UtenlandskAdresseOgFullmektigMottaker(
                UUID.randomUUID(),
                utenlandskAdresse = eksisterende,
                fullmektig = ny,
            )
            else -> ny
        }

        is RegistrertBrevmottaker.DefaultBrukerAdresseOgVergeMottaker -> when (ny) {
            is RegistrertBrevmottaker.UtenlandskAdresseMottaker -> RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker(
                UUID.randomUUID(),
                utenlandskAdresse = ny,
                verge = eksisterende.verge,
            )
            is RegistrertBrevmottaker.FullmektigMottaker -> RegistrertBrevmottaker.DefaultBrukerAdresseOgFullmektigMottaker(
                UUID.randomUUID(),
                defaultMottaker = eksisterende.defaultMottaker,
                fullmektig = ny,
            )
            else -> ny
        }

        is RegistrertBrevmottaker.DefaultBrukerAdresseOgFullmektigMottaker -> when (ny) {
            is RegistrertBrevmottaker.UtenlandskAdresseMottaker -> RegistrertBrevmottaker.UtenlandskAdresseOgFullmektigMottaker(
                UUID.randomUUID(),
                utenlandskAdresse = ny,
                fullmektig = eksisterende.fullmektig,
            )
            is RegistrertBrevmottaker.VergeMottaker -> RegistrertBrevmottaker.DefaultBrukerAdresseOgVergeMottaker(
                UUID.randomUUID(),
                defaultMottaker = eksisterende.defaultMottaker,
                verge = ny,
            )
            else -> ny
        }

        else -> ny
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

    fun fjernManuelBrevmottaker(manuellBrevmottakerId: UUID) {
        if (registrertBrevmottaker == defaultMottaker) {
            return
        }

        val eksisterende = registrertBrevmottaker

        registrertBrevmottaker = when (eksisterende) {
            is RegistrertBrevmottaker.UtenlandskAdresseOgVergeMottaker ->
                when (manuellBrevmottakerId) {
                    eksisterende.utenlandskAdresse.id -> lagDefaultOgVerge(eksisterende.verge)
                    eksisterende.verge.id -> eksisterende.utenlandskAdresse
                    else -> throw IllegalArgumentException("Ugyldig brevmottaker-id: $manuellBrevmottakerId")
                }

            is RegistrertBrevmottaker.UtenlandskAdresseOgFullmektigMottaker ->
                when (manuellBrevmottakerId) {
                    eksisterende.utenlandskAdresse.id -> lagDefaultOgFullmektig(eksisterende.fullmektig)
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

    private fun lagDefaultOgVerge(
        verge: RegistrertBrevmottaker.VergeMottaker,
    ): RegistrertBrevmottaker.DefaultBrukerAdresseOgVergeMottaker {
        return RegistrertBrevmottaker.DefaultBrukerAdresseOgVergeMottaker(
            id = UUID.randomUUID(),
            defaultMottaker = defaultMottaker as RegistrertBrevmottaker.DefaultMottaker,
            verge = verge,
        )
    }

    private fun lagDefaultOgFullmektig(
        fullmektig: RegistrertBrevmottaker.FullmektigMottaker,
    ): RegistrertBrevmottaker.DefaultBrukerAdresseOgFullmektigMottaker {
        return RegistrertBrevmottaker.DefaultBrukerAdresseOgFullmektigMottaker(
            id = UUID.randomUUID(),
            defaultMottaker = defaultMottaker as RegistrertBrevmottaker.DefaultMottaker,
            fullmektig = fullmektig,
        )
    }

    sealed interface RegistrertBrevmottaker : FrontendDto<List<ManuellBrevmottakerResponsDto>> {
        val id: UUID
        val type: MottakerType

        class DefaultMottaker(
            override val id: UUID = UUID.randomUUID(),
            override val type: MottakerType = BRUKER,
            val navn: String,
            val personIdent: String? = null,
        ) : RegistrertBrevmottaker {
            override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
                return listOf(
                    ManuellBrevmottakerResponsDto(
                        id = id,
                        brevmottaker = Brevmottaker(
                            type = type,
                            vergetype = null,
                            navn = navn,
                            personIdent = personIdent,
                            manuellAdresseInfo = null,
                        ),
                    ),
                )
            }
        }

        class UtenlandskAdresseMottaker(
            override val id: UUID,
            override val type: MottakerType = BRUKER_MED_UTENLANDSK_ADRESSE,
            val navn: String,
            val manuellAdresseInfo: ManuellAdresseInfo? = null,
        ) : RegistrertBrevmottaker {
            override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
                return listOf(
                    ManuellBrevmottakerResponsDto(
                        id = id,
                        brevmottaker = Brevmottaker(
                            type = type,
                            vergetype = null,
                            navn = navn,
                            personIdent = null,
                            manuellAdresseInfo = manuellAdresseInfo,
                        ),
                    ),
                )
            }
        }

        class FullmektigMottaker(
            override val id: UUID,
            override val type: MottakerType = FULLMEKTIG,
            val navn: String,
            val organisasjonsnummer: String? = null,
            val personIdent: String? = null,
            val manuellAdresseInfo: ManuellAdresseInfo? = null,
        ) : RegistrertBrevmottaker {
            override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
                return listOf(
                    ManuellBrevmottakerResponsDto(
                        id = id,
                        brevmottaker = Brevmottaker(
                            type = type,
                            vergetype = null,
                            navn = navn,
                            organisasjonsnummer = organisasjonsnummer,
                            personIdent = personIdent,
                            manuellAdresseInfo = manuellAdresseInfo,
                        ),
                    ),
                )
            }
        }

        class VergeMottaker(
            override val id: UUID,
            override val type: MottakerType = VERGE,
            val navn: String,
            val vergetype: Vergetype? = null,
            val personIdent: String? = null,
            val manuellAdresseInfo: ManuellAdresseInfo? = null,
        ) : RegistrertBrevmottaker {
            override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
                return listOf(
                    ManuellBrevmottakerResponsDto(
                        id = id,
                        brevmottaker = Brevmottaker(
                            type = type,
                            vergetype = vergetype,
                            navn = navn,
                            organisasjonsnummer = null,
                            personIdent = personIdent,
                            manuellAdresseInfo = manuellAdresseInfo,
                        ),
                    ),
                )
            }
        }

        class DødsboMottaker(
            override val id: UUID,
            override val type: MottakerType = DØDSBO,
            val navn: String,
            val manuellAdresseInfo: ManuellAdresseInfo? = null,
        ) : RegistrertBrevmottaker {
            override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
                return listOf(
                    ManuellBrevmottakerResponsDto(
                        id = id,
                        brevmottaker = Brevmottaker(
                            type = type,
                            vergetype = null,
                            navn = navn,
                            organisasjonsnummer = null,
                            personIdent = null,
                            manuellAdresseInfo = manuellAdresseInfo,
                        ),
                    ),
                )
            }
        }

        class UtenlandskAdresseOgVergeMottaker(
            override val id: UUID,
            override val type: MottakerType = UTENLANDSK_ADRESSE_OG_VERGE,
            val utenlandskAdresse: UtenlandskAdresseMottaker,
            val verge: VergeMottaker,
        ) : RegistrertBrevmottaker {
            override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
                return utenlandskAdresse.tilFrontendDto() + verge.tilFrontendDto()
            }
        }

        class UtenlandskAdresseOgFullmektigMottaker(
            override val id: UUID,
            override val type: MottakerType = UTENLANDSK_ADRESSE_OG_FULLMEKTIG,
            val utenlandskAdresse: UtenlandskAdresseMottaker,
            val fullmektig: FullmektigMottaker,
        ) : RegistrertBrevmottaker {
            override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
                return utenlandskAdresse.tilFrontendDto() + fullmektig.tilFrontendDto()
            }
        }

        class DefaultBrukerAdresseOgVergeMottaker(
            override val id: UUID,
            override val type: MottakerType = BRUKER_OG_VERGE,
            val defaultMottaker: DefaultMottaker,
            val verge: VergeMottaker,
        ) : RegistrertBrevmottaker {
            override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
                return defaultMottaker.tilFrontendDto() + verge.tilFrontendDto()
            }
        }

        class DefaultBrukerAdresseOgFullmektigMottaker(
            override val id: UUID,
            override val type: MottakerType = BRUKER_OG_FULLMEKTIG,
            val defaultMottaker: DefaultMottaker,
            val fullmektig: FullmektigMottaker,
        ) : RegistrertBrevmottaker {
            override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
                return defaultMottaker.tilFrontendDto() + fullmektig.tilFrontendDto()
            }
        }
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
