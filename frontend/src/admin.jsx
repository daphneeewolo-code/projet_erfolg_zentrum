import React, { useCallback, useEffect, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { Brand, Icon } from './Brand.jsx';
import { api, apiUrl, formatDate, refreshCsrf } from './api.js';
import './styles.css';

const emptyFilters = { q: '', course: '', enrollmentStatus: '', emailStatus: '', from: '' };
const statusNames = {
  SENT: 'Envoyé', QUEUED: 'En attente', FAILED: 'Échec',
  CONFIRMED: 'Confirmée', WAITLISTED: 'Liste d’attente',
};

function StatusPill({ status }) {
  return <span className={`status-pill status-${(status || 'queued').toLowerCase()}`}><i />{statusNames[status] || status}</span>;
}

function Login({ onLogin, message }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    try {
      await onLogin(username, password);
    } finally {
      setBusy(false);
    }
  }
  return (
    <main className="login-screen">
      <div className="login-art">
        <Brand light />
        <div><span className="eyebrow eyebrow-light"><span /> VOTRE ESPACE DE GESTION</span><h1>Le suivi,<br />en toute <em>sérénité.</em></h1><p>Un espace dédié pour accompagner chaque candidature avec attention.</p></div>
        <span className="login-campus">ERFOLG ZENTRUM <i /> DOUALA, CAMEROUN</span>
      </div>
      <section className="login-card">
        <span className="login-lock"><Icon name="lock" size={20} /></span>
        <span className="eyebrow">ACCÈS ADMINISTRATEUR</span>
        <h2>Content de vous revoir.</h2>
        <p>Connectez-vous pour retrouver les dossiers de candidature.</p>
        {message && <div className="form-alert" role="alert">{message}</div>}
        <form onSubmit={submit}>
          <label className="form-field" htmlFor="admin-username"><span>Identifiant</span><input id="admin-username" autoComplete="username" required value={username} onChange={(event) => setUsername(event.target.value)} /></label>
          <label className="form-field" htmlFor="admin-password"><span>Mot de passe</span><input id="admin-password" type="password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} /></label>
          <button className="button-primary full-width" disabled={busy} type="submit">{busy ? 'Connexion…' : 'Se connecter'} <Icon name="arrow" size={18} /></button>
        </form>
        <a className="login-back" href="/">← Retour au site</a>
      </section>
    </main>
  );
}

