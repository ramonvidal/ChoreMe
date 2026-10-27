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
  updateDoc,
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

describe('grocery list', () => {
  const milk = { name: 'Milk', category: 'DAIRY_EGGS', isNeeded: true, isStaple: true };

  beforeEach(async () => {
    await createFamilyBatch(parent('alice'), 'alice');
    await setDoc(doc(kidDevice('kid1'), 'families', FAMILY, 'joinRequests', 'kid1'), joinRequest('KID'));
    await approveKidBatch(parent('alice'), 'kid1');
  });

  test('kids can add items and tick them off', async () => {
    const db = kidDevice('kid1');
    const ref = doc(db, 'families', FAMILY, 'groceryItems', 'milk');
    await assertSucceeds(setDoc(ref, milk));
    await assertSucceeds(updateDoc(ref, { isNeeded: false }));
    await assertSucceeds(getDocs(collection(db, 'families', FAMILY, 'groceryItems')));
  });

  test('outsiders and pending devices cannot see or change the list', async () => {
    await setDoc(doc(parent('alice'), 'families', FAMILY, 'groceryItems', 'milk'), milk);
    await setDoc(doc(kidDevice('kid2'), 'families', FAMILY, 'joinRequests', 'kid2'), joinRequest('KID'));
    for (const db of [parent('mallory'), kidDevice('kid2')]) {
      await assertFails(getDocs(collection(db, 'families', FAMILY, 'groceryItems')));
      await assertFails(setDoc(doc(db, 'families', FAMILY, 'groceryItems', 'beer'), { ...milk, name: 'Beer' }));
      await assertFails(deleteDoc(doc(db, 'families', FAMILY, 'groceryItems', 'milk')));
    }
  });

  test('malformed items are rejected', async () => {
    const db = parent('alice');
    const ref = doc(db, 'families', FAMILY, 'groceryItems', 'x');
    await assertFails(setDoc(ref, { ...milk, name: '' }));
    await assertFails(setDoc(ref, { ...milk, name: 'x'.repeat(61) }));
    await assertFails(setDoc(ref, { ...milk, isNeeded: 'yes' }));
    await assertFails(setDoc(ref, { name: 'Milk' }));
  });
});

