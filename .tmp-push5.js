const https = require('https');
const fs = require('fs');
const TOKEN = process.env.GITHUB_TOKEN;
const OWNER = 'illheartdark', REPO = 'feishudaka';

function once(method, p, body) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null;
    const headers = { 'User-Agent': 'daka', 'Accept': 'application/vnd.github+json', 'Authorization': 'Bearer ' + TOKEN };
    if (data) { headers['Content-Type'] = 'application/json'; headers['Content-Length'] = Buffer.byteLength(data); }
    const req = https.request({ host: 'api.github.com', path: p, method, headers, family: 4 }, res => {
      let d = ''; res.on('data', c => d += c);
      res.on('end', () => { let j = null; try { j = JSON.parse(d); } catch (e) {} resolve({ code: res.statusCode, json: j, raw: d }); });
    });
    req.on('error', reject);
    req.setTimeout(60000, () => req.destroy(new Error('timeout')));
    if (data) req.write(data);
    req.end();
  });
}

async function api(method, p, body) {
  let last;
  for (let i = 1; i <= 5; i++) {
    try { return await once(method, p, body); }
    catch (e) { last = e; await new Promise(r => setTimeout(r, 1500 * i)); }
  }
  throw last;
}

(async () => {
  const parsed = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
  const commits = Array.isArray(parsed) ? parsed : [parsed];
  let parent = process.env.INITIAL_PARENT || null;
  console.log('commits:', commits.length, 'base:', (parent || 'none').slice(0, 7));
  for (const c of commits) {
    const tree = [];
    for (const f of c.files) {
      const b = await api('POST', `/repos/${OWNER}/${REPO}/git/blobs`, { content: f.b64, encoding: 'base64' });
      if (b.code !== 201) { console.log('BLOB FAIL', f.path, b.code, b.raw.slice(0, 120)); process.exit(1); }
      tree.push({ path: f.path, mode: '100644', type: 'blob', sha: b.json.sha });
    }
    const t = await api('POST', `/repos/${OWNER}/${REPO}/git/trees`, { tree });
    if (t.code !== 201) { console.log('TREE FAIL', t.code, t.raw.slice(0, 120)); process.exit(1); }
    const payload = { message: c.message, tree: t.json.sha, author: { name: c.name, email: c.email, date: c.date }, committer: { name: c.name, email: c.email, date: c.date } };
    if (parent) payload.parents = [parent];
    const cm = await api('POST', `/repos/${OWNER}/${REPO}/git/commits`, payload);
    if (cm.code !== 201) { console.log('COMMIT FAIL', cm.code, cm.raw.slice(0, 120)); process.exit(1); }
    parent = cm.json.sha;
    console.log('  ok', cm.json.sha.slice(0, 7));
  }
  const ref = await api('PATCH', `/repos/${OWNER}/${REPO}/git/refs/heads/main`, { sha: parent, force: true });
  console.log('ref', ref.code, 'HEAD=' + parent);
})().catch(e => { console.log('ERROR', e.message); process.exit(1); });
