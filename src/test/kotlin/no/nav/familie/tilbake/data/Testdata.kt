package no.nav.familie.tilbake.data

import no.nav.familie.kontrakter.felles.tilbakekreving.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultat
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Bruker
import no.nav.familie.tilbake.behandling.domain.Fagsak
import no.nav.familie.tilbake.behandling.domain.Fagsystemsbehandling
import no.nav.familie.tilbake.behandling.domain.Fagsystemsbehandlingsårsak
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.behandling.domain.Varselsperiode
import no.nav.familie.tilbake.behandling.domain.Verge
import no.nav.familie.tilbake.behandling.domain.Vergetype
import no.nav.familie.tilbake.domain.tbd.Aksjonspunkt
import no.nav.familie.tilbake.domain.tbd.Aksjonspunktsdefinisjon
import no.nav.familie.tilbake.domain.tbd.Aksjonspunktsstatus
import no.nav.familie.tilbake.domain.tbd.Aktsomhet
import no.nav.familie.tilbake.domain.tbd.Behandlingsstegstilstand
import no.nav.familie.tilbake.domain.tbd.Behandlingsstegstype
import no.nav.familie.tilbake.domain.tbd.Behandlingstegsstatus
import no.nav.familie.tilbake.domain.tbd.Behandlingsvedtak
import no.nav.familie.tilbake.domain.tbd.Behandlingsårsak
import no.nav.familie.tilbake.domain.tbd.Behandlingsårsakstype
import no.nav.familie.tilbake.domain.tbd.Brevsporing
import no.nav.familie.tilbake.domain.tbd.Brevtype
import no.nav.familie.tilbake.domain.tbd.Fagområdekode
import no.nav.familie.tilbake.domain.tbd.FaktaFeilutbetaling
import no.nav.familie.tilbake.domain.tbd.FaktaFeilutbetalingsperiode
import no.nav.familie.tilbake.domain.tbd.Foreldelsesperiode
import no.nav.familie.tilbake.domain.tbd.Foreldelsesvurderingstype
import no.nav.familie.tilbake.domain.tbd.Friteksttype
import no.nav.familie.tilbake.domain.tbd.GjelderType
import no.nav.familie.tilbake.domain.tbd.GrupperingFaktaFeilutbetaling
import no.nav.familie.tilbake.domain.tbd.GrupperingKravGrunnlag
import no.nav.familie.tilbake.domain.tbd.GrupperingKravvedtaksstatus
import no.nav.familie.tilbake.domain.tbd.GrupperingVurdertForeldelse
import no.nav.familie.tilbake.domain.tbd.Hendelsestype
import no.nav.familie.tilbake.domain.tbd.Hendelsesundertype
import no.nav.familie.tilbake.domain.tbd.Klassekode
import no.nav.familie.tilbake.domain.tbd.Klassetype
import no.nav.familie.tilbake.domain.tbd.Kravgrunnlag431
import no.nav.familie.tilbake.domain.tbd.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.domain.tbd.Kravgrunnlagsperiode432
import no.nav.familie.tilbake.domain.tbd.Kravstatuskode
import no.nav.familie.tilbake.domain.tbd.Kravvedtaksstatus437
import no.nav.familie.tilbake.domain.tbd.Meldingstype
import no.nav.familie.tilbake.domain.tbd.MottakersVarselrespons
import no.nav.familie.tilbake.domain.tbd.Navoppfulgt
import no.nav.familie.tilbake.domain.tbd.Revurderingsårsak
import no.nav.familie.tilbake.domain.tbd.SærligGrunn
import no.nav.familie.tilbake.domain.tbd.Totrinnsresultatsgrunnlag
import no.nav.familie.tilbake.domain.tbd.Totrinnsvurdering
import no.nav.familie.tilbake.domain.tbd.Vedtaksbrevsoppsummering
import no.nav.familie.tilbake.domain.tbd.Vedtaksbrevsperiode
import no.nav.familie.tilbake.domain.tbd.Vilkårsvurdering
import no.nav.familie.tilbake.domain.tbd.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.domain.tbd.VilkårsvurderingGodTro
import no.nav.familie.tilbake.domain.tbd.VilkårsvurderingSærligGrunn
import no.nav.familie.tilbake.domain.tbd.Vilkårsvurderingsperiode
import no.nav.familie.tilbake.domain.tbd.Vilkårsvurderingsresultat
import no.nav.familie.tilbake.domain.tbd.VurdertForeldelse
import no.nav.familie.tilbake.domain.tbd.ÅrsakTotrinnsvurdering
import no.nav.familie.tilbake.domain.tbd.Årsakstype
import no.nav.familie.tilbake.domain.tbd.ØkonomiXmlMottatt
import no.nav.familie.tilbake.domain.tbd.ØkonomiXmlMottattArkiv
import no.nav.familie.tilbake.domain.tbd.ØkonomiXmlSendt
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