function Dashboard({ username, courses, onLogout }) {
  const [filters, setFilters] = useState(emptyFilters);
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [dashboard, setDashboard] = useState(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [selected, setSelected] = useState(null);
  const requestId = useRef(0);

  const loadData = useCallback(async (requestedPage = 0) => {
    const currentRequest = ++requestId.current;
    setError('');
    setLoading(true);
    const params = new URLSearchParams({ page: String(requestedPage), size: '10' });
    Object.entries(filters).forEach(([key, value]) => { if (value) params.set(key, value); });
    try {
      const [list, metrics] = await Promise.all([
        api(`/api/admin/enrollments?${params}`),
        api('/api/admin/dashboard'),
      ]);
      if (currentRequest !== requestId.current) return;
      setData(list);
      setDashboard(metrics);
      setPage(requestedPage);
    } catch (cause) {
      if (currentRequest === requestId.current) setError(cause.message);
    } finally {
      if (currentRequest === requestId.current) setLoading(false);
    }
  }, [filters]);

  useEffect(() => {
    const timer = window.setTimeout(() => loadData(0), 250);
    return () => window.clearTimeout(timer);
  }, [filters, loadData]);

  async function openDetails(id) {
    setError('');
    try {
      setSelected(await api(`/api/admin/enrollments/${id}`));
    } catch (cause) {
      setError(cause.message);
    }
  }

  async function resend(id) {
    setError('');
    try {
      await api(`/api/admin/enrollments/${id}/resend`, { method: 'POST' });
      setSelected(null);
      setNotice('La fiche a été ajoutée à la file d’envoi.');
      await loadData(page);
    } catch (cause) {
      setError(cause.message);
    }
  }

  const exportParams = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => { if (value) exportParams.set(key, value); });
  const totalPages = data?.totalPages || 0;

  return (
    <div className="admin-app">
      <aside className="admin-sidebar">
        <Brand light />
        <span className="sidebar-label">ESPACE DE GESTION</span>
        <a className="sidebar-active" href="/admin.html"><span className="sidebar-active-bar" /><Icon name="users" size={18} /> Inscriptions <b>{dashboard?.total ?? '—'}</b></a>
        <div className="sidebar-bottom">
          <div className="admin-user"><span>{(username || 'A').slice(0, 1).toUpperCase()}</span><div><strong>{username}</strong><small>ADMINISTRATEUR</small></div></div>
          <button className="logout-button" onClick={onLogout} type="button">↗ <span>Déconnexion</span></button>
        </div>
      </aside>
      <main className="admin-main">
        <header className="admin-topbar"><div><span className="eyebrow">ERFOLG ZENTRUM <i /> ADMINISTRATION</span><strong>Gestion des candidatures</strong></div><div className="topbar-actions"><a className="admin-back-link" href="/">Voir le site <Icon name="arrow" size={16} /></a><button className="topbar-logout" type="button" onClick={onLogout}>Déconnexion</button></div></header>
        <section className="admin-content">
          <div className="admin-welcome"><div><span className="eyebrow"><span /> VUE D’ENSEMBLE</span><h1>Bonjour, <em>{username}.</em></h1><p>Un aperçu clair de la vie de votre centre.</p></div><a className="button-outline" href={apiUrl(`/api/admin/export.csv?${exportParams}`)}><Icon name="download" size={17} /> Exporter les dossiers</a></div>
          {error && <div className="form-alert" role="alert">{error}</div>}
          {notice && <div className="notice-banner" role="status">{notice}<button type="button" aria-label="Fermer" onClick={() => setNotice('')}><Icon name="close" size={16} /></button></div>}
          <div className="metrics-grid">
            <article className="metric-card"><span className="metric-icon"><Icon name="users" /></span><span className="metric-label">TOUTES LES CANDIDATURES</span><strong>{dashboard?.total ?? '—'}</strong><small>Dossiers reçus</small></article>
            <article className="metric-card metric-highlight"><span className="metric-icon"><Icon name="calendar" /></span><span className="metric-label">CE MOIS-CI</span><strong>{dashboard?.thisMonth ?? '—'}</strong><small>Nouvelles demandes</small></article>
            <article className="metric-card"><span className="metric-icon"><Icon name="book" /></span><span className="metric-label">FORMATIONS OUVERTES</span><strong>{dashboard?.courses ?? courses.length}</strong><small>Filières disponibles</small></article>
            <article className={`metric-card${dashboard?.failedEmails ? ' metric-warning' : ''}`}><span className="metric-icon"><Icon name="arrow" /></span><span className="metric-label">E-MAILS À VÉRIFIER</span><strong>{dashboard?.failedEmails ?? '—'}</strong><small>Envois à relancer</small></article>
          </div>

          <section className="records-card">
            <div className="records-heading"><div><span className="eyebrow"><span /> SUIVI DES DOSSIERS</span><h2>Les candidatures</h2></div><span className="records-count">{data?.totalElements ?? '…'} dossier{data?.totalElements === 1 ? '' : 's'}</span></div>
            <div className="filters-bar">
              <label className="search-field"><Icon name="search" size={18} /><input type="search" placeholder="Nom, e-mail ou numéro…" aria-label="Rechercher une candidature" value={filters.q} onChange={(event) => setFilters({ ...filters, q: event.target.value })} /></label>
              <select aria-label="Filtrer par formation" value={filters.course} onChange={(event) => setFilters({ ...filters, course: event.target.value })}><option value="">Toutes les formations</option>{courses.map((course) => <option key={course}>{course}</option>)}</select>
              <select aria-label="Filtrer par statut d’inscription" value={filters.enrollmentStatus} onChange={(event) => setFilters({ ...filters, enrollmentStatus: event.target.value })}><option value="">Tous les statuts</option><option value="CONFIRMED">Confirmée</option><option value="WAITLISTED">Liste d’attente</option></select>
              <select aria-label="Filtrer par état d’e-mail" value={filters.emailStatus} onChange={(event) => setFilters({ ...filters, emailStatus: event.target.value })}><option value="">Tous les e-mails</option><option value="SENT">Envoyé</option><option value="QUEUED">En attente</option><option value="FAILED">Échec</option></select>
              <input aria-label="Candidatures à partir de cette date" type="date" value={filters.from} onChange={(event) => setFilters({ ...filters, from: event.target.value })} />
            </div>
            <div className="table-scroll"><table className="records-table"><thead><tr><th>ÉTUDIANT</th><th>N° DE DOSSIER</th><th>FORMATION</th><th>NIVEAU</th><th>CENTRE</th><th>STATUT</th><th>DATE</th><th>ÉTAT E-MAIL</th><th /></tr></thead>
              <tbody>{data?.content.map((item) => <tr key={item.id}><td><div className="student-cell"><span className="student-avatar">{item.firstName[0]}{item.lastName[0]}</span><span><strong>{item.firstName} {item.lastName}</strong><small>{item.email}</small></span></div></td><td className="registration-id">{item.registrationNumber}</td><td>{item.course}</td><td>{item.level}</td><td>{item.centre || '—'}</td><td><StatusPill status={item.enrollmentStatus} /></td><td>{formatDate(item.createdAt)}</td><td><StatusPill status={item.emailStatus} /></td><td><div className="row-actions"><button type="button" onClick={() => openDetails(item.id)}>Détails</button><a href={apiUrl(`/api/admin/enrollments/${item.id}/pdf`)} aria-label={`Télécharger le dossier ${item.registrationNumber}`}><Icon name="download" size={16} /></a></div></td></tr>)}</tbody>
            </table></div>
            {loading && !data && <div className="empty-results"><strong>Chargement des dossiers…</strong></div>}
            {data && !data.content.length && !loading && <div className="empty-results"><span><Icon name="search" size={22} /></span><strong>Aucune candidature trouvée</strong><small>Essayez de modifier votre recherche ou vos filtres.</small></div>}
            <div className="pagination"><span>Page {totalPages ? page + 1 : 0} sur {totalPages}</span><div><button type="button" aria-label="Page précédente" disabled={page <= 0} onClick={() => loadData(page - 1)}>←</button><button type="button" aria-label="Page suivante" disabled={page + 1 >= totalPages} onClick={() => loadData(page + 1)}>→</button></div></div>
          </section>
          <footer className="admin-footer"><span>DONNÉES CONFIDENTIELLES <i /> ACCÈS RÉSERVÉ</span><span>ERFOLG ZENTRUM · DOUALA</span></footer>
        </section>
      </main>
      {selected && <div className="modal-backdrop" onMouseDown={(event) => { if (event.target === event.currentTarget) setSelected(null); }}>
        <section className="details-modal" role="dialog" aria-modal="true" aria-labelledby="details-title">
          <button className="modal-close" type="button" aria-label="Fermer" onClick={() => setSelected(null)}><Icon name="close" /></button>
          <span className="eyebrow"><span /> DOSSIER ÉTUDIANT</span><h2 id="details-title">{selected.firstName} {selected.lastName}</h2><p className="modal-registration">{selected.registrationNumber}</p>
          <div className="details-grid">{[['E-mail', selected.email], ['Téléphone', selected.phone], ['Formation', selected.course], ['Niveau', selected.level], ['Centre', selected.centre || 'Non renseigné'], ['Statut de l’inscription', <StatusPill status={selected.enrollmentStatus} />], ['Date de naissance', selected.birthDate || 'Non renseignée'], ['Ville / quartier', selected.address || 'Non renseigné'], ['Inscription reçue', formatDate(selected.createdAt)], ['Statut de l’e-mail', <StatusPill status={selected.emailStatus} />], ...(selected.emailError ? [['Dernière erreur', selected.emailError]] : [])].map(([label, value]) => <div className="detail-item" key={label}><span>{label}</span><strong>{value}</strong></div>)}</div>
          <div className="modal-actions"><button className="button-primary" type="button" onClick={() => resend(selected.id)}>Renvoyer le PDF par e-mail <Icon name="arrow" size={17} /></button><a className="button-outline" href={apiUrl(`/api/admin/enrollments/${selected.id}/pdf`)}><Icon name="download" size={16} /> Télécharger le PDF</a></div>
        </section>
      </div>}
    </div>
  );
}

