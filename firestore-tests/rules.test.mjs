// Executable review aid for ../firestore.rules — runs entirely against the local
// Firestore emulator and never touches the real project.
//
//   cd firestore-tests
//   npm install --no-save @firebase/rules-unit-testing firebase
//   firebase emulators:exec --only firestore --project demo-cestou "node rules.test.mjs"
//
// It is deliberately NOT wired into Gradle or CI: this repo has no JS toolchain
// and adding one is a separate decision. Keep it in sync when the rules change.

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} from '@firebase/rules-unit-testing';
import {
  doc, getDoc, setDoc, updateDoc, deleteDoc, collection, query, where, getDocs,
} from 'firebase/firestore';

const RULES = readFileSync(
  fileURLToPath(new URL('../firestore.rules', import.meta.url)),
  'utf8'
);

const OWNER = 'owner-uid';
const EDITOR = 'editor-uid';
const OUTSIDER = 'outsider-uid';

const testEnv = await initializeTestEnvironment({
  projectId: 'demo-cestou',
  firestore: { rules: RULES, host: '127.0.0.1', port: 8080 },
});

let pass = 0;
let fail = 0;
async function t(name, fn) {
  try {
    await fn();
    pass++;
    console.log(`  ok   ${name}`);
  } catch (e) {
    fail++;
    console.log(`  FAIL ${name}\n       ${e.message}`);
  }
}

const LIST_ID = 'list-1';
const baseList = {
  id: LIST_ID,
  title: 'Mercado',
  description: 'semana',
  price: 0,
  status: 'NEW',
  users: [OWNER, EDITOR],
  roles: { [OWNER]: 'OWNER' },
  createdAt: 1,
  isFavorite: false,
  isCategorized: true,
  shortCode: 'ABC123',
  position: 0,
  emoji: '🛒',
};

async function seed() {
  await testEnv.clearFirestore();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'shopping', LIST_ID), baseList);
    await setDoc(doc(db, 'shopping', LIST_ID, 'products', 'p1'), {
      id: 'p1', name: 'Arroz', quantity: 1, price: 10, isPurchased: false,
      category: 'Mercearia', barcode: null, unit: 'kg', total: 10, history: [],
    });
    await setDoc(doc(db, 'users', OWNER), { id: OWNER, name: 'Bruno', email: 'b@x.com', photoUrl: null });
    await setDoc(doc(db, 'users', OWNER, 'common-products', 'c1'), { id: 'c1', name: 'Arroz', category: 'Mercearia', unit: 'kg' });
    await setDoc(doc(db, 'notifications', 'n1'), {
      id: 'n1', userId: OWNER, title: 'Oi', message: 'Msg', type: 'list_joined',
      isRead: false, timestamp: 123,
    });
  });
}

const owner = () => testEnv.authenticatedContext(OWNER).firestore();
const editor = () => testEnv.authenticatedContext(EDITOR).firestore();
const outsider = () => testEnv.authenticatedContext(OUTSIDER).firestore();
const anon = () => testEnv.unauthenticatedContext().firestore();

console.log('\n== shopping ==');
await seed();
await t('member reads the list', () => assertSucceeds(getDoc(doc(editor(), 'shopping', LIST_ID))));
await t('outsider cannot read the list', () => assertFails(getDoc(doc(outsider(), 'shopping', LIST_ID))));
await t('unauthenticated cannot read the list', () => assertFails(getDoc(doc(anon(), 'shopping', LIST_ID))));
await t('member query by array-contains users works', () =>
  assertSucceeds(getDocs(query(collection(editor(), 'shopping'), where('users', 'array-contains', EDITOR)))));
await t('outsider cannot dump the collection', () =>
  assertFails(getDocs(collection(outsider(), 'shopping'))));
await t('outsider cannot query by shortCode (known regression, documented)', () =>
  assertFails(getDocs(query(collection(outsider(), 'shopping'), where('shortCode', '==', 'ABC123')))));
await t('outsider cannot query someone else array-contains', () =>
  assertFails(getDocs(query(collection(outsider(), 'shopping'), where('users', 'array-contains', OWNER)))));

await t('create own list as owner', () =>
  assertSucceeds(setDoc(doc(outsider(), 'shopping', 'new-1'),
    { ...baseList, id: 'new-1', users: [OUTSIDER], roles: { [OUTSIDER]: 'OWNER' } })));
await t('cannot create a list you are not part of', () =>
  assertFails(setDoc(doc(outsider(), 'shopping', 'new-2'),
    { ...baseList, id: 'new-2', users: [OWNER], roles: { [OWNER]: 'OWNER' } })));