object Testdata {

    private val bruker = Bruker(ident = "321321321")

    val fagsak = Fagsak(ytelsestype = Ytelsestype.BARNETRYGD,
                        fagsystem = Fagsystem.BA,
                        eksternFagsakId = "testverdi",
                        bruker = bruker)

    private val fagsystemsbehandling = Fagsystemsbehandling(
            eksternId = UUID.randomUUID().toString(),
            tilbakekrevingsvalg = Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL,
            resultat = "OPPHØR",
            årsaker = setOf(Fagsystemsbehandlingsårsak(årsak = "testverdi"))
    )
    private val date = LocalDate.now()

    private val varsel = Varsel(varseltekst = "testverdi",
                                varselbeløp = 123,
                                revurderingsvedtaksdato = date.minusDays(1),
                                perioder = setOf(Varselsperiode(fom = date.minusMonths(2), tom = date)))

    private val verge = Verge(ident = "testverdi",
                              gyldigFom = LocalDate.now(),
                              gyldigTom = LocalDate.now(),
                              type = Vergetype.VERGE_FOR_BARN,
                              orgNr = "testverdi",
                              navn = "testverdi",
                              kilde = "testverdi",
                              begrunnelse = "testverdi")

    private val behandlingsvedtak = Behandlingsvedtak(vedtaksdato = LocalDate.now(),
                                                      ansvarligSaksbehandler = "testverdi")

    private val behandlingsresultat = Behandlingsresultat(behandlingsvedtak = setOf(behandlingsvedtak))

    val behandling = Behandling(fagsakId = fagsak.id,
                                type = Behandlingstype.TILBAKEKREVING,
                                opprettetDato = LocalDate.now(),
                                avsluttetDato = LocalDate.now(),
                                ansvarligSaksbehandler = "testverdi",
                                ansvarligBeslutter = "testverdi",
                                behandlendeEnhet = "testverdi",
                                behandlendeEnhetsNavn = "testverdi",
                                manueltOpprettet = true,
                                fagsystemsbehandling = setOf(fagsystemsbehandling),
                                resultater = setOf(behandlingsresultat),
                                varsler = setOf(varsel),
                                verger = setOf(verge),
                                eksternBrukId = UUID.randomUUID())

    val behandlingsårsak = Behandlingsårsak(behandlingId = behandling.id,
                                            type = Behandlingsårsakstype.REVURDERING_KLAGE_KA,
                                            originalBehandlingId = null)

    val aksjonspunkt = Aksjonspunkt(totrinnsbehandling = true,
                                    behandlingsstegstype = Behandlingsstegstype.FAKTA_OM_VERGE,
                                    aksjonspunktsdefinisjon = Aksjonspunktsdefinisjon.FATTE_VEDTAK,
                                    behandlingId = behandling.id,
                                    tidsfrist = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
                                    status = Aksjonspunktsstatus.OPPRETTET)

    val revurderingsårsak = Revurderingsårsak(aksjonspunktId = aksjonspunkt.id,
                                              årsakstype = Årsakstype.FEIL_LOV)

    val behandlingsstegstilstand = Behandlingsstegstilstand(behandlingId = behandling.id,
                                                            behandlingsstegstype = Behandlingsstegstype.FATTE_VEDTAK,
                                                            behandlingsstegsstatus = Behandlingstegsstatus.INNGANG)