function App() {
  const [state, setState] = useState({ loading: true, username: '', message: '', courses: [] });
  useEffect(() => {
    let active = true;
    Promise.all([refreshCsrf(), api('/api/config')])
      .then(async ([, config]) => {
        try {
          const current = await api('/api/admin/me');
          if (active) setState({ loading: false, username: current.username, message: '', courses: config.courses });
        } catch {
          if (active) setState({ loading: false, username: '', message: '', courses: config.courses });
        }
      })
      .catch((cause) => { if (active) setState({ loading: false, username: '', message: cause.message, courses: [] }); });
    return () => { active = false; };
  }, []);

  async function login(username, password) {
    try {
      const user = await api('/api/admin/login', { method: 'POST', body: JSON.stringify({ username, password }) });
      setState((current) => ({ ...current, username: user.username, message: '' }));
    } catch (cause) {
      setState((current) => ({ ...current, message: cause.message }));
    }
  }

  async function logout() {
    await api('/api/admin/logout', { method: 'POST' });
    setState((current) => ({ ...current, username: '', message: '' }));
  }

  if (state.loading) return <div className="loading-screen"><span className="loading-mark">E<span>.</span></span><p>Préparation de votre espace…</p></div>;
  if (!state.username) return <Login onLogin={login} message={state.message} />;
  return <Dashboard username={state.username} courses={state.courses} onLogout={() => logout().catch((cause) => setState((current) => ({ ...current, message: cause.message })))} />;
}

createRoot(document.getElementById('root')).render(<App />);
