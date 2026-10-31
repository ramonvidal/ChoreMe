// Publishes firestore.rules to the Firebase project named in the service account key
// (GOOGLE_APPLICATION_CREDENTIALS). Uses the Admin SDK, whose key (Firebase console:
// Project settings > Service accounts) can already manage rules: no extra roles needed.
import { readFileSync } from 'node:fs';
import { applicationDefault, initializeApp } from 'firebase-admin/app';
import { getSecurityRules } from 'firebase-admin/security-rules';

const KEEP = 20; // older published versions beyond this are tidied away

const key = JSON.parse(readFileSync(process.env.GOOGLE_APPLICATION_CREDENTIALS, 'utf8'));
initializeApp({ credential: applicationDefault(), projectId: key.project_id });
const rules = getSecurityRules();

const source = readFileSync(new URL('./firestore.rules', import.meta.url), 'utf8');
const released = await rules.releaseFirestoreRulesetFromSource(source);
console.log(`Published the rules to ${key.project_id} (${released.name})`);

// Each publish keeps a copy; Firebase allows a limited number, so drop the oldest.
const { rulesets } = await rules.listRulesetMetadata(100);
const old = rulesets
  .sort((a, b) => new Date(b.createTime) - new Date(a.createTime))
  .slice(KEEP)
  .filter((r) => r.name !== released.name);
for (const r of old) {
  try {
    await rules.deleteRuleset(r.name);
  } catch (e) {
    console.log(`Kept ${r.name}: ${e.message}`);
  }
}