    val totrinnsvurdering = Totrinnsvurdering(behandlingId = behandling.id,
                                              aksjonspunktsdefinisjon = Aksjonspunktsdefinisjon.FATTE_VEDTAK,
                                              godkjent = true,
                                              begrunnelse = "testverdi")

    val årsakTotrinnsvurdering = ÅrsakTotrinnsvurdering(årsakstype = Årsakstype.ANNET,
                                                        totrinnsvurderingId = totrinnsvurdering.id)

    val mottakersVarselrespons = MottakersVarselrespons(behandlingId = behandling.id,
                                                        akseptertFaktagrunnlag = true,
                                                        kilde = "testverdi")

    val vurdertForeldelse = VurdertForeldelse(behandlingId = behandling.id)

    val grupperingVurdertForeldelse = GrupperingVurdertForeldelse(vurdertForeldelseId = vurdertForeldelse.id,
                                                                  behandlingId = behandling.id)

    val foreldelsesperiode = Foreldelsesperiode(vurdertForeldelseId = vurdertForeldelse.id,
                                                fom = LocalDate.now(),
                                                tom = LocalDate.now(),
                                                foreldelsesvurderingstype = Foreldelsesvurderingstype.IKKE_FORELDET,
                                                begrunnelse = "testverdi",
                                                foreldelsesfrist = LocalDate.now(),
                                                oppdagelsesdato = LocalDate.now())

    private val kravgrunnlagsbeløp433 = Kravgrunnlagsbeløp433(klassekode = Klassekode.FPADATORD,
                                                              klassetype = Klassetype.JUST,
                                                              opprinneligUtbetalingsbeløp = 123.11,
                                                              nyttBeløp = 123.11,
                                                              tilbakekrevesBeløp = 123.11,
                                                              uinnkrevdBeløp = 123.11,
                                                              resultatkode = "testverdi",
                                                              årsakskode = "testverdi",
                                                              skyldkode = "testverdi",
                                                              skatteprosent = 35.11)

    private val kravgrunnlagsperiode432 = Kravgrunnlagsperiode432(fom = LocalDate.now(),
                                                                  tom = LocalDate.now(),
                                                                  beløp = setOf(kravgrunnlagsbeløp433),
                                                                  månedligSkattebeløp = 123.11)

    val kravgrunnlag431 = Kravgrunnlag431(vedtakId = "testverdi",
                                          kravstatuskode = "testverdi",
                                          fagområdekode = Fagområdekode.UKJENT,
                                          fagsystem = no.nav.familie.tilbake.domain.tbd.Fagsystem.ARENA,
                                          fagsystemVedtaksdato = LocalDate.now(),
                                          omgjortVedtakId = "testverdi",
                                          gjelderVedtakId = "testverdi",
                                          gjelderType = GjelderType.APPLIKASJONSBRUKER,
                                          utbetalesTilId = "testverdi",
                                          hjemmelkode = "testverdi",
                                          beregnesRenter = true,
                                          ansvarligEnhet = "testverdi",
                                          bostedsenhet = "testverdi",
                                          behandlingsenhet = "testverdi",
                                          kontrollfelt = "testverdi",
                                          saksbehandlerId = "testverdi",
                                          referanse = "testverdi",
                                          eksternKravgrunnlagId = "testverdi",
                                          perioder = setOf(kravgrunnlagsperiode432))

    val kravvedtaksstatus437 = Kravvedtaksstatus437(vedtakId = "testverdi",
                                                    kravstatuskode = Kravstatuskode.ANNULERT,
                                                    fagområdekode = Fagområdekode.UKJENT,
                                                    fagsystemId = "testverdi",
                                                    gjelderVedtakId = "testverdi",
                                                    gjelderType = GjelderType.ORGANISASJON,
                                                    referanse = "testverdi")

    val grupperingKravGrunnlag = GrupperingKravGrunnlag(kravgrunnlag431Id = kravgrunnlag431.id,
                                                        behandlingId = behandling.id,
                                                        aktiv = true,
                                                        sperret = true)

    private val vilkårsvurderingSærligGrunn = VilkårsvurderingSærligGrunn(særligGrunn = SærligGrunn.GRAD_AV_UAKTSOMHET,
                                                                          begrunnelse = "testverdi")

