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
    if (request.method === 'GET' && url.pathname === '/rewards') {
      return handleGetRewards(request, env, url);
    }
    if (request.method === 'POST' && url.pathname === '/redeem') {
      return handlePostRedeem(request, env);
    }
    if (request.method === 'GET' && url.pathname === '/redeem-history') {
      return handleGetRedeemHistory(request, env, url);
    }
    if (request.method === 'GET' && url.pathname === '/places') {
      return handleGetPlaces(request, env, url);
    }
    if (request.method === 'GET' && url.pathname === '/store-photos') {
      return handleGetStorePhotos(request, env, url);
    }
    if (request.method === 'GET' && url.pathname === '/comments') {
      return handleGetComments(request, env, url);
    }
    if (request.method === 'POST' && url.pathname === '/review') {
      return handlePostReview(request, env);
    }
    if (request.method === 'POST' && url.pathname === '/validate-qr') {
      return handleValidateQr(request, env);
    }
    if (request.method === 'POST' && url.pathname === '/login-email') {
      return handleLoginEmail(request, env);
    }
    if (request.method === 'POST' && url.pathname === '/forgot-password') {
      return handleForgotPassword(request, env);
    }
    if (request.method === 'POST' && url.pathname === '/reset-password') {
      return handleResetPassword(request, env);
    }
    return new Response('Not found', { status: 404 });
  }
};

// ── Password helpers ────────────────────────────────────────────────────────
function generateSalt() {
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  return Array.from(bytes).map(b => b.toString(16).padStart(2, '0')).join('');
}

async function hashPasswordPBKDF2(password, saltHex) {
  const enc = new TextEncoder();
  const saltBytes = new Uint8Array(saltHex.match(/.{2}/g).map(h => parseInt(h, 16)));
  const keyMaterial = await crypto.subtle.importKey(
    'raw', enc.encode(password), { name: 'PBKDF2' }, false, ['deriveBits']
  );
  const bits = await crypto.subtle.deriveBits(
    { name: 'PBKDF2', hash: 'SHA-256', salt: saltBytes, iterations: 100000 },
    keyMaterial, 256
  );
  return Array.from(new Uint8Array(bits)).map(b => b.toString(16).padStart(2, '0')).join('');
}

async function handleRegister(request, env) {
  let body;
  try { body = await request.json(); } catch { return corsResponse({ error: 'Invalid JSON' }, 400); }

  const { email, name, lastName, tel, password } = body;
  if (!email) return corsResponse({ error: 'Email is required' }, 400);

  try {
    const existing = await env.DB.prepare('SELECT id_user_data, password_hash FROM user_data WHERE LOWER(email) = LOWER(?)').bind(email).first();
    if (existing) {
      // Email/password registration on existing account → error
      if (password) return corsResponse({ error: 'Email ya registrado. Intentá iniciar sesión.' }, 409);
      // Google sign-in on existing account → success silently
      return corsResponse({ success: true, existing: true });
    }

    let passwordHash = null;
    if (password) {
      const salt = generateSalt();
      const hash = await hashPasswordPBKDF2(password, salt);
      passwordHash = salt + ':' + hash;
    }

    const userDataResult = await env.DB.prepare(
      "INSERT INTO user_data (name, last_name, tel, email, address, password_hash) VALUES (?, ?, ?, ?, '', ?)"
    ).bind(name || '', lastName || '', tel || '', email, passwordHash).run();
    const userDataId = userDataResult.meta.last_row_id;

    let username = email.split('@')[0].replace(/[^a-zA-Z0-9_]/g, '_');
    const dup = await env.DB.prepare('SELECT id_user FROM users WHERE username = ?').bind(username).first();
    if (dup) username = username + '_' + Date.now();

    const userResult = await env.DB.prepare(
      'INSERT INTO users (username, fk_user_data, fk_rol) VALUES (?, ?, 2)'
    ).bind(username, userDataId).run();
    const userId = userResult.meta.last_row_id;

    await env.DB.prepare('INSERT INTO customer (points, fk_user) VALUES (0, ?)').bind(userId).run();

    // Send welcome email (non-blocking)
    await sendWelcomeEmail(env, email, name);

    return corsResponse({ success: true, userId, userDataId });
  } catch (err) {
    return corsResponse({ error: err.message }, 500);
  }
}

