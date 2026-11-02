let csrfToken = '';

export async function refreshCsrf() {
  const response = await fetch('/api/csrf');
  if (!response.ok) throw new Error('Impossible de préparer la connexion sécurisée.');
  csrfToken = (await response.json()).token;
}

export async function api(url, options = {}) {
  const headers = new Headers(options.headers);
  if (options.body) headers.set('Content-Type', 'application/json');
  if (options.method && options.method !== 'GET') {
    if (!csrfToken) await refreshCsrf();
    headers.set('X-XSRF-TOKEN', csrfToken);
  }

  const response = await fetch(url, { ...options, headers });
  if (response.status === 204) return null;

  const contentType = response.headers.get('content-type') || '';
  const result = contentType.includes('application/json') ? await response.json() : null;
  if (!response.ok) {
    throw new Error(result?.message || 'La demande n’a pas pu aboutir.');
  }
  return result;
}

export function formatDate(value) {
  return new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium' }).format(new Date(value));
}

export function downloadPdf(base64, filename) {
  const binary = atob(base64);
  const bytes = Uint8Array.from(binary, (character) => character.charCodeAt(0));
  const objectUrl = URL.createObjectURL(new Blob([bytes], { type: 'application/pdf' }));
  const link = document.createElement('a');
  link.href = objectUrl;
  link.download = filename;
  link.click();
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1000);
}