describe('chores', () => {
  const chore = {
    title: 'Wash dishes', template: 'DISHES', recurrenceType: 'EVERY_DAYS', everyDays: 1, weekDays: [],
    dueDate: '2026-10-24', status: 'OPEN', claimedBy: null, completedBy: null, approvedBy: null,
    lastDoneBy: null, lastDoneOn: null, createdBy: 'alice',
  };
  const ref = (db) => doc(db, 'families', FAMILY, 'chores', 'dishes');

  beforeEach(async () => {
    await createFamilyBatch(parent('alice'), 'alice');
    for (const kid of ['kid1', 'kid2']) {
      await setDoc(doc(kidDevice(kid), 'families', FAMILY, 'joinRequests', kid), joinRequest('KID'));
      await approveKidBatch(parent('alice'), kid, `member-${kid}`);
    }
    await setDoc(ref(parent('alice')), chore);
  });

  test('anyone can report a chore, in their own name', async () => {
    const kid = kidDevice('kid1');
    await assertSucceeds(setDoc(doc(kid, 'families', FAMILY, 'chores', 'new'), { ...chore, createdBy: 'member-kid1' }));
    await assertFails(setDoc(doc(kid, 'families', FAMILY, 'chores', 'fake'), { ...chore, createdBy: 'alice' }));
    // A kid can't sneak in a chore that's already done or approved.
    await assertFails(setDoc(doc(kid, 'families', FAMILY, 'chores', 'done'), { ...chore, createdBy: 'member-kid1', status: 'DONE' }));
    await assertFails(setDoc(doc(kid, 'families', FAMILY, 'chores', 'mine'), { ...chore, createdBy: 'member-kid1', claimedBy: 'member-kid2' }));
  });

  test('kids can take back their own open reports, parents can delete any', async () => {
    const kid = kidDevice('kid1');
    await setDoc(doc(kid, 'families', FAMILY, 'chores', 'mine'), { ...chore, createdBy: 'member-kid1' });
    await assertFails(deleteDoc(ref(kid)));
    await assertFails(deleteDoc(doc(kidDevice('kid2'), 'families', FAMILY, 'chores', 'mine')));
    await assertSucceeds(deleteDoc(doc(kid, 'families', FAMILY, 'chores', 'mine')));
    await assertSucceeds(deleteDoc(ref(parent('alice'))));
  });

  test('saved chores: anyone saves one in their own name; the saver or a parent removes it', async () => {
    const saved = (db, id) => doc(db, 'families', FAMILY, 'choreTemplates', id);
    const feedCat = { title: 'Feed the cat', createdBy: 'member-kid1' };
    await assertSucceeds(setDoc(saved(kidDevice('kid1'), 'feed-the-cat'), feedCat));
    await assertFails(setDoc(saved(kidDevice('kid2'), 'x'), feedCat));
    await assertFails(setDoc(saved(kidDevice('kid1'), 'y'), { ...feedCat, title: '' }));
    await assertSucceeds(getDocs(collection(kidDevice('kid2'), 'families', FAMILY, 'choreTemplates')));
    await assertFails(deleteDoc(saved(kidDevice('kid2'), 'feed-the-cat')));
    await assertSucceeds(deleteDoc(saved(parent('alice'), 'feed-the-cat')));
  });

  test('a kid grabs, lets go, then finishes a chore for approval', async () => {
    const db = kidDevice('kid1');
    await assertSucceeds(updateDoc(ref(db), { status: 'CLAIMED', claimedBy: 'member-kid1' }));
    await assertSucceeds(updateDoc(ref(db), { status: 'OPEN', claimedBy: null }));
    await assertSucceeds(updateDoc(ref(db), { status: 'PENDING_APPROVAL', claimedBy: 'member-kid1', completedBy: 'member-kid1' }));
  });

  test('a kid cannot grab a chore in someone else\'s name or take over a sibling\'s', async () => {
    await assertFails(updateDoc(ref(kidDevice('kid1')), { status: 'CLAIMED', claimedBy: 'member-kid2' }));
    await updateDoc(ref(kidDevice('kid2')), { status: 'CLAIMED', claimedBy: 'member-kid2' });
    await assertFails(updateDoc(ref(kidDevice('kid1')), { status: 'OPEN', claimedBy: null }));
    await assertFails(updateDoc(ref(kidDevice('kid1')), { status: 'PENDING_APPROVAL', claimedBy: 'member-kid1', completedBy: 'member-kid1' }));
  });

  test('kids join or leave a chore done together, only ever in their own name', async () => {
    const leo = kidDevice('kid1');
    const mia = kidDevice('kid2');
    await updateDoc(ref(leo), { status: 'CLAIMED', claimedBy: 'member-kid1' });
    await assertFails(updateDoc(ref(leo), { helpers: ['member-kid2'] })); // can't sign up a sibling
    await assertSucceeds(updateDoc(ref(mia), { helpers: ['member-kid2'] }));
    await assertFails(updateDoc(ref(mia), { claimedBy: 'member-kid2', helpers: [] })); // can't take it over
    await assertSucceeds(updateDoc(ref(mia), { helpers: [] }));
    await assertSucceeds(updateDoc(ref(mia), { helpers: ['member-kid2'] }));
    // Either can say it's done, and nobody is dropped on the way.
    await assertFails(updateDoc(ref(mia), { status: 'PENDING_APPROVAL', completedBy: 'member-kid2', helpers: [] }));
    await assertSucceeds(updateDoc(ref(mia), { status: 'PENDING_APPROVAL', completedBy: 'member-kid2' }));
  });

  test('a kid letting go hands a shared chore to whoever is doing it with them', async () => {
    const leo = kidDevice('kid1');
    await updateDoc(ref(leo), { status: 'CLAIMED', claimedBy: 'member-kid1' });
    await updateDoc(ref(kidDevice('kid2')), { helpers: ['member-kid2'] });
    await assertFails(updateDoc(ref(leo), { status: 'OPEN', claimedBy: null })); // the helper is still on it
    await assertSucceeds(updateDoc(ref(leo), { claimedBy: 'member-kid2', helpers: [] }));
  });

  test('parents say who does a chore, several people at once', async () => {
    await assertSucceeds(updateDoc(ref(parent('alice')), { status: 'CLAIMED', claimedBy: 'member-kid1', helpers: ['member-kid2'] }));
    await assertFails(updateDoc(ref(parent('alice')), { helpers: 'member-kid2' }));
    await assertFails(setDoc(doc(kidDevice('kid1'), 'families', FAMILY, 'chores', 'new'), { ...chore, createdBy: 'member-kid1', helpers: ['member-kid2'] }));
  });

  test('a kid cannot skip approval or edit the chore', async () => {
    const db = kidDevice('kid1');
    await assertFails(updateDoc(ref(db), { status: 'DONE', completedBy: 'member-kid1' }));
    await assertFails(updateDoc(ref(db), { title: 'Nothing' }));
    await assertFails(updateDoc(ref(db), { dueDate: '2027-01-01' }));
    await updateDoc(ref(db), { status: 'PENDING_APPROVAL', claimedBy: 'member-kid1', completedBy: 'member-kid1' });
    // Approving is a parent's job, even for the sibling.
    await assertFails(updateDoc(ref(kidDevice('kid2')), { status: 'OPEN', lastDoneBy: 'member-kid1', approvedBy: 'member-kid2' }));
  });

  test('a parent approves, which reopens the chore for its next date', async () => {
    await updateDoc(ref(kidDevice('kid1')), { status: 'PENDING_APPROVAL', claimedBy: 'member-kid1', completedBy: 'member-kid1' });
    await assertSucceeds(updateDoc(ref(parent('alice')), {
      status: 'OPEN', dueDate: '2026-10-25', claimedBy: null, completedBy: null,
      approvedBy: 'alice', lastDoneBy: 'member-kid1', lastDoneOn: '2026-10-24',
    }));
  });

  test('"let everyone know when done" is a yes/no that kids cannot flip', async () => {
    await assertSucceeds(updateDoc(ref(parent('alice')), { notifyWhenDone: false }));
    await assertFails(updateDoc(ref(parent('alice')), { notifyWhenDone: 'yes' }));
    await assertFails(updateDoc(ref(kidDevice('kid1')), { notifyWhenDone: true }));
  });

  test('anyone can report something urgent, but only parents change it afterwards', async () => {
    await assertSucceeds(setDoc(doc(kidDevice('kid1'), 'families', FAMILY, 'chores', 'leak'), {
      ...chore, createdBy: 'member-kid1', urgent: true,
    }));
    await assertFails(setDoc(doc(parent('alice'), 'families', FAMILY, 'chores', 'bad'), { ...chore, urgent: 'very' }));
    await assertFails(updateDoc(ref(kidDevice('kid1')), { urgent: true }));
    await assertSucceeds(updateDoc(ref(parent('alice')), { urgent: true }));
  });

  test('points: parents set what a chore is worth; kids report at the usual worth', async () => {
    await assertSucceeds(updateDoc(ref(parent('alice')), { points: 5 }));
    await assertFails(updateDoc(ref(parent('alice')), { points: 9 }));
    await assertFails(updateDoc(ref(parent('alice')), { points: 'lots' }));
    await assertFails(updateDoc(ref(kidDevice('kid1')), { points: 3 }));
    const kid = kidDevice('kid1');
    await assertSucceeds(setDoc(doc(kid, 'families', FAMILY, 'chores', 'k1'), { ...chore, createdBy: 'member-kid1', points: 1 }));
    await assertFails(setDoc(doc(kid, 'families', FAMILY, 'chores', 'k2'), { ...chore, createdBy: 'member-kid1', points: 5 }));
    await assertSucceeds(updateDoc(ref(parent('alice')), { doneCount: 2 }));
  });

  test('saved chores keep their points; the saver or a parent changes them', async () => {
    const saved = doc(kidDevice('kid1'), 'families', FAMILY, 'choreTemplates', 'garage');
    await assertSucceeds(setDoc(saved, { title: 'Clean the garage', createdBy: 'member-kid1', points: 3 }));
    await assertFails(updateDoc(doc(kidDevice('kid2'), 'families', FAMILY, 'choreTemplates', 'garage'), { points: 5 }));
    await assertSucceeds(updateDoc(doc(parent('alice'), 'families', FAMILY, 'choreTemplates', 'garage'), { points: 5 }));
    await assertFails(updateDoc(doc(parent('alice'), 'families', FAMILY, 'choreTemplates', 'garage'), { points: 0 }));
  });

  test('outsiders cannot see chores, and malformed chores are rejected', async () => {
    await assertFails(getDocs(collection(parent('mallory'), 'families', FAMILY, 'chores')));
    await assertFails(setDoc(doc(parent('alice'), 'families', FAMILY, 'chores', 'bad'), { ...chore, status: 'FINISHED' }));
    await assertFails(setDoc(doc(parent('alice'), 'families', FAMILY, 'chores', 'bad'), { ...chore, title: '' }));
  });
});

