package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.Klokke
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.behandling.saksbehandling.Saksbehandlingsteg
import no.nav.tilbakekreving.behandling.saksbehandling.Venter
import no.nav.tilbakekreving.behandling.saksbehandling.ÅrsakTilTilbakeføring
import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler
import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.eksternfagsak.EksternFagsakRevurdering
import no.nav.tilbakekreving.entities.ForhåndsvarselEntity
import no.nav.tilbakekreving.hendelse.KravgrunnlagHendelse
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselErSendtDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselResponseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.IkkeVurdertDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelseVurderingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UttalelsesfristDto
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagSammenligning.OverordnetSammendrag
import java.time.LocalDate
import java.util.UUID

class Forhåndsvarsel internal constructor(
    private var vurdering: Vurdering,
) : Saksbehandlingsteg {
    override val type: Behandlingssteg = Behandlingssteg.FORHÅNDSVARSEL

    override val behandlingsstatus: BehandlingsstatusModell get() = vurdering.behandlingsstatus

    override fun erFullstendig(klokke: Klokke): Boolean = vurdering.erFullstendig()

    override fun erPåbegynt(): Boolean = vurdering.erPåbegynt()

    override fun trengerNyVurdering(): ÅrsakTilTilbakeføring? = vurdering.trengerNyVurdering()

    override fun underkjennSteget() {
        vurdering.underkjenn()
    }

    override fun nullstill(kravgrunnlag: KravgrunnlagHendelse, eksternFagsakRevurdering: EksternFagsakRevurdering) {}

    override fun venter(klokke: Klokke): Venter? = vurdering.venter(klokke)

    fun tilEntity(behandlingRef: UUID): ForhåndsvarselEntity = vurdering.tilEntity(behandlingRef)

    fun nullstillUnntakOgUttalelse() {
        vurdering = vurdering.nullstillUnntakOgUttalelse()
    }

    fun lagreUttalelse(
        uttalelseVurdering: UttalelseVurdering,
        uttalelseInfo: UttalelseInfo?,
        kommentar: String?,
    ) {
        vurdering.lagreUttalelse(
            Brukeruttalelse(
                id = UUID.randomUUID(),
                uttalelseVurdering = uttalelseVurdering,
                uttalelseInfo = uttalelseInfo,
                kommentar = kommentar,
                tilbakeført = null,
            ),
        )
    }

    fun lagreOpprinneligFrist(opprinneligFrist: LocalDate) {
        vurdering = vurdering.lagreOpprinneligFrist(
            Uttalelsesfrist(
                id = UUID.randomUUID(),
                opprinneligFrist = opprinneligFrist,
                nyFrist = null,
                begrunnelse = null,
            ),
        )
    }

    fun lagreFristUtsettelse(nyFrist: LocalDate, begrunnelse: String): UttalelsesfristDto = vurdering.lagreFristUtsettelse(nyFrist, begrunnelse)

    fun lagreForhåndsvarselUnntak(
        begrunnelseForUnntak: BegrunnelseForUnntak,
        beskrivelse: String,
    ) {
        vurdering = vurdering.lagreForhåndsvarselUnntak(
            ForhåndsvarselUnntak(
                id = UUID.randomUUID(),
                begrunnelseForUnntak = begrunnelseForUnntak,
                beskrivelse = beskrivelse,
                tilbakeført = null,
            ),
        )
    }

    override fun meldingerTilSaksbehandler(): Set<MeldingTilSaksbehandler> = vurdering.meldingerTilSaksbehandler()

    override fun nyttKravgrunnlagMottatt(sammendrag: OverordnetSammendrag) {
        vurdering.nyttKravgrunnlagMottatt(sammendrag)
    }

    fun nyForhåndsvarselTilFrontend(varselbrev: Varselbrev?): ForhaandsvarselResponseDto {
        return vurdering.tilFrontendDto(varselbrev)
    }

    fun erForhåndsvarselSendt(): Boolean? = vurdering.erForhåndsvarselSendt()

    companion object {
        fun opprett(): Forhåndsvarsel {
            return Forhåndsvarsel(IkkeVurdert)
        }
    }

    internal sealed interface Vurdering {
        val behandlingsstatus: BehandlingsstatusModell

        fun erFullstendig(): Boolean

        fun erPåbegynt(): Boolean

        fun trengerNyVurdering(): ÅrsakTilTilbakeføring?

        fun underkjenn()

        fun venter(klokke: Klokke): Venter?

        fun nullstillUnntakOgUttalelse(): Vurdering

        fun lagreUttalelse(brukeruttalelse: Brukeruttalelse)

        fun lagreOpprinneligFrist(uttalelsesfrist: Uttalelsesfrist): Vurdering

        fun lagreFristUtsettelse(nyFrist: LocalDate, begrunnelse: String): UttalelsesfristDto {
            error("Kan ikke utsette uttalelsesfrist uten at forhåndsvarsel er sendt")
        }

        fun lagreForhåndsvarselUnntak(forhåndsvarselUnntak: ForhåndsvarselUnntak): Vurdering

        fun meldingerTilSaksbehandler(): Set<MeldingTilSaksbehandler>

        fun nyttKravgrunnlagMottatt(sammendrag: OverordnetSammendrag)

        fun tilFrontendDto(varselbrev: Varselbrev?): ForhaandsvarselResponseDto

        fun erForhåndsvarselSendt(): Boolean?

        fun tilEntity(behandlingRef: UUID): ForhåndsvarselEntity
    }

    internal data object IkkeVurdert : Vurdering {
        override val behandlingsstatus = BehandlingsstatusModell.TIL_FORHÅNDSVARSEL

        override fun erFullstendig() = false

        override fun erPåbegynt() = false

        override fun trengerNyVurdering(): ÅrsakTilTilbakeføring? = null

        override fun underkjenn() {}

        override fun venter(klokke: Klokke): Venter? = null

        override fun nullstillUnntakOgUttalelse(): Vurdering = this

        override fun lagreUttalelse(brukeruttalelse: Brukeruttalelse) {
            error("Kan ikke registrere brukeruttalelse uten forhåndsvarsel eller unntak")
        }

        override fun lagreOpprinneligFrist(uttalelsesfrist: Uttalelsesfrist): Vurdering = VarselSendt(uttalelsesfrist, null)

        override fun lagreForhåndsvarselUnntak(forhåndsvarselUnntak: ForhåndsvarselUnntak): Vurdering = Unntak(forhåndsvarselUnntak, null)

        override fun meldingerTilSaksbehandler(): Set<MeldingTilSaksbehandler> = emptySet()

        override fun nyttKravgrunnlagMottatt(sammendrag: OverordnetSammendrag) {}

        override fun tilFrontendDto(varselbrev: Varselbrev?) = ForhaandsvarselResponseDto(
            forhaandsvarselSteg = IkkeVurdertDto,
            brukeruttalelse = null,
        )

        override fun erForhåndsvarselSendt(): Boolean? = null

        override fun tilEntity(behandlingRef: UUID) = ForhåndsvarselEntity(
            brukeruttalelseEntity = null,
            forhåndsvarselUnntakEntity = null,
            uttalelsesfristEntity = null,
        )
    }

    internal class VarselSendt(
        private var uttalelsesfrist: Uttalelsesfrist,
        private var brukeruttalelse: Brukeruttalelse?,
    ) : Vurdering {
        override val behandlingsstatus = BehandlingsstatusModell.TIL_BEHANDLING

        override fun erFullstendig() = brukeruttalelse != null

        override fun erPåbegynt() = true

        override fun trengerNyVurdering(): ÅrsakTilTilbakeføring? = brukeruttalelse?.tilbakeført()

        override fun underkjenn() {
            brukeruttalelse?.vurderPåNytt(ÅrsakTilTilbakeføring.Underkjent)
        }

        override fun venter(klokke: Klokke): Venter? {
            return uttalelsesfrist.gjeldendeFrist(klokke)?.let {
                Venter(
                    grunn = Venter.Grunn.BRUKERUTTALELSE,
                    frist = it,
                )
            }
        }

        override fun nullstillUnntakOgUttalelse(): Vurdering {
            brukeruttalelse = null
            return this
        }

        override fun lagreUttalelse(brukeruttalelse: Brukeruttalelse) {
            this.brukeruttalelse = brukeruttalelse
        }

        override fun lagreOpprinneligFrist(uttalelsesfrist: Uttalelsesfrist): Vurdering {
            this.uttalelsesfrist = uttalelsesfrist
            return this
        }

        override fun lagreFristUtsettelse(nyFrist: LocalDate, begrunnelse: String): UttalelsesfristDto {
            uttalelsesfrist.utsettFrist(nyFrist, begrunnelse)
            return uttalelsesfrist.nyTilFrontendDto()
        }

        override fun lagreForhåndsvarselUnntak(forhåndsvarselUnntak: ForhåndsvarselUnntak): Vurdering {
            error("Kan ikke registrere unntak etter at forhåndsvarsel er sendt")
        }

        override fun meldingerTilSaksbehandler(): Set<MeldingTilSaksbehandler> = brukeruttalelse?.meldingerTilSaksbehandler() ?: emptySet()

        override fun nyttKravgrunnlagMottatt(sammendrag: OverordnetSammendrag) {}

        override fun tilFrontendDto(varselbrev: Varselbrev?): ForhaandsvarselResponseDto {
            val forhåndsvarsel = requireNotNull(varselbrev) { "Varselbrev må finnes når forhåndsvarsel er sendt" }
            return ForhaandsvarselResponseDto(
                forhaandsvarselSteg = ForhaandsvarselErSendtDto(
                    forhåndsvarselInfo = forhåndsvarsel.tilForhåndsvarselDto(),
                    uttalelsesfrist = uttalelsesfrist.nyTilFrontendDto(),
                    ferdigvurdert = erFullstendig(),
                    tilbakeført = trengerNyVurdering()?.frontendDto,
                ),
                brukeruttalelse = brukeruttalelse?.nyTilFrontendDto()
                    ?: UttalelseDto(harBrukerUttaltSeg = UttalelseVurderingDto.IKKE_VURDERT),
            )
        }

        override fun erForhåndsvarselSendt(): Boolean = true

        override fun tilEntity(behandlingRef: UUID) = ForhåndsvarselEntity(
            brukeruttalelseEntity = brukeruttalelse?.tilEntity(behandlingRef),
            forhåndsvarselUnntakEntity = null,
            uttalelsesfristEntity = uttalelsesfrist.tilEntity(behandlingRef),
        )
    }

    internal class Unntak(
        private var forhåndsvarselUnntak: ForhåndsvarselUnntak,
        private var brukeruttalelse: Brukeruttalelse?,
    ) : Vurdering {
        override val behandlingsstatus = BehandlingsstatusModell.TIL_BEHANDLING

        override fun erFullstendig() = true

        override fun erPåbegynt() = true

        override fun trengerNyVurdering(): ÅrsakTilTilbakeføring? {
            return brukeruttalelse?.tilbakeført() ?: forhåndsvarselUnntak.tilbakeført()
        }

        override fun underkjenn() {
            brukeruttalelse?.vurderPåNytt(ÅrsakTilTilbakeføring.Underkjent)
            forhåndsvarselUnntak.vurderPåNytt(ÅrsakTilTilbakeføring.Underkjent)
        }

        override fun venter(klokke: Klokke): Venter? = null

        override fun nullstillUnntakOgUttalelse(): Vurdering = IkkeVurdert

        override fun lagreUttalelse(brukeruttalelse: Brukeruttalelse) {
            this.brukeruttalelse = brukeruttalelse
        }

        override fun lagreOpprinneligFrist(uttalelsesfrist: Uttalelsesfrist): Vurdering {
            return VarselSendt(
                uttalelsesfrist = uttalelsesfrist,
                brukeruttalelse = brukeruttalelse,
            )
        }

        override fun lagreForhåndsvarselUnntak(forhåndsvarselUnntak: ForhåndsvarselUnntak): Vurdering {
            this.forhåndsvarselUnntak = forhåndsvarselUnntak
            if (!forhåndsvarselUnntak.skalBeholdeBrukeruttalelse()) {
                brukeruttalelse = null
            }
            return this
        }

        override fun meldingerTilSaksbehandler(): Set<MeldingTilSaksbehandler> = brukeruttalelse?.meldingerTilSaksbehandler() ?: emptySet()

        override fun nyttKravgrunnlagMottatt(sammendrag: OverordnetSammendrag) {
            if (sammendrag.gammeltBeløp < sammendrag.nyttBeløp) {
                forhåndsvarselUnntak.vurderPåNytt(ÅrsakTilTilbakeføring.NyttKravgrunnlag)
            }
        }

        override fun tilFrontendDto(varselbrev: Varselbrev?) = ForhaandsvarselResponseDto(
            forhaandsvarselSteg = forhåndsvarselUnntak.nyTilFrontendDto(erFullstendig()),
            brukeruttalelse = brukeruttalelse?.nyTilFrontendDto(),
        )

        override fun erForhåndsvarselSendt(): Boolean = false

        override fun tilEntity(behandlingRef: UUID) = ForhåndsvarselEntity(
            brukeruttalelseEntity = brukeruttalelse?.tilEntity(behandlingRef),
            forhåndsvarselUnntakEntity = forhåndsvarselUnntak.tilEntity(behandlingRef),
            uttalelsesfristEntity = null,
        )
    }
}
