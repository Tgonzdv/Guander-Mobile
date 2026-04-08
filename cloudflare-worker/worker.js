export default {
  async fetch(request, env) {
    if (request.method === 'OPTIONS') {
      return corsResponse(null, 204);
    }
    const url = new URL(request.url);
    if (request.method === 'POST' && url.pathname === '/register') {
      return handleRegister(request, env);
    }
    return new Response('Not found', { status: 404 });
  }
};

async function handleRegister(request, env) {
  let body;
  try { body = await request.json(); } catch { return corsResponse({ error: 'Invalid JSON' }, 400); }

  const { email, name, lastName } = body;
  if (!email) return corsResponse({ error: 'Email is required' }, 400);

  try {
    const existing = await env.DB.prepare('SELECT id_user_data FROM user_data WHERE email = ?').bind(email).first();
    if (existing) return corsResponse({ success: true, existing: true });

    const userDataResult = await env.DB.prepare(
      "INSERT INTO user_data (name, last_name, tel, email, address, password_hash) VALUES (?, ?, '', ?, '', NULL)"
    ).bind(name || '', lastName || '', email).run();
    const userDataId = userDataResult.meta.last_row_id;

    let username = email.split('@')[0].replace(/[^a-zA-Z0-9_]/g, '_');
    const dup = await env.DB.prepare('SELECT id_user FROM users WHERE username = ?').bind(username).first();
    if (dup) username = username + '_' + Date.now();

    const userResult = await env.DB.prepare(
      'INSERT INTO users (username, fk_user_data, fk_rol) VALUES (?, ?, 2)'
    ).bind(username, userDataId).run();
    const userId = userResult.meta.last_row_id;

    await env.DB.prepare('INSERT INTO customer (points, fk_user) VALUES (0, ?)').bind(userId).run();

    return corsResponse({ success: true, userId, userDataId });
  } catch (err) {
    return corsResponse({ error: err.message }, 500);
  }
}

function corsResponse(body, status = 200) {
  const headers = {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type',
  };
  return new Response(body ? JSON.stringify(body) : null, { status, headers });
}