await t('cannot create with mismatched id', () =>
  assertFails(setDoc(doc(outsider(), 'shopping', 'new-3'),
    { ...baseList, id: 'other', users: [OUTSIDER], roles: { [OUTSIDER]: 'OWNER' } })));
await t('cannot create with a role for a non-member', () =>
  assertFails(setDoc(doc(outsider(), 'shopping', 'new-4'),
    { ...baseList, id: 'new-4', users: [OUTSIDER], roles: { [OUTSIDER]: 'OWNER', [OWNER]: 'EDITOR' } })));

await seed();
await t('editor updates list content', () =>
  assertSucceeds(updateDoc(doc(editor(), 'shopping', LIST_ID), { title: 'Novo', price: 12.5 })));
await t('editor cannot grant itself OWNER', () =>
  assertFails(updateDoc(doc(editor(), 'shopping', LIST_ID), { roles: { [OWNER]: 'OWNER', [EDITOR]: 'OWNER' } })));
await t('editor cannot remove the owner', () =>
  assertFails(updateDoc(doc(editor(), 'shopping', LIST_ID), { users: [EDITOR] })));
await t('editor cannot change the shortCode', () =>
  assertFails(updateDoc(doc(editor(), 'shopping', LIST_ID), { shortCode: 'ZZZZZZ' })));
await t('owner can change roles', () =>
  assertSucceeds(updateDoc(doc(owner(), 'shopping', LIST_ID), { roles: { [OWNER]: 'OWNER', [EDITOR]: 'EDITOR' } })));
await t('owner cannot write itself out of the list', () =>
  assertFails(updateDoc(doc(owner(), 'shopping', LIST_ID), { users: [EDITOR] })));

await seed();
await t('outsider self-joins (users arrayUnion of own uid)', () =>
  assertSucceeds(updateDoc(doc(outsider(), 'shopping', LIST_ID), { users: [OWNER, EDITOR, OUTSIDER] })));
await seed();
await t('outsider cannot join and edit at the same time', () =>
  assertFails(updateDoc(doc(outsider(), 'shopping', LIST_ID), { users: [OWNER, EDITOR, OUTSIDER], title: 'hack' })));
await t('outsider cannot add a third party', () =>
  assertFails(updateDoc(doc(outsider(), 'shopping', LIST_ID), { users: [OWNER, EDITOR, 'someone-else'] })));
await t('outsider cannot join by replacing the members list', () =>
  assertFails(updateDoc(doc(outsider(), 'shopping', LIST_ID), { users: [OUTSIDER] })));
await t('editor cannot delete the list', () =>
  assertFails(deleteDoc(doc(editor(), 'shopping', LIST_ID))));
await t('owner deletes the list', () =>
  assertSucceeds(deleteDoc(doc(owner(), 'shopping', LIST_ID))));

console.log('\n== shopping/{id}/products ==');
await seed();
await t('member lists products', () =>
  assertSucceeds(getDocs(collection(editor(), 'shopping', LIST_ID, 'products'))));
await t('outsider cannot list products', () =>
  assertFails(getDocs(collection(outsider(), 'shopping', LIST_ID, 'products'))));
await t('member creates a product', () =>
  assertSucceeds(setDoc(doc(editor(), 'shopping', LIST_ID, 'products', 'p2'),
    { id: 'p2', name: 'Feijão', quantity: 1, price: 8, isPurchased: false, category: 'Mercearia', barcode: null, unit: 'kg', total: 8, history: [] })));
await t('product id must match the document id', () =>
  assertFails(setDoc(doc(editor(), 'shopping', LIST_ID, 'products', 'p3'), { id: 'wrong', name: 'X', quantity: 1 })));
await t('member updates a product', () =>
  assertSucceeds(updateDoc(doc(editor(), 'shopping', LIST_ID, 'products', 'p1'), { isPurchased: true })));
await t('outsider cannot write a product', () =>
  assertFails(updateDoc(doc(outsider(), 'shopping', LIST_ID, 'products', 'p1'), { isPurchased: true })));
await t('member deletes a product', () =>
  assertSucceeds(deleteDoc(doc(editor(), 'shopping', LIST_ID, 'products', 'p1'))));

console.log('\n== users ==');
await seed();
await t('any signed-in user reads a profile by uid (shared-list member names)', () =>
  assertSucceeds(getDoc(doc(editor(), 'users', OWNER))));
await t('unauthenticated cannot read a profile', () =>
  assertFails(getDoc(doc(anon(), 'users', OWNER))));
await t('the users collection cannot be enumerated', () =>
  assertFails(getDocs(collection(editor(), 'users'))));
await t('user updates own profile', () =>
  assertSucceeds(updateDoc(doc(owner(), 'users', OWNER), { name: 'Bruno C' })));
await t('user cannot update another profile', () =>
  assertFails(updateDoc(doc(editor(), 'users', OWNER), { name: 'hacked' })));
