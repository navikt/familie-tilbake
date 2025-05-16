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

    fun oppdaterRegistrertBrevmottaker(nyBrevmottaker: RegistrertBrevmottaker): RegistrertBrevmottaker

    fun kombiner(annen: VergeMottaker): RegistrertBrevmottaker {
        throw IllegalArgumentException("Ugyldig mottaker $annen")
    }

    fun kombiner(annen: FullmektigMottaker): RegistrertBrevmottaker {
        throw IllegalArgumentException("Ugyldig mottaker $annen")
    }

    fun kombiner(annen: DefaultMottaker): RegistrertBrevmottaker = this

    fun kombiner(annen: UtenlandskAdresseMottaker): RegistrertBrevmottaker {
        throw IllegalArgumentException("Ugyldig mottaker $annen")
    }

    fun kombiner(annen: DødsboMottaker): RegistrertBrevmottaker {
        throw IllegalArgumentException("Ugyldig mottaker $annen")
    }

    class DefaultMottaker(
        override val id: UUID = UUID.randomUUID(),
        val navn: String,
        val personIdent: String? = null,
    ) : RegistrertBrevmottaker {
        override fun oppdaterRegistrertBrevmottaker(
            nyBrevmottaker: RegistrertBrevmottaker,
        ): RegistrertBrevmottaker = nyBrevmottaker.kombiner(this)

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
        ): RegistrertBrevmottaker = nyBrevmottaker.kombiner(this)

        override fun kombiner(annen: FullmektigMottaker): RegistrertBrevmottaker {
            return UtenlandskAdresseOgFullmektigMottaker(
                id = UUID.randomUUID(),
                utenlandskAdresse = this,
                fullmektig = annen,
            )
        }

        override fun kombiner(annen: VergeMottaker): RegistrertBrevmottaker {
            return UtenlandskAdresseOgVergeMottaker(
                id = UUID.randomUUID(),
                utenlandskAdresse = this,
                verge = annen,
            )
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

        override fun kombiner(annen: DefaultMottaker): RegistrertBrevmottaker {
            return DefaultBrukerAdresseOgFullmektigMottaker(
                id = UUID.randomUUID(),
                defaultMottaker = annen,
                fullmektig = this,
            )
        }

        override fun kombiner(annen: UtenlandskAdresseMottaker): RegistrertBrevmottaker {
            return UtenlandskAdresseOgFullmektigMottaker(
                id = UUID.randomUUID(),
                utenlandskAdresse = annen,
                fullmektig = this,
            )
        }

        override fun oppdaterRegistrertBrevmottaker(nyBrevmottaker: RegistrertBrevmottaker): RegistrertBrevmottaker = nyBrevmottaker.kombiner(this)
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

        override fun kombiner(annen: DefaultMottaker): RegistrertBrevmottaker {
            return DefaultBrukerAdresseOgVergeMottaker(
                id = UUID.randomUUID(),
                defaultMottaker = annen,
                verge = this,
            )
        }

        override fun kombiner(annen: UtenlandskAdresseMottaker): RegistrertBrevmottaker {
            return UtenlandskAdresseOgVergeMottaker(
                id = UUID.randomUUID(),
                utenlandskAdresse = annen,
                verge = this
            )
        }

        override fun oppdaterRegistrertBrevmottaker(nyBrevmottaker: RegistrertBrevmottaker): RegistrertBrevmottaker = nyBrevmottaker.kombiner(this)
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

        override fun kombiner(annen: VergeMottaker): RegistrertBrevmottaker {
            return this
        }

        override fun kombiner(annen: UtenlandskAdresseMottaker): RegistrertBrevmottaker {
            return this
        }

        override fun kombiner(annen: FullmektigMottaker): RegistrertBrevmottaker {
            return this
        }

        override fun oppdaterRegistrertBrevmottaker(nyBrevmottaker: RegistrertBrevmottaker): RegistrertBrevmottaker = nyBrevmottaker.kombiner(this)
    }

    class UtenlandskAdresseOgVergeMottaker(
        override val id: UUID,
        val utenlandskAdresse: UtenlandskAdresseMottaker,
        val verge: VergeMottaker,
    ) : RegistrertBrevmottaker {
        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return utenlandskAdresse.tilFrontendDto() + verge.tilFrontendDto()
        }

        override fun oppdaterRegistrertBrevmottaker(nyBrevmottaker: RegistrertBrevmottaker): RegistrertBrevmottaker = nyBrevmottaker.kombiner(utenlandskAdresse)
    }

    class UtenlandskAdresseOgFullmektigMottaker(
        override val id: UUID,
        val utenlandskAdresse: UtenlandskAdresseMottaker,
        val fullmektig: FullmektigMottaker,
    ) : RegistrertBrevmottaker {
        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return utenlandskAdresse.tilFrontendDto() + fullmektig.tilFrontendDto()
        }

        override fun oppdaterRegistrertBrevmottaker(nyBrevmottaker: RegistrertBrevmottaker): RegistrertBrevmottaker = nyBrevmottaker.kombiner(utenlandskAdresse)
    }

    class DefaultBrukerAdresseOgVergeMottaker(
        override val id: UUID,
        val defaultMottaker: DefaultMottaker,
        val verge: VergeMottaker,
    ) : RegistrertBrevmottaker {
        override fun oppdaterRegistrertBrevmottaker(nyBrevmottaker: RegistrertBrevmottaker): RegistrertBrevmottaker = nyBrevmottaker.kombiner(verge)

        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return defaultMottaker.tilFrontendDto() + verge.tilFrontendDto()
        }
    }

    class DefaultBrukerAdresseOgFullmektigMottaker(
        override val id: UUID,
        val defaultMottaker: DefaultMottaker,
        val fullmektig: FullmektigMottaker,
    ) : RegistrertBrevmottaker {
        override fun oppdaterRegistrertBrevmottaker(nyBrevmottaker: RegistrertBrevmottaker): RegistrertBrevmottaker = nyBrevmottaker.kombiner(fullmektig)

        override fun tilFrontendDto(): List<ManuellBrevmottakerResponsDto> {
            return defaultMottaker.tilFrontendDto() + fullmektig.tilFrontendDto()
        }
    }
}