describe('locations and places', () => {
  const here = { latitude: 41.3874, longitude: 2.1686, accuracy: 20, at: 1792900000000 };
  const loc = (db, memberId) => doc(db, 'families', FAMILY, 'locations', memberId);
  const place = (db, id) => doc(db, 'families', FAMILY, 'places', id);

  beforeEach(async () => {
    await createFamilyBatch(parent('alice'), 'alice');
    for (const kid of ['kid1', 'kid2']) {
      await setDoc(doc(kidDevice(kid), 'families', FAMILY, 'joinRequests', kid), joinRequest('KID'));
      await approveKidBatch(parent('alice'), kid, `member-${kid}`);
    }
  });

  test('everyone shares and sees locations, but only their own', async () => {
    await assertSucceeds(setDoc(loc(kidDevice('kid1'), 'member-kid1'), here));
    await assertSucceeds(setDoc(loc(parent('alice'), 'alice'), here));
    await assertSucceeds(getDocs(collection(kidDevice('kid2'), 'families', FAMILY, 'locations')));
    await assertFails(setDoc(loc(kidDevice('kid1'), 'member-kid2'), here));
    await assertFails(setDoc(loc(parent('alice'), 'member-kid1'), here));
  });

  test('stopping sharing deletes your location; others cannot delete it, except a parent', async () => {
    await setDoc(loc(kidDevice('kid1'), 'member-kid1'), here);
    await assertFails(deleteDoc(loc(kidDevice('kid2'), 'member-kid1')));
    await assertSucceeds(deleteDoc(loc(kidDevice('kid1'), 'member-kid1')));
    await setDoc(loc(kidDevice('kid1'), 'member-kid1'), here);
    await assertSucceeds(deleteDoc(loc(parent('alice'), 'member-kid1')));
  });

  test('outsiders see nothing, and bad coordinates are rejected', async () => {
    await setDoc(loc(parent('alice'), 'alice'), here);
    await assertFails(getDocs(collection(parent('mallory'), 'families', FAMILY, 'locations')));
    await assertFails(setDoc(loc(parent('alice'), 'alice'), { ...here, latitude: 123 }));
    await assertFails(setDoc(loc(parent('alice'), 'alice'), { ...here, at: 'now' }));
  });

  test('arrival events: written about yourself, read by everyone, cleaned up by the author', async () => {
    const event = { memberId: 'member-kid1', placeId: 'school', placeName: 'School', type: 'ARRIVED', at: 1792900000000 };
    const events = (db) => collection(db, 'families', FAMILY, 'placeEvents');
    await assertSucceeds(setDoc(doc(events(kidDevice('kid1')), 'e1'), event));
    await assertFails(setDoc(doc(events(kidDevice('kid2')), 'e2'), event));
    await assertFails(setDoc(doc(events(kidDevice('kid1')), 'e3'), { ...event, type: 'TELEPORTED' }));
    await assertSucceeds(getDocs(events(parent('alice'))));
    await assertFails(getDocs(events(parent('mallory'))));
    await assertFails(deleteDoc(doc(events(kidDevice('kid2')), 'e1')));
    await assertSucceeds(deleteDoc(doc(events(kidDevice('kid1')), 'e1')));
  });

  test('each phone registers its own push address, which the family can read', async () => {
    const endpoint = (db, uid) => doc(db, 'families', FAMILY, 'pushEndpoints', uid);
    const mine = { memberId: 'member-kid1', url: 'https://ntfy.sh/upAbC123?up=1' };
    await assertSucceeds(setDoc(endpoint(kidDevice('kid1'), 'kid1'), mine));
    await assertFails(setDoc(endpoint(kidDevice('kid2'), 'kid1'), { ...mine, memberId: 'member-kid2' }));
    await assertFails(setDoc(endpoint(kidDevice('kid1'), 'kid1'), { ...mine, memberId: 'alice' }));
    await assertFails(setDoc(endpoint(kidDevice('kid1'), 'kid1'), { ...mine, url: 'http://insecure.example' }));
    await assertSucceeds(getDocs(collection(kidDevice('kid2'), 'families', FAMILY, 'pushEndpoints')));
    await assertFails(getDocs(collection(parent('mallory'), 'families', FAMILY, 'pushEndpoints')));
    await assertSucceeds(deleteDoc(endpoint(parent('alice'), 'kid1')));
  });

  test('someone waiting to join can read where to ping, strangers cannot', async () => {
    await setDoc(doc(parent('alice'), 'families', FAMILY, 'pushEndpoints', 'alice'), { memberId: 'alice', url: 'https://ntfy.sh/upA?up=1' });
    await setDoc(doc(parent('bob'), 'families', FAMILY, 'joinRequests', 'bob'), joinRequest('PARENT', 'Bob'));
    await assertSucceeds(getDocs(collection(parent('bob'), 'families', FAMILY, 'pushEndpoints')));
    await assertFails(getDocs(collection(parent('mallory'), 'families', FAMILY, 'pushEndpoints')));
  });

  test('parents manage places, everyone can see them', async () => {
    const home = { name: 'Home', latitude: 41.3874, longitude: 2.1686, radius: 100 };
    await assertSucceeds(setDoc(place(parent('alice'), 'home'), home));
    await assertSucceeds(getDocs(collection(kidDevice('kid1'), 'families', FAMILY, 'places')));
    await assertFails(setDoc(place(kidDevice('kid1'), 'school'), { ...home, name: 'School' }));
    await assertFails(deleteDoc(place(kidDevice('kid1'), 'home')));
    await assertFails(setDoc(place(parent('alice'), 'huge'), { ...home, radius: 50000 }));
  });
});

