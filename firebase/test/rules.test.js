import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  arrayUnion,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  collection,
  setDoc,
  writeBatch,
} from 'firebase/firestore';

const FAMILY = 'fam1';
const CODE = 'ABC234';

let env;

const parent = (uid) => env.authenticatedContext(uid, { firebase: { sign_in_provider: 'google.com' } }).firestore();
const kidDevice = (uid) => env.authenticatedContext(uid, { firebase: { sign_in_provider: 'anonymous' } }).firestore();

// The same batch the app sends when a parent creates a family.
function createFamilyBatch(db, uid, familyId = FAMILY, code = CODE) {
  const batch = writeBatch(db);
  batch.set(doc(db, 'families', familyId), { name: 'Vidals', inviteCode: code, createdBy: uid });
  batch.set(doc(db, 'inviteCodes', code), { familyId, familyName: 'Vidals' });
  batch.set(doc(db, 'families', familyId, 'memberUids', uid), { memberId: uid, role: 'PARENT' });
  batch.set(doc(db, 'families', familyId, 'members', uid), { name: 'Ramon', role: 'PARENT', color: 0, uids: [uid] });
  batch.set(doc(db, 'users', uid), { familyId, familyName: 'Vidals' });
  return batch.commit();
}

function approveKidBatch(db, kidUid, memberId = 'kid-leo') {
  const batch = writeBatch(db);
  batch.set(doc(db, 'families', FAMILY, 'members', memberId), { name: 'Leo', role: 'KID', color: 1, uids: [kidUid] });
  batch.set(doc(db, 'families', FAMILY, 'memberUids', kidUid), { memberId, role: 'KID' });
  batch.delete(doc(db, 'families', FAMILY, 'joinRequests', kidUid));
  return batch.commit();
}

const joinRequest = (role, name = 'Leo') => ({ name, role, deviceName: 'Pixel', createdAt: 1 });

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-choreme',
    firestore: { rules: readFileSync('firestore.rules', 'utf8') },
  });
});

after(() => env.cleanup());

beforeEach(() => env.clearFirestore());

describe('creating a family', () => {
  test('a signed-in parent can create a family', async () => {
    await assertSucceeds(createFamilyBatch(parent('alice'), 'alice'));
  });

  test('an anonymous (kid) account cannot create a family', async () => {
    await assertFails(createFamilyBatch(kidDevice('kid1'), 'kid1'));
  });

  test('nobody can add themselves to a family someone else created', async () => {
    await createFamilyBatch(parent('alice'), 'alice');
    const db = parent('mallory');
    await assertFails(setDoc(doc(db, 'families', FAMILY, 'memberUids', 'mallory'), { memberId: 'mallory', role: 'PARENT' }));
    await assertFails(setDoc(doc(db, 'families', FAMILY, 'members', 'mallory'), { name: 'M', role: 'PARENT', uids: ['mallory'] }));
  });

  test('an invite code cannot be pointed at another family', async () => {
    await createFamilyBatch(parent('alice'), 'alice');
    await assertFails(setDoc(doc(parent('mallory'), 'inviteCodes', 'ZZZ999'), { familyId: FAMILY, familyName: 'x' }));
    await assertFails(setDoc(doc(parent('mallory'), 'inviteCodes', CODE), { familyId: 'other', familyName: 'x' }));
  });
});

