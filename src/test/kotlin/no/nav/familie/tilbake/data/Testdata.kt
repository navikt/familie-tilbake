package no.nav.familie.tilbake.data

import no.nav.familie.tilbake.domain.*
import no.nav.familie.tilbake.domain.behandling.Behandlingsresultat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

object Testdata {

    private val bruker = Bruker(ident = "321321321")

    val fagsak = Fagsak(ytelsestype = Ytelsestype.BA,
                        eksternFagsakId = "testverdi",
                        bruker = bruker)

    val eksternBehandling = EksternBehandling(henvisning = "testverdi",
                                              eksternId = UUID.randomUUID())

    val varsel = Varsel(varseltekst = "testverdi",
                        varselbeløp = 123)

    val verge = Verge(ident = "testverdi",
                      gyldigFom = LocalDate.now(),
                      gyldigTom = LocalDate.now(),
                      type = Vergetype.BARN,
                      orgNr = "testverdi",
                      navn = "testverdi",
                      kilde = "testverdi",
                      begrunnelse = "testverdi")

    val behandlingsresultat = Behandlingsresultat()

    val behandling = Behandling(fagsakId = fagsak.id,
                                type = Behandlingstype.TILBAKEKREVING,
                                opprettetDato = LocalDate.now(),
                                avsluttetDato = LocalDate.now(),
                                ansvarligSaksbehandler = "testverdi",
                                ansvarligBeslutter = "testverdi",
                                behandlendeEnhet = "testverdi",
                                behandlendeEnhetsNavn = "testverdi",
                                manueltOpprettet = true,
                                eksternBehandling = setOf(eksternBehandling),
                                verger = setOf(verge),
                                varsler = setOf(varsel),
                                resultater = setOf(behandlingsresultat),
                                eksternId = UUID.randomUUID())

    val behandlingsstegstype = Behandlingsstegstype(kode = "testverdi",
                                                    navn = "testverdi",
                                                    definertBehandlingsstatus = Behandlingsstatus.OPPRETTET,
                                                    beskrivelse = "testverdi")

    val behandlingsårsak = Behandlingsårsak(behandlingId = behandling.id,
                                            type = Behandlingsårsakstype.REVURDERING_KLAGE_KA,
                                            originalBehandlingId = null)

    val vurderingspunktsdefinisjon = Vurderingspunktsdefinisjon(kode = "testverdi",
                                                                behandlingsstegstypeId = behandlingsstegstype.id,
                                                                navn = "testverdi",
                                                                beskrivelse = "testverdi")

    val aksjonspunktsdefinisjon = Aksjonspunktsdefinisjon(kode = "testverdi",
                                                          navn = "testverdi",
                                                          vurderingspunktsdefinisjonId = vurderingspunktsdefinisjon.id,
                                                          beskrivelse = "testverdi",
                                                          totrinnsbehandlingDefault = true,
                                                          fristperiode = "testverdi",
                                                          skjermlenketype = Skjermlenketype.FORELDELSE)

    val aksjonspunkt = Aksjonspunkt(totrinnsbehandling = true,
                                    behandlingsstegstypeId = behandlingsstegstype.id,
                                    aksjonspunktsdefinisjonId = aksjonspunktsdefinisjon.id,
                                    behandlingId = behandling.id,
                                    tidsfrist = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
                                    status = Aksjonspunktsstatus.OPPRETTET)

    val revurderingsårsak = Revurderingsårsak(aksjonspunktId = aksjonspunkt.id,
                                              årsakstype = Årsakstype.FEIL_LOV)

    val behandlingsstegstilstand = Behandlingsstegstilstand(behandlingId = behandling.id,
                                                            behandlingsstegstypeId = behandlingsstegstype.id,
                                                            behandlingsstegsstatus = Behandlingstegsstatus.INNGANG)