await t('user creates own profile document', () =>
  assertSucceeds(setDoc(doc(editor(), 'users', EDITOR), { id: EDITOR, name: 'Ed', email: 'e@x.com', photoUrl: null })));
await t('user cannot add unexpected fields to the profile', () =>
  assertFails(setDoc(doc(editor(), 'users', EDITOR), { id: EDITOR, name: 'Ed', isAdmin: true })));
await t('user cannot delete another profile', () =>
  assertFails(deleteDoc(doc(editor(), 'users', OWNER))));
await t('user deletes own profile (account deletion)', () =>
  assertSucceeds(deleteDoc(doc(owner(), 'users', OWNER))));

console.log('\n== users/{uid}/common-products ==');
await seed();
await t('owner reads own common products', () =>
  assertSucceeds(getDocs(collection(owner(), 'users', OWNER, 'common-products'))));
await t('other user cannot read them', () =>
  assertFails(getDocs(collection(editor(), 'users', OWNER, 'common-products'))));
await t('owner seeds a common product', () =>
  assertSucceeds(setDoc(doc(owner(), 'users', OWNER, 'common-products', 'c2'),
    { id: 'c2', name: 'Café', category: 'Mercearia', unit: 'un' })));
await t('other user cannot write them', () =>
  assertFails(setDoc(doc(editor(), 'users', OWNER, 'common-products', 'c3'),
    { id: 'c3', name: 'Café', category: 'Mercearia', unit: 'un' })));
await t('owner removes a common product', () =>
  assertSucceeds(deleteDoc(doc(owner(), 'users', OWNER, 'common-products', 'c1'))));

console.log('\n== notifications ==');
await seed();
const notif = (id, userId, over = {}) => ({
  id, userId, title: 'Alguém entrou', message: 'Fulano entrou na lista Mercado',
  type: 'list_joined', isRead: false, timestamp: 1700000000000, ...over,
});
await t('recipient reads own notifications', () =>
  assertSucceeds(getDocs(query(collection(owner(), 'notifications'), where('userId', '==', OWNER)))));
await t('other user cannot read the recipient feed', () =>
  assertFails(getDocs(query(collection(editor(), 'notifications'), where('userId', '==', OWNER)))));
await t('nobody can dump notifications', () =>
  assertFails(getDocs(collection(editor(), 'notifications'))));
await t('a third party may notify another user (join/finish flows)', () =>
  assertSucceeds(setDoc(doc(editor(), 'notifications', 'n2'), notif('n2', OWNER))));
await t('unauthenticated cannot notify', () =>
  assertFails(setDoc(doc(anon(), 'notifications', 'n3'), notif('n3', OWNER))));
await t('notification cannot be created already read', () =>
  assertFails(setDoc(doc(editor(), 'notifications', 'n4'), notif('n4', OWNER, { isRead: true }))));
await t('unknown notification type is rejected', () =>
  assertFails(setDoc(doc(editor(), 'notifications', 'n5'), notif('n5', OWNER, { type: 'phishing' }))));
await t('extra fields are rejected', () =>
  assertFails(setDoc(doc(editor(), 'notifications', 'n6'), { ...notif('n6', OWNER), payloadUrl: 'http://evil' })));
await t('oversized message is rejected', () =>
  assertFails(setDoc(doc(editor(), 'notifications', 'n7'), notif('n7', OWNER, { message: 'x'.repeat(501) }))));
await t('id must match the document id', () =>
  assertFails(setDoc(doc(editor(), 'notifications', 'n8'), notif('nope', OWNER))));
await t('recipient marks as read', () =>
  assertSucceeds(updateDoc(doc(owner(), 'notifications', 'n1'), { isRead: true })));
await t('recipient cannot rewrite the message', () =>
  assertFails(updateDoc(doc(owner(), 'notifications', 'n1'), { message: 'outra' })));
await t('sender cannot mark someone else notification as read', () =>
  assertFails(updateDoc(doc(editor(), 'notifications', 'n1'), { isRead: true })));
await t('notifications cannot be deleted', () =>
  assertFails(deleteDoc(doc(owner(), 'notifications', 'n1'))));

console.log('\n== unmapped paths ==');
await t('unknown top-level collection is denied', () =>
  assertFails(setDoc(doc(owner(), 'whatever', 'x'), { a: 1 })));
await t('the prices sub-collection (KDoc-only, never used) is denied', () =>
  assertFails(setDoc(doc(owner(), 'shopping', LIST_ID, 'products', 'p1', 'prices', 'x'), { a: 1 })));

console.log(`\n${pass} passed, ${fail} failed`);
await testEnv.cleanup();
process.exit(fail === 0 ? 0 : 1);
