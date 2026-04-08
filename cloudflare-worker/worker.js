export default {
  async fetch(request, env) {
    if (request.method === 'OPTIONS') {
      return corsResponse(null, 204);
    }
    const url = new URL(request.url);
    if (request.method === 'POST' && url.pathname === '/register') {
      return handleRegister(request, env);
    }
    if (request.method === 'GET' && url.pathname === '/dashboard') {
      return handleGetDashboard(request, env, url);
    }
    if (request.method === 'GET' && url.pathname === '/profile') {
      return handleGetProfile(request, env, url);
    }
    if (request.method === 'PUT' && url.pathname === '/profile') {
      return handlePutProfile(request, env);
    }
    if (request.method === 'POST' && url.pathname === '/sign-upload') {
      return handleSignUpload(request, env);
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

async function handleGetDashboard(request, env, url) {
  const email = url.searchParams.get('email');
  if (!email) return corsResponse({ error: 'Email is required' }, 400);

  try {
    // Get the latest user record for this email (case-insensitive), ordered by most recent
    const userData = await env.DB.prepare(
      `SELECT ud.name, u.id_user
       FROM user_data ud
       JOIN users u ON u.fk_user_data = ud.id_user_data
       WHERE LOWER(ud.email) = LOWER(?)
       ORDER BY u.id_user DESC
       LIMIT 1`
    ).bind(email).first();

    if (!userData) return corsResponse({ error: 'User not found' }, 404);

    // Get the customer record with the highest points for this user (handles duplicates)
    const customerData = await env.DB.prepare(
      'SELECT points FROM customer WHERE fk_user = ? ORDER BY points DESC LIMIT 1'
    ).bind(userData.id_user).first();
    const points = customerData ? customerData.points : 0;

    const { results } = await env.DB.prepare(
      `SELECT n.name, n.description
       FROM notifications n
       JOIN notif_users nu ON n.id_notification = nu.fk_notifications_id
       WHERE nu.fk_users_id = ?
       ORDER BY n.id_notification DESC
       LIMIT 5`
    ).bind(userData.id_user).all();

    return corsResponse({ points, name: userData.name, notifications: results || [] });
  } catch (err) {
    return corsResponse({ error: err.message }, 500);
  }
}

function corsResponse(body, status = 200) {
  const headers = {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type',
  };
  return new Response(body ? JSON.stringify(body) : null, { status, headers });
}

async function handleGetProfile(request, env, url) {
  const email = url.searchParams.get('email');
  if (!email) return corsResponse({ error: 'Email is required' }, 400);

  // Ensure photo_url column exists (runs silently if already present)
  await env.DB.prepare('ALTER TABLE user_data ADD COLUMN photo_url VARCHAR(500)').run().catch(() => {});

  try {
    const row = await env.DB.prepare(`
      SELECT ud.name, ud.last_name, ud.tel, ud.email, ud.address, ud.photo_url,
             u.id_user, u.date_reg
      FROM user_data ud
      JOIN users u ON u.fk_user_data = ud.id_user_data
      WHERE LOWER(ud.email) = LOWER(?)
      ORDER BY u.id_user DESC
      LIMIT 1
    `).bind(email).first();

    if (!row) return corsResponse({ error: 'User not found' }, 404);

    const customerRow = await env.DB.prepare(
      'SELECT id_customer, points FROM customer WHERE fk_user = ? ORDER BY points DESC LIMIT 1'
    ).bind(row.id_user).first();

    const customerId = customerRow ? customerRow.id_customer : null;
    const points = customerRow ? customerRow.points : 0;

    let placesVisited = 0;
    let coupons = 0;
    if (customerId) {
      try {
        const placesRow = await env.DB.prepare(`
          SELECT
            (SELECT COUNT(DISTINCT fk_store) FROM store_purchase WHERE fk_customer = ?) +
            (SELECT COUNT(DISTINCT fk_professional) FROM prof_purchase WHERE fk_customer = ?) AS total
        `).bind(customerId, customerId).first();
        placesVisited = placesRow ? (placesRow.total || 0) : 0;
      } catch (e) { placesVisited = 0; }

      try {
        const couponsRow = await env.DB.prepare(`
          SELECT
            (SELECT COUNT(*) FROM coupon_buy_store WHERE fk_customer = ?) +
            (SELECT COUNT(*) FROM coupon_buy_prof WHERE fk_customer = ?) AS total
        `).bind(customerId, customerId).first();
        coupons = couponsRow ? (couponsRow.total || 0) : 0;
      } catch (e) { coupons = 0; }
    }

    return corsResponse({
      name: row.name,
      lastName: row.last_name,
      tel: row.tel,
      email: row.email,
      address: row.address,
      photoUrl: row.photo_url || null,
      dateReg: row.date_reg,
      points,
      placesVisited,
      coupons
    });
  } catch (err) {
    return corsResponse({ error: err.message }, 500);
  }
}

async function handlePutProfile(request, env) {
  let body;
  try { body = await request.json(); } catch { return corsResponse({ error: 'Invalid JSON' }, 400); }

  const { email, name, lastName, tel, address, photoUrl } = body;
  if (!email) return corsResponse({ error: 'Email is required' }, 400);

  await env.DB.prepare('ALTER TABLE user_data ADD COLUMN photo_url VARCHAR(500)').run().catch(() => {});

  try {
    await env.DB.prepare(
      `UPDATE user_data SET name = ?, last_name = ?, tel = ?, address = ?, photo_url = ?
       WHERE LOWER(email) = LOWER(?)`
    ).bind(name || '', lastName || '', tel || '', address || '', photoUrl || null, email).run();
    return corsResponse({ success: true });
  } catch (err) {
    return corsResponse({ error: err.message }, 500);
  }
}

async function handleSignUpload(request, env) {
  const timestamp = Math.floor(Date.now() / 1000);
  const folder = 'profile_pics';
  const stringToSign = `folder=${folder}&timestamp=${timestamp}${env.CLOUDINARY_API_SECRET}`;
  const msgBuffer = new TextEncoder().encode(stringToSign);
  const hashBuffer = await crypto.subtle.digest('SHA-1', msgBuffer);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const signature = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');

  return corsResponse({
    timestamp: String(timestamp),
    signature,
    apiKey: env.CLOUDINARY_API_KEY,
    cloudName: env.CLOUDINARY_CLOUD_NAME,
    folder
  });
}