    val behandlingsstegssekvens = Behandlingsstegssekvens(behandlingstype = Behandlingstype.TILBAKEKREVING,
                                                          behandlingsstegstypeId = behandlingsstegstype.id,
                                                          sekvensnummer = 1)

    val behandlingsresultat = Behandlingsresultat(behandlingId = behandling.id)

    val behandlingsvedtak = Behandlingsvedtak(vedtaksdato = LocalDate.now(),
                                              ansvarligSaksbehandler = "testverdi",
                                              behandlingsresultatId = behandlingsresultat.id)

    val totrinnsvurdering = Totrinnsvurdering(behandlingId = behandling.id,
                                              aksjonspunktsdefinisjonId = aksjonspunktsdefinisjon.id,
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

    val kravgrunnlag431 = Kravgrunnlag431(vedtakId = "testverdi",
                                          kravstatuskode = "testverdi",
                                          fagområdekode = Fagområdekode.UKJENT,
                                          fagsystem = Fagsystem.ARENA,
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
                                          eksternKravgrunnlagId = "testverdi")

    val kravgrunnlagsperiode432 = Kravgrunnlagsperiode432(kravgrunnlag431Id = kravgrunnlag431.id,
                                                          fom = LocalDate.now(),
                                                          tom = LocalDate.now(),
                                                          månedligSkattebeløp = 123.11)

    val kravgrunnlagsbeløp433 = Kravgrunnlagsbeløp433(kravgrunnlagsperiode432Id = kravgrunnlagsperiode432.id,
                                                      klassekode = Klassekode.FPADATORD,
                                                      klassetype = Klassetype.JUST,
                                                      opprinneligUtbetalingsbeløp = 123.11,
                                                      nyttBeløp = 123.11,
                                                      tilbakekrevesBeløp = 123.11,
                                                      uinnkrevdBeløp = 123.11,
                                                      resultatkode = "testverdi",
                                                      årsakskode = "testverdi",
                                                      skyldkode = "testverdi",
                                                      skatteprosent = 35.11)

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

    val vilkår = Vilkårsvurdering(behandlingId = behandling.id)

    val vilkårsperiode = Vilkårsvurderingsperiode(vilkårsvurderingId = vilkår.id,
                                                  fom = LocalDate.now(),
                                                  tom = LocalDate.now(),
                                                  navoppfulgt = Navoppfulgt.HAR_IKKE_FULGT_OPP,
                                                  vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                                                  begrunnelse = "testverdi")

    val vilkårsvurderingAktsomhet = VilkårsvurderingAktsomhet(vilkårsperiodeId = vilkårsperiode.id,
                                                              aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                                                              ileggRenter = true,
                                                              andelTilbakekreves = 123.11,
                                                              manueltSattBeløp = null,
                                                              begrunnelse = "testverdi",
                                                              særligeGrunnerTilReduksjon = true,
                                                              tilbakekrevSmabeløp = true,
                                                              særligeGrunnerBegrunnelse = "testverdi")

    val vilkårsvurderingSærligGrunn = VilkårsvurderingSærligGrunn(vilkårsvurderingAktsomhetId = vilkårsvurderingAktsomhet.id,
                                                                  særligGrunn = SærligGrunn.GRAD_AV_UAKTSOMHET,
                                                                  begrunnelse = "testverdi")

    val vilkårsvurderingGodTro = VilkårsvurderingGodTro(vilkårsperiodeId = vilkårsperiode.id,
                                                        beløpErIBehold = true,
                                                        beløpTilbakekreves = 32165,
                                                        begrunnelse = "testverdi")

    val faktaFeilutbetaling = FaktaFeilutbetaling(begrunnelse = "testverdi")

    val faktaFeilutbetalingsperiode = FaktaFeilutbetalingsperiode(faktaFeilutbetalingId = faktaFeilutbetaling.id,
                                                                  fom = LocalDate.now(),
                                                                  tom = LocalDate.now(),
                                                                  hendelsestype = Hendelsestype.PPN_ANNET_TYPE,
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