describe('leaving', () => {
  beforeEach(async () => {
    await createFamilyBatch(parent('alice'), 'alice');
    await setDoc(doc(kidDevice('kid1'), 'families', FAMILY, 'joinRequests', 'kid1'), joinRequest('KID'));
    await approveKidBatch(parent('alice'), 'kid1');
  });

  test('a device takes only itself off a shared kid profile, then loses access', async () => {
    const db = parent('alice');
    const batch = writeBatch(db);
    batch.update(doc(db, 'families', FAMILY, 'members', 'kid-leo'), { uids: arrayUnion('kid2') });
    batch.set(doc(db, 'families', FAMILY, 'memberUids', 'kid2'), { memberId: 'kid-leo', role: 'KID' });
    await batch.commit();

    const kid = kidDevice('kid1');
    await assertFails(updateDoc(doc(kid, 'families', FAMILY, 'members', 'kid-leo'), { uids: [] }));
    await assertFails(updateDoc(doc(kid, 'families', FAMILY, 'members', 'kid-leo'), { uids: ['kid1'], name: 'Boss' }));
    const leave = writeBatch(kid);
    leave.update(doc(kid, 'families', FAMILY, 'members', 'kid-leo'), { uids: ['kid2'] });
    leave.delete(doc(kid, 'families', FAMILY, 'memberUids', 'kid1'));
    await assertSucceeds(leave.commit());
    await assertFails(getDocs(collection(kid, 'families', FAMILY, 'members')));
  });

  test('nobody can remove someone else unless they are a parent', async () => {
    await assertFails(deleteDoc(doc(kidDevice('kid1'), 'families', FAMILY, 'memberUids', 'alice')));
    await assertFails(deleteDoc(doc(kidDevice('kid1'), 'families', FAMILY, 'members', 'alice')));
  });

  test('a parent can delete the family and its invite code', async () => {
    const db = parent('alice');
    await assertFails(deleteDoc(doc(kidDevice('kid1'), 'families', FAMILY)));
    await assertSucceeds(deleteDoc(doc(db, 'inviteCodes', CODE)));
    await assertSucceeds(deleteDoc(doc(db, 'families', FAMILY)));
  });
});

