package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.FrontendDto
import no.nav.tilbakekreving.api.v1.dto.ManuellBrevmottakerResponsDto
import no.nav.tilbakekreving.kontrakter.brev.Brevmottaker
import no.nav.tilbakekreving.kontrakter.brev.ManuellAdresseInfo
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.BRUKER
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.DØDSBO
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.FULLMEKTIG
import no.nav.tilbakekreving.kontrakter.brev.MottakerType.VERGE
import no.nav.tilbakekreving.kontrakter.verge.Vergetype
import java.util.UUID

sealed interface RegistrertBrevmottaker : FrontendDto<List<ManuellBrevmottakerResponsDto>> {
    val id: UUID

    fun oppdaterRegistrertBrevmottaker(nyBrevmottaker: RegistrertBrevmottaker): RegistrertBrevmottaker {
        throw IllegalArgumentException("Ugyldig mottaker $nyBrevmottaker")
    }

    class DefaultMottaker(
        override val id: UUID = UUID.randomUUID(),
        val navn: String,
        val personIdent: String? = null,
    ) : RegistrertBrevmottaker {
        override fun oppdaterRegistrertBrevmottaker(
            nyBrevmottaker: RegistrertBrevmottaker,
        ): RegistrertBrevmottaker = when (nyBrevmottaker) {
            is VergeMottaker -> DefaultBrukerAdresseOgVergeMottaker(
                UUID.randomUUID(),
                defaultMottaker = this,
                verge = nyBrevmottaker,
            )
            is FullmektigMottaker -> DefaultBrukerAdresseOgFullmektigMottaker(
                UUID.randomUUID(),
                defaultMottaker = this,
                fullmektig = nyBrevmottaker,
            )
            is UtenlandskAdresseMottaker -> UtenlandskAdresseMottaker(
                UUID.randomUUID(),
                navn = nyBrevmottaker.navn,
                manuellAdresseInfo = nyBrevmottaker.manuellAdresseInfo,
            )
            is DødsboMottaker -> DødsboMottaker(
                UUID.randomUUID(),
                navn = nyBrevmottaker.navn,
                manuellAdresseInfo = nyBrevmottaker.manuellAdresseInfo,
            )
            else -> {
                throw IllegalArgumentException("Ugyldig mottaker $nyBrevmottaker")
            }
        }

        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return listOf(
                ManuellBrevmottakerResponsDto(
                    id = id,
                    brevmottaker = Brevmottaker(
                        type = BRUKER,
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
        val navn: String,
        val manuellAdresseInfo: ManuellAdresseInfo? = null,
    ) : RegistrertBrevmottaker {
        override fun oppdaterRegistrertBrevmottaker(
            nyBrevmottaker: RegistrertBrevmottaker,
        ): RegistrertBrevmottaker = when (nyBrevmottaker) {
            is VergeMottaker -> UtenlandskAdresseOgVergeMottaker(
                UUID.randomUUID(),
                utenlandskAdresse = this,
                verge = nyBrevmottaker,
            )

            is FullmektigMottaker -> UtenlandskAdresseOgFullmektigMottaker(
                UUID.randomUUID(),
                utenlandskAdresse = this,
                fullmektig = nyBrevmottaker,
            )

            else -> nyBrevmottaker
        }

        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return listOf(
                ManuellBrevmottakerResponsDto(
                    id = id,
                    brevmottaker = Brevmottaker(
                        type = BRUKER_MED_UTENLANDSK_ADRESSE,
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
                        type = FULLMEKTIG,
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
                        type = VERGE,
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
        val navn: String,
        val manuellAdresseInfo: ManuellAdresseInfo? = null,
    ) : RegistrertBrevmottaker {
        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return listOf(
                ManuellBrevmottakerResponsDto(
                    id = id,
                    brevmottaker = Brevmottaker(
                        type = DØDSBO,
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
        val utenlandskAdresse: UtenlandskAdresseMottaker,
        val verge: VergeMottaker,
    ) : RegistrertBrevmottaker {
        override fun oppdaterRegistrertBrevmottaker(
            nyBrevmottaker: RegistrertBrevmottaker,
        ): RegistrertBrevmottaker = when (nyBrevmottaker) {
            is UtenlandskAdresseMottaker -> UtenlandskAdresseOgVergeMottaker(
                UUID.randomUUID(),
                utenlandskAdresse = nyBrevmottaker,
                verge = verge,
            )
            is FullmektigMottaker -> UtenlandskAdresseOgFullmektigMottaker(
                UUID.randomUUID(),
                utenlandskAdresse = utenlandskAdresse,
                fullmektig = nyBrevmottaker,
            )
            else -> nyBrevmottaker
        }

        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return utenlandskAdresse.tilFrontendDto() + verge.tilFrontendDto()
        }
    }

    class UtenlandskAdresseOgFullmektigMottaker(
        override val id: UUID,
        val utenlandskAdresse: UtenlandskAdresseMottaker,
        val fullmektig: FullmektigMottaker,
    ) : RegistrertBrevmottaker {
        override fun oppdaterRegistrertBrevmottaker(
            nyBrevmottaker: RegistrertBrevmottaker,
        ): RegistrertBrevmottaker = when (nyBrevmottaker) {
            is UtenlandskAdresseMottaker -> UtenlandskAdresseOgFullmektigMottaker(
                UUID.randomUUID(),
                utenlandskAdresse = nyBrevmottaker,
                fullmektig = fullmektig,
            )
            is VergeMottaker -> UtenlandskAdresseOgVergeMottaker(
                UUID.randomUUID(),
                utenlandskAdresse = utenlandskAdresse,
                verge = nyBrevmottaker,
            )
            else -> nyBrevmottaker
        }

        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return utenlandskAdresse.tilFrontendDto() + fullmektig.tilFrontendDto()
        }
    }

    class DefaultBrukerAdresseOgVergeMottaker(
        override val id: UUID,
        val defaultMottaker: DefaultMottaker,
        val verge: VergeMottaker,
    ) : RegistrertBrevmottaker {
        override fun oppdaterRegistrertBrevmottaker(
            nyBrevmottaker: RegistrertBrevmottaker,
        ): RegistrertBrevmottaker = when (nyBrevmottaker) {
            is UtenlandskAdresseMottaker -> UtenlandskAdresseOgVergeMottaker(
                UUID.randomUUID(),
                utenlandskAdresse = nyBrevmottaker,
                verge = verge,
            )
            is FullmektigMottaker -> DefaultBrukerAdresseOgFullmektigMottaker(
                UUID.randomUUID(),
                defaultMottaker = defaultMottaker,
                fullmektig = nyBrevmottaker,
            )
            else -> nyBrevmottaker
        }

        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return defaultMottaker.tilFrontendDto() + verge.tilFrontendDto()
        }
    }

    class DefaultBrukerAdresseOgFullmektigMottaker(
        override val id: UUID,
        val defaultMottaker: DefaultMottaker,
        val fullmektig: FullmektigMottaker,
    ) : RegistrertBrevmottaker {
        override fun oppdaterRegistrertBrevmottaker(
            nyBrevmottaker: RegistrertBrevmottaker,
        ): RegistrertBrevmottaker = when (nyBrevmottaker) {
            is UtenlandskAdresseMottaker -> UtenlandskAdresseOgFullmektigMottaker(
                UUID.randomUUID(),
                utenlandskAdresse = nyBrevmottaker,
                fullmektig = fullmektig,
            )
            is VergeMottaker -> DefaultBrukerAdresseOgVergeMottaker(
                UUID.randomUUID(),
                defaultMottaker = defaultMottaker,
                verge = nyBrevmottaker,
            )
            else -> nyBrevmottaker
        }

        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return defaultMottaker.tilFrontendDto() + fullmektig.tilFrontendDto()
        }
    }
}
