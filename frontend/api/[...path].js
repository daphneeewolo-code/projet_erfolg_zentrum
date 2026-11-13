function readBody(request) {
  if (request.body !== undefined) {
    if (Buffer.isBuffer(request.body)) return Promise.resolve(request.body);
    if (typeof request.body === 'string') return Promise.resolve(Buffer.from(request.body));
    return Promise.resolve(Buffer.from(JSON.stringify(request.body)));
  }

  return new Promise((resolve, reject) => {
    const chunks = [];
    request.on('data', (chunk) => chunks.push(chunk));
    request.on('end', () => resolve(Buffer.concat(chunks)));
    request.on('error', reject);
  });
}

export default async function handler(request, response) {
  const backendUrl = process.env.BACKEND_URL;
  if (!backendUrl) {
    response.status(503).json({ message: 'Le serveur API n’est pas encore configuré.' });
    return;
  }

  const incomingUrl = new URL(request.url, 'http://localhost');
  const targetUrl = new URL(`${incomingUrl.pathname}${incomingUrl.search}`, backendUrl);
  const headers = new Headers();
  for (const name of ['accept', 'content-type', 'cookie', 'x-xsrf-token']) {
    const value = request.headers[name];
    if (value) headers.set(name, Array.isArray(value) ? value.join(', ') : value);
  }

  const options = { method: request.method, headers, redirect: 'manual' };
  if (request.method !== 'GET' && request.method !== 'HEAD') {
    options.body = await readBody(request);
  }

  const upstream = await fetch(targetUrl, options);
  response.status(upstream.status);
  for (const name of ['content-type', 'content-disposition', 'cache-control', 'location']) {
    const value = upstream.headers.get(name);
    if (value) response.setHeader(name, value);
  }
  const cookies = upstream.headers.getSetCookie();
  if (cookies.length) response.setHeader('set-cookie', cookies);

  response.send(Buffer.from(await upstream.arrayBuffer()));
}