async function handleLoginEmail(request, env) {
  let body;
  try { body = await request.json(); } catch { return corsResponse({ error: 'Invalid JSON' }, 400); }

  const { email, password } = body;
  if (!email || !password) return corsResponse({ error: 'Email y contraseña requeridos' }, 400);

  try {
    const row = await env.DB.prepare(
      'SELECT id_user_data, name, last_name, password_hash FROM user_data WHERE LOWER(email) = LOWER(?)'
    ).bind(email).first();

    if (!row) return corsResponse({ error: 'Credenciales incorrectas' }, 401);
    if (!row.password_hash) return corsResponse({ error: 'Esta cuenta usa Google para iniciar sesión' }, 400);

    const parts = row.password_hash.split(':');
    if (parts.length !== 2) return corsResponse({ error: 'Error interno de credenciales' }, 500);

    const [salt, storedHash] = parts;
    const inputHash = await hashPasswordPBKDF2(password, salt);
    if (inputHash !== storedHash) return corsResponse({ error: 'Credenciales incorrectas' }, 401);

    return corsResponse({ success: true, email, name: row.name, lastName: row.last_name });
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

async function handleForgotPassword(request, env) {
  let body;
  try { body = await request.json(); } catch { return corsResponse({ error: 'Invalid JSON' }, 400); }

  const { email } = body;
  if (!email) return corsResponse({ error: 'Email requerido' }, 400);

  try {
    await env.DB.prepare(`CREATE TABLE IF NOT EXISTS password_reset_tokens (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      email TEXT NOT NULL,
      token TEXT NOT NULL,
      expires_at INTEGER NOT NULL,
      created_at INTEGER NOT NULL
    )`).run();

    const user = await env.DB.prepare(
      'SELECT name FROM user_data WHERE LOWER(email) = LOWER(?)'
    ).bind(email).first();

    // Always return success to avoid email enumeration
    if (!user) return corsResponse({ success: true });

    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    const expiresAt = Date.now() + 15 * 60 * 1000;

    await env.DB.prepare(
      'DELETE FROM password_reset_tokens WHERE LOWER(email) = LOWER(?)'
    ).bind(email).run();

    await env.DB.prepare(
      'INSERT INTO password_reset_tokens (email, token, expires_at, created_at) VALUES (?, ?, ?, ?)'
    ).bind(email, otp, expiresAt, Date.now()).run();

    await sendPasswordResetEmail(env, email, user.name, otp);

    return corsResponse({ success: true });
  } catch (err) {
    return corsResponse({ error: err.message }, 500);
  }
}

async function handleResetPassword(request, env) {
  let body;
  try { body = await request.json(); } catch { return corsResponse({ error: 'Invalid JSON' }, 400); }

  const { email, token, newPassword } = body;
  if (!email || !token || !newPassword) return corsResponse({ error: 'Datos incompletos' }, 400);
  if (newPassword.length < 6) return corsResponse({ error: 'La contrase\u00f1a debe tener al menos 6 caracteres' }, 400);

  try {
    const row = await env.DB.prepare(
      'SELECT id, expires_at FROM password_reset_tokens WHERE LOWER(email) = LOWER(?) AND token = ?'
    ).bind(email, token).first();

    if (!row) return corsResponse({ error: 'C\u00f3digo inv\u00e1lido' }, 400);
    if (Date.now() > row.expires_at) {
      await env.DB.prepare('DELETE FROM password_reset_tokens WHERE id = ?').bind(row.id).run();
      return corsResponse({ error: 'El c\u00f3digo expir\u00f3. Solicit\u00e1 uno nuevo.' }, 400);
    }

    const salt = generateSalt();
    const hash = await hashPasswordPBKDF2(newPassword, salt);
    const passwordHash = salt + ':' + hash;

    await env.DB.prepare(
      'UPDATE user_data SET password_hash = ? WHERE LOWER(email) = LOWER(?)'
    ).bind(passwordHash, email).run();

    await env.DB.prepare('DELETE FROM password_reset_tokens WHERE id = ?').bind(row.id).run();

    return corsResponse({ success: true });
  } catch (err) {
    return corsResponse({ error: err.message }, 500);
  }
}

async function sendPasswordResetEmail(env, email, name, otp) {
  if (!env.RESEND_API_KEY) return;
  const displayName = name || 'Guandero';
  const html = `
    <div style="font-family:sans-serif;max-width:520px;margin:0 auto;padding:32px 24px;background:#fff;border-radius:12px">
      <div style="text-align:center;margin-bottom:24px">
        <h1 style="color:#2E7D32;font-size:24px;margin:0">\uD83D\uDD12 Restablecer contrase\u00f1a</h1>
      </div>
      <p style="font-size:16px;color:#333">Hola <strong>${displayName}</strong>,</p>
      <p style="font-size:15px;color:#555;line-height:1.6">
        Recibimos una solicitud para restablecer tu contrase\u00f1a. Ingres\u00e1 el siguiente c\u00f3digo en la app:
      </p>
      <div style="text-align:center;margin:32px 0">
        <div style="display:inline-block;background:#F1F8F1;border:2px solid #2E7D32;border-radius:12px;padding:20px 40px">
          <span style="font-size:36px;font-weight:bold;letter-spacing:10px;color:#2E7D32">${otp}</span>
        </div>
      </div>
      <p style="font-size:13px;color:#888;text-align:center">Este c\u00f3digo expira en <strong>15 minutos</strong>.</p>
      <hr style="border:none;border-top:1px solid #eee;margin:24px 0" />
      <p style="font-size:12px;color:#aaa;text-align:center">
        Si no solicitaste esto, ignor\u00e1 este mensaje.<br />
        &copy; ${new Date().getFullYear()} Guander
      </p>
    </div>
  `;
  try {
    await fetch('https://api.resend.com/emails', {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + env.RESEND_API_KEY,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        from: 'Guander <noreply@guander.site>',
        to: [email],
        subject: '\uD83D\uDD12 C\u00f3digo para restablecer tu contrase\u00f1a',
        html,
      }),
    });
  } catch (_) {
    // Non-blocking
  }
}