    private val vilkårsvurderingGodTro = VilkårsvurderingGodTro(beløpErIBehold = true,
                                                                beløpTilbakekreves = 32165,
                                                                begrunnelse = "testverdi")

    private val vilkårsvurderingAktsomhet =
            VilkårsvurderingAktsomhet(aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                                      ileggRenter = true,
                                      andelTilbakekreves = 123.11,
                                      manueltSattBeløp = null,
                                      begrunnelse = "testverdi",
                                      særligeGrunnerTilReduksjon = true,
                                      tilbakekrevSmabeløp = true,
                                      særligeGrunnerBegrunnelse = "testverdi",
                                      vilkårsvurderingSærligeGrunner = setOf(vilkårsvurderingSærligGrunn))

    private val vilkårsperiode =
            Vilkårsvurderingsperiode(fom = LocalDate.now(),
                                     tom = LocalDate.now(),
                                     navoppfulgt = Navoppfulgt.HAR_IKKE_FULGT_OPP,
                                     vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                                     begrunnelse = "testverdi",
                                     vilkårsvurderingAktsomheter = setOf(vilkårsvurderingAktsomhet),
                                     vilkårsvurderingGodTro = setOf(vilkårsvurderingGodTro))

    val vilkår = Vilkårsvurdering(behandlingId = behandling.id,
                                  perioder = setOf(vilkårsperiode))

    val faktaFeilutbetaling = FaktaFeilutbetaling(begrunnelse = "testverdi")

    val faktaFeilutbetalingsperiode = FaktaFeilutbetalingsperiode(faktaFeilutbetalingId = faktaFeilutbetaling.id,
                                                                  fom = LocalDate.now(),
                                                                  tom = LocalDate.now(),
                                                                  hendelsestype = Hendelsestype.BA_ANNET,
                                                                  hendelsesundertype = Hendelsesundertype.ENDRET_DEKNINGSGRAD)

    val grupperingFaktaFeilutbetaling = GrupperingFaktaFeilutbetaling(behandlingId = behandling.id,
                                                                      faktaFeilutbetalingId = faktaFeilutbetaling.id)

    val økonomiXmlMottatt = ØkonomiXmlMottatt(melding = "testverdi",
                                              sekvens = 1,
                                              tilkoblet = true,
                                              eksternFagsakId = "testverdi",
                                              henvisning = "testverdi")

    val totrinnsresultatsgrunnlag = Totrinnsresultatsgrunnlag(behandlingId = behandling.id,
                                                              grupperingFaktaFeilutbetalingId = grupperingFaktaFeilutbetaling.id,
                                                              grupperingVurdertForeldelseId = grupperingVurdertForeldelse.id,
                                                              vilkårsvurderingId = vilkår.id)

    val vedtaksbrevsoppsummering = Vedtaksbrevsoppsummering(behandlingId = behandling.id,
                                                            oppsummeringFritekst = "testverdi",
                                                            fritekst = "testverdi")

    val vedtaksbrevsperiode = Vedtaksbrevsperiode(behandlingId = behandling.id,
                                                  fom = LocalDate.now(),
                                                  tom = LocalDate.now(),
                                                  fritekst = "testverdi",
                                                  fritekststype = Friteksttype.FAKTA_AVSNITT)

    val økonomiXmlSendt = ØkonomiXmlSendt(behandlingId = behandling.id,
                                          melding = "testverdi",
                                          kvittering = "testverdi",
                                          meldingstype = Meldingstype.VEDTAK)

    val grupperingKravvedtaksstatus = GrupperingKravvedtaksstatus(kravvedtaksstatus437Id = kravvedtaksstatus437.id,
                                                                  behandlingId = behandling.id)

    val brevsporing = Brevsporing(behandlingId = behandling.id,
                                  journalpostId = "testverdi",
                                  dokumentId = "testverdi",
                                  brevtype = Brevtype.INNHENT_DOKUMENTASJONBREV)

    val økonomiXmlMottattArkiv = ØkonomiXmlMottattArkiv(melding = "testverdi")

}