describe('joining a family', () => {
  beforeEach(() => createFamilyBatch(parent('alice'), 'alice'));

  test('anyone signed in can look up an invite code', async () => {
    await assertSucceeds(getDoc(doc(kidDevice('kid1'), 'inviteCodes', CODE)));
  });

  test('outsiders cannot read the family or its members', async () => {
    const db = parent('mallory');
    await assertFails(getDoc(doc(db, 'families', FAMILY)));
    await assertFails(getDocs(collection(db, 'families', FAMILY, 'members')));
  });

  test('a kid device can ask to join as a kid but not as a parent', async () => {
    const db = kidDevice('kid1');
    await assertFails(setDoc(doc(db, 'families', FAMILY, 'joinRequests', 'kid1'), joinRequest('PARENT')));
    await assertSucceeds(setDoc(doc(db, 'families', FAMILY, 'joinRequests', 'kid1'), joinRequest('KID')));
  });

  test('nobody can file a request on behalf of someone else', async () => {
    await assertFails(setDoc(doc(parent('mallory'), 'families', FAMILY, 'joinRequests', 'bob'), joinRequest('PARENT')));
  });

  test('a pending request does not give access', async () => {
    const db = kidDevice('kid1');
    await setDoc(doc(db, 'families', FAMILY, 'joinRequests', 'kid1'), joinRequest('KID'));
    await assertFails(getDocs(collection(db, 'families', FAMILY, 'members')));
    await assertFails(setDoc(doc(db, 'families', FAMILY, 'memberUids', 'kid1'), { memberId: 'kid1', role: 'KID' }));
    // ...but the requester can watch their own request and membership.
    await assertSucceeds(getDoc(doc(db, 'families', FAMILY, 'joinRequests', 'kid1')));
    await assertSucceeds(getDoc(doc(db, 'families', FAMILY, 'memberUids', 'kid1')));
  });

  test('a parent approves a kid, who can then see the family', async () => {
    await setDoc(doc(kidDevice('kid1'), 'families', FAMILY, 'joinRequests', 'kid1'), joinRequest('KID'));
    await assertSucceeds(getDocs(collection(parent('alice'), 'families', FAMILY, 'joinRequests')));
    await assertSucceeds(approveKidBatch(parent('alice'), 'kid1'));
    await assertSucceeds(getDocs(collection(kidDevice('kid1'), 'families', FAMILY, 'members')));
  });

  test('a parent can link a second device to an existing kid profile', async () => {
    await setDoc(doc(kidDevice('kid1'), 'families', FAMILY, 'joinRequests', 'kid1'), joinRequest('KID'));
    await approveKidBatch(parent('alice'), 'kid1');
    await setDoc(doc(kidDevice('kid2'), 'families', FAMILY, 'joinRequests', 'kid2'), joinRequest('KID'));
    const db = parent('alice');
    const batch = writeBatch(db);
    batch.update(doc(db, 'families', FAMILY, 'members', 'kid-leo'), { uids: arrayUnion('kid2') });
    batch.set(doc(db, 'families', FAMILY, 'memberUids', 'kid2'), { memberId: 'kid-leo', role: 'KID' });
    batch.delete(doc(db, 'families', FAMILY, 'joinRequests', 'kid2'));
    await assertSucceeds(batch.commit());
  });
});

describe('kids inside the family', () => {
  beforeEach(async () => {
    await createFamilyBatch(parent('alice'), 'alice');
    await setDoc(doc(kidDevice('kid1'), 'families', FAMILY, 'joinRequests', 'kid1'), joinRequest('KID'));
    await approveKidBatch(parent('alice'), 'kid1');
  });

  test('kids cannot approve join requests', async () => {
    await setDoc(doc(parent('bob'), 'families', FAMILY, 'joinRequests', 'bob'), joinRequest('PARENT', 'Bob'));
    const db = kidDevice('kid1');
    await assertFails(getDocs(collection(db, 'families', FAMILY, 'joinRequests')));
    await assertFails(setDoc(doc(db, 'families', FAMILY, 'memberUids', 'bob'), { memberId: 'bob', role: 'PARENT' }));
  });

  test('kids cannot promote themselves or remove members', async () => {
    const db = kidDevice('kid1');
    await assertFails(setDoc(doc(db, 'families', FAMILY, 'members', 'kid-leo'), { name: 'Leo', role: 'PARENT', uids: ['kid1'] }));
    await assertFails(deleteDoc(doc(db, 'families', FAMILY, 'members', 'alice')));
    await assertFails(deleteDoc(doc(db, 'families', FAMILY, 'memberUids', 'alice')));
  });

  test('a parent can remove a kid device, which loses access', async () => {
    await assertSucceeds(deleteDoc(doc(parent('alice'), 'families', FAMILY, 'memberUids', 'kid1')));
    await assertFails(getDocs(collection(kidDevice('kid1'), 'families', FAMILY, 'members')));
  });
});

describe('user pointers', () => {
  test('users can only write their own pointer', async () => {
    await assertSucceeds(setDoc(doc(parent('bob'), 'users', 'bob'), { familyId: FAMILY }));
    await assertFails(setDoc(doc(parent('bob'), 'users', 'alice'), { familyId: 'x' }));
    await assertFails(getDoc(doc(parent('bob'), 'users', 'alice')));
  });
});