async function sendWelcomeEmail(env, email, name) {
  if (!env.RESEND_API_KEY) return;
  const displayName = name ? name : 'Guandero';
  const html = `
    <div style="font-family:sans-serif;max-width:520px;margin:0 auto;padding:32px 24px;background:#fff;border-radius:12px">
      <div style="text-align:center;margin-bottom:24px">
        <h1 style="color:#2E7D32;font-size:26px;margin:0">\uD83C\uDF1F \u00a1Bienvenido a Guander!</h1>
      </div>
      <p style="font-size:16px;color:#333">Hola <strong>${displayName}</strong>,</p>
      <p style="font-size:15px;color:#555;line-height:1.6">
        Tu cuenta fue creada exitosamente. Ya pod\u00e9s escanear QR en locales afiliados,
        acumular puntos y canjear recompensas exclusivas.
      </p>
      <div style="text-align:center;margin:32px 0">
        <a href="https://guander.app" style="background:#2E7D32;color:#fff;padding:14px 32px;border-radius:8px;text-decoration:none;font-size:15px;font-weight:bold">Abrir Guander</a>
      </div>
      <hr style="border:none;border-top:1px solid #eee;margin:24px 0" />
      <p style="font-size:12px;color:#aaa;text-align:center">
        Si no creaste esta cuenta, ignor\u00e1 este mensaje.<br />
        &copy; ${new Date().getFullYear()} Guander
      </p>
    </div>
  `;
  try {
    await fetch('https://api.resend.com/emails', {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + env.RESEND_API_KEY,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        from: 'Guander <onboarding@resend.dev>',
        to: [email],
        subject: '\uD83C\uDF1F \u00a1Bienvenido a Guander!',
        html,
      }),
    });
  } catch (_) {
    // Non-blocking: registration still succeeds if email fails
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
            (SELECT COUNT(*) FROM coupon_buy_store WHERE fk_customer_id = ?) +
            (SELECT COUNT(*) FROM coupon_buy_prof WHERE fk_customer_id = ?) AS total
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

// ── Helper: ensure points_history table exists ──────────────────────────────
async function ensurePointsHistoryTable(env) {
  await env.DB.prepare(`
    CREATE TABLE IF NOT EXISTS points_history (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      fk_customer INTEGER NOT NULL,
      description TEXT NOT NULL,
      points_change INTEGER NOT NULL,
      redemption_code TEXT,
      created_at TEXT DEFAULT CURRENT_TIMESTAMP
    )
  `).run().catch(() => {});
}

// ── Helper: get customer row for email ──────────────────────────────────────
async function getCustomerByEmail(env, email) {
  const userData = await env.DB.prepare(`
    SELECT u.id_user FROM user_data ud
    JOIN users u ON u.fk_user_data = ud.id_user_data
    WHERE LOWER(ud.email) = LOWER(?)
    ORDER BY u.id_user DESC LIMIT 1
  `).bind(email).first();
  if (!userData) return null;

  const customer = await env.DB.prepare(
    'SELECT id_customer, points FROM customer WHERE fk_user = ? ORDER BY points DESC LIMIT 1'
  ).bind(userData.id_user).first();
  return customer || null;
}

// ── GET /rewards?email= ──────────────────────────────────────────────────────
async function handleGetRewards(request, env, url) {
  const email = url.searchParams.get('email');
  if (!email) return corsResponse({ error: 'Email required' }, 400);

  await ensurePointsHistoryTable(env);

  try {
    // Find user — if no customer row exists yet, return 0 points with rewards still visible
    const userData = await env.DB.prepare(`
      SELECT u.id_user FROM user_data ud
      JOIN users u ON u.fk_user_data = ud.id_user_data
      WHERE LOWER(ud.email) = LOWER(?)
      ORDER BY u.id_user DESC LIMIT 1
    `).bind(email).first();

    let customerId = null;
    let points = 0;
    if (userData) {
      const customerRow = await env.DB.prepare(
        'SELECT id_customer, points FROM customer WHERE fk_user = ? ORDER BY points DESC LIMIT 1'
      ).bind(userData.id_user).first();
      if (customerRow) {
        customerId = customerRow.id_customer;
        points = customerRow.points || 0;
      }
    }

    let rewards = [];
    try {
      const alreadyBoughtStore = customerId
        ? `AND id_coupon NOT IN (SELECT fk_coupon_id FROM coupon_buy_store WHERE fk_customer_id = ${customerId})`
        : '';
      const alreadyBoughtProf = customerId
        ? `AND id_coupon NOT IN (SELECT fk_coupon_prof_id FROM coupon_buy_prof WHERE fk_customer_id = ${customerId})`
        : '';

      const { results } = await env.DB.prepare(`
        SELECT id_coupon AS id, name, description, point_req,
               code_coupon, fk_store, 'store' AS type
        FROM coupon_store
        WHERE (state IS NULL OR state = 1)
          AND (fk_coupon_state IS NULL OR fk_coupon_state = 1)
          AND (expiration_date IS NULL OR expiration_date >= date('now'))
          ${alreadyBoughtStore}

        UNION ALL

        SELECT id_coupon AS id, name, description, point_req,
               code_coupon, 0 AS fk_store, 'prof' AS type
        FROM coupon_prof
        WHERE (fk_coupon_state IS NULL OR fk_coupon_state = 1)
          AND (expiration_date IS NULL OR expiration_date >= date('now'))
          ${alreadyBoughtProf}

        ORDER BY point_req ASC
      `).all();
      rewards = results || [];
    } catch (e) { rewards = []; }

    return corsResponse({ points, rewards });
  } catch (err) {
    return corsResponse({ error: err.message }, 500);
  }
}

// ── POST /redeem ─────────────────────────────────────────────────────────────
async function handlePostRedeem(request, env) {
  let body;
  try { body = await request.json(); } catch { return corsResponse({ error: 'Invalid JSON' }, 400); }

  const { email, couponId, couponType } = body;
  if (!email || !couponId || !couponType) {
    return corsResponse({ error: 'email, couponId y couponType son requeridos' }, 400);
  }

  await ensurePointsHistoryTable(env);

  try {
    const customer = await getCustomerByEmail(env, email);
    if (!customer) return corsResponse({ error: 'Usuario no encontrado' }, 404);

    let coupon;
    if (couponType === 'store') {
      coupon = await env.DB.prepare(
        'SELECT id_coupon, name, point_req, code_coupon FROM coupon_store WHERE id_coupon = ? AND state = 1 AND fk_coupon_state = 1'
      ).bind(couponId).first();
    } else {
      coupon = await env.DB.prepare(
        'SELECT id_coupon, name, point_req, code_coupon FROM coupon_prof WHERE id_coupon = ? AND fk_coupon_state = 1'
      ).bind(couponId).first();
    }

    if (!coupon) return corsResponse({ error: 'Cupón no disponible' }, 404);

    // One coupon per person check
    if (couponType === 'store') {
      const alreadyRedeemed = await env.DB.prepare(
        'SELECT fk_customer_id FROM coupon_buy_store WHERE fk_customer_id = ? AND fk_coupon_id = ? LIMIT 1'
      ).bind(customer.id_customer, couponId).first();
      if (alreadyRedeemed) return corsResponse({ error: 'Ya canjeaste este cupón' }, 400);
    }

    if (customer.points < coupon.point_req) {
      return corsResponse({ error: 'Puntos insuficientes' }, 400);
    }

    const newPoints = customer.points - coupon.point_req;
    await env.DB.prepare('UPDATE customer SET points = ? WHERE id_customer = ?')
      .bind(newPoints, customer.id_customer).run();

    if (couponType === 'store') {
      await env.DB.prepare(
        'INSERT INTO coupon_buy_store (fk_customer_id, fk_coupon_id) VALUES (?, ?)'
      ).bind(customer.id_customer, coupon.id_coupon).run();
    } else {
      await env.DB.prepare(
        'INSERT INTO coupon_buy_prof (fk_coupon_prof_id, fk_customer_id) VALUES (?, ?)'
      ).bind(coupon.id_coupon, customer.id_customer).run();
    }

    await env.DB.prepare(
      'INSERT INTO points_history (fk_customer, description, points_change, redemption_code) VALUES (?, ?, ?, ?)'
    ).bind(customer.id_customer, 'Canjeado: ' + coupon.name, -coupon.point_req, coupon.code_coupon).run();

    return corsResponse({ success: true, code: coupon.code_coupon, remainingPoints: newPoints });
  } catch (err) {
    return corsResponse({ error: err.message }, 500);
  }
}

// ── GET /redeem-history?email= ───────────────────────────────────────────────
async function handleGetRedeemHistory(request, env, url) {
  const email = url.searchParams.get('email');
  if (!email) return corsResponse({ error: 'Email required' }, 400);

  await ensurePointsHistoryTable(env);

  try {
    const customer = await getCustomerByEmail(env, email);
    if (!customer) return corsResponse({ history: [] });

    const { results } = await env.DB.prepare(`
      SELECT
        description AS name,
        points_change,
        redemption_code,
        CASE
          WHEN julianday('now') - julianday(created_at) < 1 THEN 'Hoy'
          WHEN julianday('now') - julianday(created_at) < 2 THEN 'Hace 1 día'
          ELSE 'Hace ' || CAST(CAST(julianday('now') - julianday(created_at) AS INTEGER) AS TEXT) || ' días'
        END AS date
      FROM points_history
      WHERE fk_customer = ?
      ORDER BY id DESC
      LIMIT 30
    `).bind(customer.id_customer).all();

    return corsResponse({ history: results || [] });
  } catch (err) {
    return corsResponse({ error: err.message }, 500);
  }
}

// ── Helper: haversine distance in km ─────────────────────────────────────────
function haversineKm(lat1, lng1, lat2, lng2) {
  const R = 6371;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLng = (lng2 - lng1) * Math.PI / 180;
  const a = Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLng / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

// ── Helper: map store category name → app category slug ─────────────────────
function mapStoreCategory(catName) {
  const n = (catName || '').toLowerCase();
  if (n.includes('café') || n.includes('cafe') || n.includes('coffee') ||
      n.includes('restaurant') || n.includes('bar') || n.includes('brunch')) return 'restaurant';
  if (n.includes('spa') || n.includes('grooming') || n.includes('foto') ||
      n.includes('photo') || n.includes('hotel') || n.includes('servic')) return 'service';
  return 'store';
}

// ── Helper: parse "lat,lng" location string ──────────────────────────────────
function parseLoc(loc) {
  if (!loc) return { lat: -34.6037, lng: -58.3816 };
  const parts = loc.split(',');
  return {
    lat: parseFloat(parts[0]) || -34.6037,
    lng: parseFloat(parts[1]) || -58.3816
  };
}

// ── GET /store-photos?storeId= ───────────────────────────────────────────────
async function handleGetStorePhotos(request, env, url) {
  const storeId = parseInt(url.searchParams.get('storeId') || '0');
  if (!storeId) return corsResponse({ photos: [] });

  try {
    const store = await env.DB.prepare(
      `SELECT image_url, gallery_urls FROM stores WHERE id_store = ?`
    ).bind(storeId).first().catch(() => null);

    if (!store) return corsResponse({ photos: [] });

    const photos = [];

    // gallery_urls is a JSON array string like ["url1","url2",...]
    let gallery = [];
    try {
      if (store.gallery_urls) {
        gallery = JSON.parse(store.gallery_urls);
        if (!Array.isArray(gallery)) gallery = [];
      }
    } catch (_) { gallery = []; }

    // The gallery already contains the primary photo first (as uploaded by the dashboard).
    // Add all gallery photos, then add image_url only if not already present.
    for (const u of gallery) {
      if (u && !photos.includes(u)) photos.push(u);
    }
    if (store.image_url && !photos.includes(store.image_url)) {
      photos.unshift(store.image_url);
    }

    return corsResponse({ photos });
  } catch (err) {
    return corsResponse({ photos: [] });
  }
}

// ── GET /places?lat=&lng= ────────────────────────────────────────────────────
async function handleGetPlaces(request, env, url) {
  const userLat = parseFloat(url.searchParams.get('lat') || '-34.6037');
  const userLng = parseFloat(url.searchParams.get('lng') || '-58.3816');

  try {
    // Query real stores table with optional category join
    const { results: storeRows } = await env.DB.prepare(`
      SELECT s.id_store AS id_place, s.name, s.description, s.address, s.location,
             COALESCE(cat.name, '') AS cat_name, 'store' AS place_type,
             COALESCE(s.image_url, '') AS photo_url,
             s.gallery_urls,
             COALESCE(s.stars, 0) AS stars,
             COALESCE(sch.week, '') AS sched_week,
             COALESCE(sch.weekend, '') AS sched_weekend,
             COALESCE(sch.sunday, '') AS sched_sunday,
             COALESCE(s.social_whatsapp, '') AS whatsapp,
             COALESCE(s.social_web, '') AS website,
             COALESCE(s.social_instagram, '') AS instagram,
             COALESCE(s.social_twitter, '') AS twitter
      FROM stores s
      LEFT JOIN category cat ON cat.id_category = s.fk_category
      LEFT JOIN schedule sch ON sch.id_schedule = s.fk_schedule
    `).all().catch(() => ({ results: [] }));

    // Query real professionals table (name comes from fk_user_id → users → user_data)
    const { results: profRows } = await env.DB.prepare(`
      SELECT p.id_professional AS id_place,
             COALESCE(ud.name, p.description) AS name,
             p.description, p.address, p.location,
             '' AS cat_name, 'professional' AS place_type
      FROM professionals p
      LEFT JOIN users u ON u.id_user = p.fk_user_id
      LEFT JOIN user_data ud ON ud.id_user_data = u.fk_user_data
    `).all().catch(() => ({ results: [] }));

    const allPlaces = [
      ...(storeRows || []).map(s => {
        const { lat, lng } = parseLoc(s.location);
        // Build ordered photo list: gallery first, then image_url as fallback
        let galleryList = [];
        try {
          if (s.gallery_urls) {
            const parsed = JSON.parse(s.gallery_urls);
            if (Array.isArray(parsed)) galleryList = parsed.filter(Boolean);
          }
        } catch (_) {}
        if (galleryList.length === 0 && s.photo_url) galleryList = [s.photo_url];
        return {
          id_place: s.id_place,
          name: s.name,
          description: s.description || '',
          address: s.address || '',
          photo_url: galleryList[0] || s.photo_url || '',
          gallery_urls: JSON.stringify(galleryList),
          stars: s.stars || 0,
          sched_week: s.sched_week || '',
          sched_weekend: s.sched_weekend || '',
          sched_sunday: s.sched_sunday || '',
          whatsapp: s.whatsapp || '',
          website: s.website || '',
          instagram: s.instagram || '',
          twitter: s.twitter || '',
          category: mapStoreCategory(s.cat_name),
          lat, lng,
          points_reward: 50,
          is_open: 1,
          place_type: 'store',
          distance_km: Math.round(haversineKm(userLat, userLng, lat, lng) * 10) / 10
        };
      }),
      ...(profRows || []).map(p => {
        const { lat, lng } = parseLoc(p.location);
        return {
          id_place: p.id_place,
          name: p.name,
          description: p.description || '',
          address: p.address || '',
          lat, lng,
          points_reward: 50,
          is_open: 1,
          place_type: 'professional',
          distance_km: Math.round(haversineKm(userLat, userLng, lat, lng) * 10) / 10
        };
      })
    ];

    allPlaces.sort((a, b) => a.distance_km - b.distance_km);
    return corsResponse({ places: allPlaces });
  } catch (err) {
    return corsResponse({ error: err.message }, 500);
  }
}

// ── GET /comments?placeId=&placeType=&email= ─────────────────────────────────
async function handleGetComments(request, env, url) {
  const placeId = parseInt(url.searchParams.get('placeId') || '0');
  const placeType = url.searchParams.get('placeType') || 'store';
  const email = url.searchParams.get('email') || '';
  if (!placeId) return corsResponse({ error: 'placeId required' }, 400);

  const dateExpr = (col) => `
    CASE
      WHEN julianday('now') - julianday(${col}) < 1 THEN 'Hoy'
      WHEN julianday('now') - julianday(${col}) < 2 THEN 'Hace 1 día'
      ELSE 'Hace ' || CAST(CAST(julianday('now') - julianday(${col}) AS INTEGER) AS TEXT) || ' días'
    END`;

  const authorJoin = `
    LEFT JOIN customer cust ON cust.id_customer = cm.fk_customer_id
    LEFT JOIN users us ON us.id_user = cust.fk_user
    LEFT JOIN user_data ud ON ud.id_user_data = us.fk_user_data`;

  try {
    let placeName = '';
    let comments = [];

    if (placeType === 'professional') {
      const place = await env.DB.prepare(
        `SELECT COALESCE(ud.name, p.description) AS name
         FROM professionals p
         LEFT JOIN users u ON u.id_user = p.fk_user_id
         LEFT JOIN user_data ud ON ud.id_user_data = u.fk_user_data
         WHERE p.id_professional = ?`
      ).bind(placeId).first().catch(() => null);
      placeName = place ? place.name : '';

      const { results } = await env.DB.prepare(`
        SELECT cm.id_comment AS id,
               COALESCE(ud.name, 'Usuario') AS authorName,
               cm.stars AS rating,
               cm.body AS comment,
               ${dateExpr('cm.date')} AS date
        FROM comments_prof cm
        ${authorJoin}
        WHERE cm.fk_professional_id = ?
        ORDER BY cm.id_comment DESC
      `).bind(placeId).all().catch(() => ({ results: [] }));
      comments = results || [];
    } else {
      const place = await env.DB.prepare(
        'SELECT name FROM stores WHERE id_store = ?'
      ).bind(placeId).first().catch(() => null);
      placeName = place ? place.name : '';

      const { results } = await env.DB.prepare(`
        SELECT cm.id_comment AS id,
               COALESCE(ud.name, 'Usuario') AS authorName,
               cm.stars AS rating,
               cm.body AS comment,
               ${dateExpr('cm.date')} AS date
        FROM comments_store cm
        ${authorJoin}
        WHERE cm.fk_store_id = ?
        ORDER BY cm.id_comment DESC
      `).bind(placeId).all().catch(() => ({ results: [] }));
      comments = results || [];
    }

    let canComment = false;
    let alreadyCommented = false;
    if (email) {
      const customer = await getCustomerByEmail(env, email);
      if (customer) {
        const table = placeType === 'professional' ? 'prof_purchase' : 'store_purchase';
        const fkCol = placeType === 'professional' ? 'fk_professional' : 'fk_store';
        const custCol = 'fk_customer';
        const check = await env.DB.prepare(
          `SELECT 1 FROM ${table} WHERE ${custCol} = ? AND ${fkCol} = ? LIMIT 1`
        ).bind(customer.id_customer, placeId).first().catch(() => null);
        canComment = !!check;

        if (canComment) {
          const commentTable = placeType === 'professional' ? 'comments_prof' : 'comments_store';
          const commentFkCol = placeType === 'professional' ? 'fk_professional_id' : 'fk_store_id';
          const existing = await env.DB.prepare(
            `SELECT 1 FROM ${commentTable} WHERE fk_customer_id = ? AND ${commentFkCol} = ? LIMIT 1`
          ).bind(customer.id_customer, placeId).first().catch(() => null);
          if (existing) {
            canComment = false;
            alreadyCommented = true;
          }
        }
      }
    }

    return corsResponse({ placeId, placeName, totalComments: comments.length, canComment, alreadyCommented, comments });
  } catch (err) {
    return corsResponse({ error: err.message }, 500);
  }
}

// ── POST /review ──────────────────────────────────────────────────────────────
async function handlePostReview(request, env) {
  let body;
  try { body = await request.json(); } catch { return corsResponse({ error: 'Invalid JSON' }, 400); }

  const { email, placeId, placeType, rating, comment } = body;
  if (!email || !placeId || !rating || !comment) {
    return corsResponse({ error: 'Faltan campos requeridos' }, 400);
  }

  try {
    const customer = await getCustomerByEmail(env, email);
    if (!customer) return corsResponse({ error: 'Usuario no encontrado' }, 404);

    // Prevent duplicate reviews
    const commentTable = placeType === 'professional' ? 'comments_prof' : 'comments_store';
    const commentFkCol = placeType === 'professional' ? 'fk_professional_id' : 'fk_store_id';
    const existingReview = await env.DB.prepare(
      `SELECT 1 FROM ${commentTable} WHERE fk_customer_id = ? AND ${commentFkCol} = ? LIMIT 1`
    ).bind(customer.id_customer, parseInt(placeId)).first().catch(() => null);
    if (existingReview) return corsResponse({ error: 'Ya dejaste una reseña en este lugar' }, 409);

    const now = new Date().toISOString().replace('T', ' ').substring(0, 19);

    if (placeType === 'professional') {
      await env.DB.prepare(
        'INSERT INTO comments_prof (body, stars, date, fk_customer_id, fk_professional_id) VALUES (?, ?, ?, ?, ?)'
      ).bind(comment, parseInt(rating), now, customer.id_customer, parseInt(placeId)).run();
    } else {
      await env.DB.prepare(
        'INSERT INTO comments_store (body, stars, date, fk_customer_id, fk_store_id) VALUES (?, ?, ?, ?, ?)'
      ).bind(comment, parseInt(rating), now, customer.id_customer, parseInt(placeId)).run();
    }

    return corsResponse({ success: true });
  } catch (err) {
    return corsResponse({ error: err.message }, 500);
  }
}

// ── POST /validate-qr ──────────────────────────────────────────────────────
async function handleValidateQr(request, env) {
  let body;
  try { body = await request.json(); } catch { return corsResponse({ error: 'Invalid JSON' }, 400); }

  const { email, qrData } = body;
  if (!email || !qrData) return corsResponse({ error: 'Faltan campos' }, 400);

  try {
    let qr;
    try { qr = JSON.parse(qrData); } catch { return corsResponse({ error: 'QR inválido' }, 400); }

    const { type, id, name: storeName, amount, item, secret } = qr;
    if (secret !== 'guander2026') return corsResponse({ error: 'QR inválido' }, 400);
    if (!id || !amount) return corsResponse({ error: 'QR incompleto' }, 400);

    const customer = await getCustomerByEmail(env, email);
    if (!customer) return corsResponse({ error: 'Usuario no encontrado' }, 404);

    const pointsEarned = Math.max(1, Math.floor(parseInt(amount) / 1000));
    const now = new Date().toISOString().replace('T', ' ').substring(0, 19);

    if (type === 'professional') {
      await env.DB.prepare(
        'INSERT INTO prof_purchase (date, amount, points_earn, fk_professional, fk_customer) VALUES (?, ?, ?, ?, ?)'
      ).bind(now, parseInt(amount), pointsEarned, parseInt(id), customer.id_customer).run();
    } else {
      await env.DB.prepare(
        'INSERT INTO store_purchase (date, amount, points_earn, fk_customer, fk_store) VALUES (?, ?, ?, ?, ?)'
      ).bind(now, parseInt(amount), pointsEarned, customer.id_customer, parseInt(id)).run();
    }

    await env.DB.prepare(
      'UPDATE customer SET points = points + ? WHERE id_customer = ?'
    ).bind(pointsEarned, customer.id_customer).run();

    await ensurePointsHistoryTable(env);
    await env.DB.prepare(
      'INSERT INTO points_history (fk_customer, description, points_change) VALUES (?, ?, ?)'
    ).bind(customer.id_customer, 'Consumo en ' + (storeName || 'local afiliado'), pointsEarned).run();

    const updated = await env.DB.prepare(
      'SELECT points FROM customer WHERE id_customer = ?'
    ).bind(customer.id_customer).first();

    return corsResponse({
      success: true,
      storeName: storeName || '',
      item: item || '',
      amount: parseInt(amount),
      pointsEarned,
      newBalance: updated ? updated.points : 0
    });
  } catch (err) {
    return corsResponse({ error: err.message }, 500);
  }
}
