import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { Brand, Icon } from './Brand.jsx';
import { api, downloadPdf, refreshCsrf } from './api.js';
import './styles.css';

function Field({ label, name, children, required = false, wide = false }) {
  return (
    <label className={`form-field${wide ? ' field-wide' : ''}`} htmlFor={name}>
      <span>{label}{required && <b aria-hidden="true"> *</b>}</span>
      {children}
    </label>
  );
}

function App() {
  const [config, setConfig] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [receipt, setReceipt] = useState(null);
  const [form, setForm] = useState({
    firstName: '', lastName: '', email: '', phone: '', course: '', level: '', centre: '', birthDate: '', address: '',
  });

  useEffect(() => {
    Promise.all([refreshCsrf(), api('/api/config')])
      .then(([, data]) => setConfig(data))
      .catch((cause) => setError(cause.message));
  }, []);

  const admissionsOpen = config
    ? !config.openingDate || Date.now() >= new Date(`${config.openingDate}T00:00:00`).getTime() - (Number(config.openingMarginDays) || 0) * 86_400_000
    : true;

  const admissionsStatus = config?.openingDate
    ? (admissionsOpen ? 'Admissions 2026–27 ouvertes' : `Ouverture des inscriptions le ${new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long' }).format(new Date(`${config.openingDate}T00:00:00`))}`)
    : 'Admissions 2026–27 ouvertes';

  function update(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function submit(event) {
    event.preventDefault();
    setError('');
    setBusy(true);
    const request = { ...form };
    if (!request.birthDate) delete request.birthDate;
    if (!request.address.trim()) delete request.address;
    try {
      const result = await api('/api/enrollments', {
        method: 'POST',
        body: JSON.stringify(request),
      });
      setReceipt({ ...result, firstName: form.firstName });
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch (cause) {
      setError(cause.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="public-app">
      <header className="site-header">
        <Brand />
        <div className="header-right">
          <span className="header-open"><i /> {admissionsStatus}</span>
          <a className="admin-link" href="/admin.html">Espace administration <Icon name="arrow" size={16} /></a>
        </div>
      </header>

      <main>
        <section className="hero">
          <div className="hero-copy">
            <div className="eyebrow"><span /> UNE FORMATION. UN NOUVEAU DÉPART.</div>
            <h1>Le prochain<br />chapitre de votre vie<br />se joue <em>ici.</em></h1>
            <p className="hero-lead">Développez des compétences qui ouvrent des portes. À Erfolg Zentrum, votre ambition devient un vrai savoir-faire.</p>
            <a className="hero-cta" href="#inscription">Choisir ma formation <Icon name="arrow" size={18} /></a>
            <div className="hero-proof">
              <span className="proof-avatars"><i>EZ</i><i>+</i><i>+</i></span>
              <span><strong>Votre avenir, concrètement.</strong><small>Un dossier simple. Une formation qui compte.</small></span>
            </div>
            <div className="hero-decoration" aria-hidden="true">
              <div className="art-sun" />
              <div className="art-orbit orbit-a" />
              <div className="art-orbit orbit-b" />
              <div className="art-card"><span className="art-card-icon"><Icon name="book" size={19} /></span><span><b>Le savoir ouvre des portes</b><small>Commencez aujourd’hui</small></span><i>↗</i></div>
              <div className="art-index">ERFOLG ZENTRUM <span>·</span> DOUALA</div>
              <span className="art-star star-one">✳</span><span className="art-star star-two">✳</span>
            </div>
          </div>

          <section className="application-card" id="inscription" aria-labelledby="form-title">
            {receipt ? (
              <div className="success-state">
                <span className="success-icon"><Icon name="check" size={29} /></span>
                <div className="eyebrow"><span />{receipt.enrollmentStatus === 'WAITLISTED' ? ' LISTE D’ATTENTE' : ' DEMANDE ENREGISTRÉE'}</div>
                <h2>Un beau début,<br /><em>{receipt.firstName}.</em></h2>
                <p>{receipt.enrollmentStatus === 'WAITLISTED'
                  ? 'Le groupe choisi est complet. Votre demande est enregistrée sur la liste d’attente; l’équipe vous contactera si une place se libère.'
                  : 'Votre demande a bien été transmise à l’équipe d’Erfolg Zentrum. Nous sommes ravis de vous accompagner.'}</p>
                <div className="receipt-number"><span>VOTRE NUMÉRO D’INSCRIPTION</span><strong>{receipt.registrationNumber}</strong><small>Conservez-le pour le suivi de votre dossier.</small></div>
                <div className="success-note"><Icon name="check" size={17} />{receipt.enrollmentStatus === 'WAITLISTED'
                  ? ' Votre fiche de demande est prête à être téléchargée.'
                  : ' Votre fiche d’inscription est prête à être téléchargée.'}</div>
                <button className="button-primary full-width" type="button" onClick={() => downloadPdf(receipt.pdfBase64, `${receipt.registrationNumber}.pdf`)}>
                  Télécharger ma fiche PDF <Icon name="download" size={18} />
                </button>
                <button className="button-text" type="button" onClick={() => window.location.reload()}>Effectuer une nouvelle inscription</button>
              </div>
            ) : admissionsOpen ? (
              <>
                <div className="card-heading">
                  <div className="card-kicker"><span className="kicker-number">01</span><span>VOTRE CANDIDATURE</span><span className="kicker-line" /><span className="kicker-session">2026—27</span></div>
                  <h2 id="form-title">Faisons connaissance.</h2>
                  <p>Remplissez votre dossier en quelques minutes.</p>
                </div>
                {error && <div className="form-alert" role="alert">{error}</div>}
                <form className="application-form" onSubmit={submit}>
                  <div className="form-section-title"><span>01</span> VOS COORDONNÉES</div>
                  <div className="form-grid">
                    <Field label="Prénom" name="firstName" required><input id="firstName" name="firstName" autoComplete="given-name" placeholder="Ex. Marie" maxLength="100" required value={form.firstName} onChange={update} /></Field>
                    <Field label="Nom" name="lastName" required><input id="lastName" name="lastName" autoComplete="family-name" placeholder="Ex. Mbarga" maxLength="100" required value={form.lastName} onChange={update} /></Field>
                    <Field label="Adresse e-mail" name="email" required wide><input id="email" name="email" type="email" autoComplete="email" placeholder="vous@exemple.com" maxLength="254" required value={form.email} onChange={update} /></Field>
                    <Field label="Téléphone" name="phone" required wide><input id="phone" name="phone" type="tel" autoComplete="tel" placeholder="+237 6XX XXX XXX" maxLength="30" pattern="^\+?[0-9][0-9\s().-]{6,19}$" required value={form.phone} onChange={update} /></Field>
                  </div>
                  <div className="form-section-title section-gap"><span>02</span> VOTRE PROJET</div>
                  <div className="form-grid">
                    <Field label="Formation souhaitée" name="course" required wide>
                      <select id="course" name="course" required value={form.course} onChange={update}>
                        <option value="">{config ? 'Choisissez votre formation' : 'Chargement des formations…'}</option>
                        {config?.courses.map((course) => <option key={course} value={course}>{course}</option>)}
                      </select>
                    </Field>
                    <Field label="Votre niveau" name="level" required wide>
                      <select id="level" name="level" required value={form.level} onChange={update}>
                        <option value="">Sélectionnez votre niveau</option>
                        {config?.levels.map((level) => <option key={level} value={level}>{level}</option>)}
                      </select>
                    </Field>
                    <Field label="Centre" name="centre" required wide>
                      <select id="centre" name="centre" required value={form.centre} onChange={update}>
                        <option value="">Choisissez votre centre</option>
                        {config?.centres.map((centre) => <option key={centre} value={centre}>{centre}</option>)}
                      </select>
                    </Field>
                    <Field label="Date de naissance" name="birthDate"><input id="birthDate" name="birthDate" type="date" autoComplete="bday" value={form.birthDate} onChange={update} /></Field>
                    <Field label="Ville / quartier" name="address"><input id="address" name="address" autoComplete="address-level2" placeholder="Ex. Bonanjo" maxLength="300" value={form.address} onChange={update} /></Field>
                  </div>
                  <button className="button-primary full-width submit-application" disabled={busy || !config} type="submit">
                    {busy ? 'Envoi de votre dossier…' : 'Envoyer ma demande'} <Icon name="arrow" size={18} />
                  </button>
                  <p className="privacy-note"><Icon name="lock" size={15} /> Vos informations restent confidentielles et accessibles uniquement à l’administration.</p>
                </form>
              </>
            ) : (
              <div className="success-state">
                <span className="success-icon"><Icon name="calendar" size={29} /></span>
                <div className="eyebrow"><span /> INSCRIPTIONS NON OUVERTES</div>
                <h2>Les inscriptions ouvrent bientôt.</h2>
                <p>{config?.openingDate
                  ? `La plateforme s’ouvre automatiquement ${Number(config.openingMarginDays) || 25} jours avant la date annoncée, soit le ${new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long' }).format(new Date(`${config.openingDate}T00:00:00`))}.`
                  : 'Les candidatures seront bientôt ouvertes. Revenez plus tard.'}</p>
              </div>
            )}
          </section>
        </section>

        <section className="benefits" aria-label="Les avantages de l’inscription">
          <div className="benefit"><span className="benefit-icon"><Icon name="book" /></span><span><strong>Le bon savoir-faire</strong><small>Des filières pensées pour votre avenir.</small></span></div>
          <div className="benefit"><span className="benefit-icon"><Icon name="calendar" /></span><span><strong>Simple et rapide</strong><small>Votre dossier se remplit en quelques minutes.</small></span></div>
          <div className="benefit"><span className="benefit-icon"><Icon name="lock" /></span><span><strong>En toute confiance</strong><small>Vos informations restent privées.</small></span></div>
          <div className="benefit-location"><Icon name="pin" size={18} /> DOUALA, CAMEROUN</div>
        </section>
      </main>
      <footer className="site-footer"><Brand light /><span>APPRENDRE AUJOURD’HUI. RÉUSSIR DEMAIN.</span><span>© 2026 ERFOLG ZENTRUM</span></footer>
    </div>
  );
}

createRoot(document.getElementById('root')).render(<App />);