describe('user pointers', () => {
  test('users can only write their own pointer', async () => {
    await assertSucceeds(setDoc(doc(parent('bob'), 'users', 'bob'), { familyId: FAMILY }));
    await assertFails(setDoc(doc(parent('bob'), 'users', 'alice'), { familyId: 'x' }));
    await assertFails(getDoc(doc(parent('bob'), 'users', 'alice')));
  });
});

describe('family news, notes, lists, chore log and away', () => {
  const fam = (db, ...path) => doc(db, 'families', FAMILY, ...path);

  beforeEach(async () => {
    await createFamilyBatch(parent('alice'), 'alice');
    for (const kid of ['kid1', 'kid2']) {
      await setDoc(doc(kidDevice(kid), 'families', FAMILY, 'joinRequests', kid), joinRequest('KID'));
      await approveKidBatch(parent('alice'), kid, `member-${kid}`);
    }
  });

  test('"at the shop" and "on my way": only in your own name, one per kind', async () => {
    const kid = kidDevice('kid1');
    await assertSucceeds(setDoc(fam(kid, 'announcements', 'member-kid1_ON_MY_WAY'), { memberId: 'member-kid1', kind: 'ON_MY_WAY', at: 1 }));
    await assertFails(setDoc(fam(kid, 'announcements', 'member-kid2_ON_MY_WAY'), { memberId: 'member-kid2', kind: 'ON_MY_WAY', at: 1 }));
    await assertFails(setDoc(fam(kid, 'announcements', 'random'), { memberId: 'member-kid1', kind: 'ON_MY_WAY', at: 1 }));
    await assertFails(setDoc(fam(kid, 'announcements', 'member-kid1_PARTY'), { memberId: 'member-kid1', kind: 'PARTY', at: 1 }));
    await assertSucceeds(getDocs(collection(kidDevice('kid2'), 'families', FAMILY, 'announcements')));
    await assertFails(deleteDoc(fam(kidDevice('kid2'), 'announcements', 'member-kid1_ON_MY_WAY')));
    await assertSucceeds(deleteDoc(fam(kid, 'announcements', 'member-kid1_ON_MY_WAY')));
    await assertFails(getDocs(collection(parent('mallory'), 'families', FAMILY, 'announcements')));
  });

  test('messages: anyone sends one in their own name; nobody edits them', async () => {
    const msg = { text: "Dinner's ready!", from: 'member-kid1', at: 1 };
    await assertSucceeds(setDoc(fam(kidDevice('kid1'), 'messages', 'm1'), msg));
    await assertFails(setDoc(fam(kidDevice('kid1'), 'messages', 'm2'), { ...msg, from: 'alice' }));
    await assertFails(setDoc(fam(kidDevice('kid1'), 'messages', 'm3'), { ...msg, text: 'x'.repeat(201) }));
    await assertSucceeds(getDocs(collection(kidDevice('kid2'), 'families', FAMILY, 'messages')));
    await assertFails(updateDoc(fam(kidDevice('kid1'), 'messages', 'm1'), { text: 'changed' }));
    await assertFails(deleteDoc(fam(kidDevice('kid2'), 'messages', 'm1')));
    await assertSucceeds(deleteDoc(fam(kidDevice('kid1'), 'messages', 'm1')));
    await assertFails(getDocs(collection(parent('mallory'), 'families', FAMILY, 'messages')));
  });

  test('notes: anyone writes; the writer or a parent edits and removes', async () => {
    const note = { text: 'Wi-Fi: hunter2', createdBy: 'member-kid1', updatedAt: 1 };
    await assertSucceeds(setDoc(fam(kidDevice('kid1'), 'notes', 'wifi'), note));
    await assertFails(setDoc(fam(kidDevice('kid1'), 'notes', 'fake'), { ...note, createdBy: 'alice' }));
    await assertFails(setDoc(fam(kidDevice('kid1'), 'notes', 'long'), { ...note, text: 'x'.repeat(301) }));
    await assertFails(updateDoc(fam(kidDevice('kid2'), 'notes', 'wifi'), { text: 'mine now', updatedAt: 2 }));
    await assertFails(deleteDoc(fam(kidDevice('kid2'), 'notes', 'wifi')));
    await assertSucceeds(updateDoc(fam(kidDevice('kid1'), 'notes', 'wifi'), { text: 'Wi-Fi: hunter3', updatedAt: 2 }));
    await assertFails(updateDoc(fam(parent('alice'), 'notes', 'wifi'), { createdBy: 'alice' }));
    await assertSucceeds(deleteDoc(fam(parent('alice'), 'notes', 'wifi')));
  });

  test('lists: anyone makes one; items can be on it', async () => {
    await assertSucceeds(setDoc(fam(kidDevice('kid1'), 'lists', 'pharmacy'), { name: 'Pharmacy', createdBy: 'member-kid1' }));
    await assertFails(setDoc(fam(kidDevice('kid1'), 'lists', 'x'), { name: '', createdBy: 'member-kid1' }));
    const item = { name: 'Toothpaste', category: 'PERSONAL_CARE', isNeeded: true, isStaple: false, listId: 'pharmacy' };
    await assertSucceeds(setDoc(fam(kidDevice('kid2'), 'groceryItems', 'pharmacy--toothpaste'), item));
    await assertFails(setDoc(fam(kidDevice('kid2'), 'groceryItems', 'bad'), { ...item, listId: 7 }));
    await assertFails(deleteDoc(fam(kidDevice('kid2'), 'lists', 'pharmacy')));
    await assertSucceeds(deleteDoc(fam(kidDevice('kid1'), 'lists', 'pharmacy')));
  });

  test('the chore log is written by parents, read by everyone', async () => {
    const entry = { choreId: 'dishes', title: 'Dishes', doneBy: 'member-kid1', on: '2026-10-26', at: 1 };
    await assertFails(setDoc(fam(kidDevice('kid1'), 'choreLog', 'e1'), entry));
    await assertSucceeds(setDoc(fam(parent('alice'), 'choreLog', 'e1'), entry));
    await assertSucceeds(getDocs(collection(kidDevice('kid1'), 'families', FAMILY, 'choreLog')));
    await assertFails(setDoc(fam(parent('alice'), 'choreLog', 'e2'), { ...entry, on: 'Monday' }));
  });

  test('away: you mark yourself, parents mark anyone, kids not each other', async () => {
    await assertSucceeds(updateDoc(fam(kidDevice('kid1'), 'members', 'member-kid1'), { awayUntil: '2026-11-02' }));
    await assertSucceeds(updateDoc(fam(kidDevice('kid1'), 'members', 'member-kid1'), { awayUntil: null }));
    await assertFails(updateDoc(fam(kidDevice('kid1'), 'members', 'member-kid1'), { awayUntil: 'soon' }));
    await assertFails(updateDoc(fam(kidDevice('kid1'), 'members', 'member-kid1'), { awayUntil: '2026-11-02', role: 'PARENT' }));
    await assertFails(updateDoc(fam(kidDevice('kid1'), 'members', 'member-kid2'), { awayUntil: '2026-11-02' }));
    await assertSucceeds(updateDoc(fam(parent('alice'), 'members', 'member-kid2'), { awayUntil: '2026-11-02' }));
  });
});
