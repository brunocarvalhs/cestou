// Self-service account deletion for Cestou (Google Play "delete account" web link).
// Mirrors feature/settings DeleteAccountUseCase: Firestore cleanup first (rules need an
// authenticated request), then user.delete(). Success is shown only after user.delete() resolves.
import { initializeApp } from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-app.js';
import {
  initializeAuth, inMemoryPersistence, browserPopupRedirectResolver, connectAuthEmulator,
  GoogleAuthProvider, EmailAuthProvider, signInWithPopup, signInWithEmailAndPassword,
  reauthenticateWithPopup, reauthenticateWithCredential, signOut, deleteUser
} from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js';
import {
  getFirestore, connectFirestoreEmulator, collection, doc, query, where, getDocs,
  deleteDoc, updateDoc, arrayRemove, deleteField
} from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-firestore.js';

// Public web config (the API key is not a secret; access is enforced by Auth + Firestore rules).
// Values come from app/google-services.json; there is no registered Firebase web app yet.
const app = initializeApp({
  apiKey: 'AIzaSyAq-I9D9s3k_o0VuwlRAkavRjPhcMa8_24',
  authDomain: 'how-much-2a72e.firebaseapp.com',
  projectId: 'how-much-2a72e'
});
// In-memory persistence: every visit starts with a fresh sign-in, so the recent-login
// requirement of user.delete() normally holds.
const auth = initializeAuth(app, {
  persistence: inMemoryPersistence,
  popupRedirectResolver: browserPopupRedirectResolver
});
const db = getFirestore(app);
if (['localhost', '127.0.0.1'].includes(location.hostname)) {
  connectAuthEmulator(auth, 'http://127.0.0.1:9099', { disableWarnings: true });
  connectFirestoreEmulator(db, '127.0.0.1', 8080);
}

const RECENT_LOGIN_WINDOW_MS = 4 * 60 * 1000; // same as FirebaseAuthService.RECENT_LOGIN_WINDOW_MS
let authAt = 0;
let lang = 'pt';

const T = {
  pt: {
    title: 'Excluir pela web',
    signinTitle: '1. Entre na sua conta',
    signinIntro: 'Use a mesma forma de login que você usa no app.',
    google: 'Entrar com Google',
    or: 'ou',
    email: 'E-mail',
    password: 'Senha',
    signinEmail: 'Entrar com e-mail',
    confirmTitle: '2. Confirme a exclusão',
    signedInAs: 'Conectado como',
    confirmWarning: 'Isto apaga sua conta, seu perfil, suas listas individuais e remove você das listas compartilhadas. Não é possível desfazer.',
    typeEmail: 'Digite seu e-mail para confirmar',
    typeEmailHint: 'Precisa ser exatamente o e-mail mostrado acima.',
    checkbox: 'Entendo que a exclusão é permanente.',
    deleteBtn: 'Excluir minha conta definitivamente',
    cancel: 'Cancelar e sair',
    reauthTitle: 'Entre novamente para continuar',
    reauthIntro: 'Por segurança, a exclusão exige um login recente. Entre de novo com a mesma conta.',
    reauthBtn: 'Entrar novamente',
    progressTitle: 'Excluindo…',
    progressHint: 'Não feche esta página.',
    stepLists: 'Removendo suas listas de compras…',
    stepProfile: 'Removendo seu perfil…',
    stepAccount: 'Excluindo sua conta de login…',
    doneTitle: 'Conta excluída',
    doneText: 'Sua conta e seus dados foram excluídos. Você pode fechar esta página.',
    signingIn: 'Entrando…',
    errFields: 'Preencha e-mail e senha.',
    errCreds: 'E-mail ou senha incorretos.',
    errPopup: 'O login foi cancelado.',
    errNetwork: 'Sem conexão. Verifique sua internet e tente novamente.',
    errMany: 'Muitas tentativas. Aguarde alguns minutos e tente novamente.',
    errMismatch: 'Entre com a mesma conta que você está excluindo.',
    errDenied: 'O servidor recusou uma das operações. Sua conta NÃO foi excluída. Tente novamente ou exclua pelo app.',
    errGeneric: 'Algo deu errado. Sua conta NÃO foi excluída. Tente novamente.'
  },
  en: {
    title: 'Delete on the web',
    signinTitle: '1. Sign in to your account',
    signinIntro: 'Use the same sign-in method you use in the app.',
    google: 'Sign in with Google',
    or: 'or',
    email: 'Email',
    password: 'Password',
    signinEmail: 'Sign in with email',
    confirmTitle: '2. Confirm deletion',
    signedInAs: 'Signed in as',
    confirmWarning: 'This deletes your account, your profile and your personal lists, and removes you from shared lists. It cannot be undone.',
    typeEmail: 'Type your email to confirm',
    typeEmailHint: 'It must match the email shown above exactly.',
    checkbox: 'I understand the deletion is permanent.',
    deleteBtn: 'Permanently delete my account',
    cancel: 'Cancel and sign out',
    reauthTitle: 'Sign in again to continue',
    reauthIntro: 'For your security, deletion requires a recent sign-in. Sign in again with the same account.',
    reauthBtn: 'Sign in again',
    progressTitle: 'Deleting…',
    progressHint: 'Do not close this page.',
    stepLists: 'Removing your shopping lists…',
    stepProfile: 'Removing your profile…',
    stepAccount: 'Deleting your sign-in account…',
    doneTitle: 'Account deleted',
    doneText: 'Your account and data have been deleted. You can close this page.',
    signingIn: 'Signing in…',
    errFields: 'Enter your email and password.',
    errCreds: 'Incorrect email or password.',
    errPopup: 'Sign-in was cancelled.',
    errNetwork: 'No connection. Check your internet and try again.',
    errMany: 'Too many attempts. Wait a few minutes and try again.',
    errMismatch: 'Sign in with the same account you are deleting.',
    errDenied: 'The server refused one of the operations. Your account was NOT deleted. Try again or delete it in the app.',
    errGeneric: 'Something went wrong. Your account was NOT deleted. Please try again.'
  },
  es: {
    title: 'Eliminar en la web',
    signinTitle: '1. Inicia sesión en tu cuenta',
    signinIntro: 'Usa el mismo método de inicio de sesión que usas en la app.',
    google: 'Iniciar sesión con Google',
    or: 'o',
    email: 'Correo electrónico',
    password: 'Contraseña',
    signinEmail: 'Iniciar sesión con correo',
    confirmTitle: '2. Confirma la eliminación',
    signedInAs: 'Sesión iniciada como',
    confirmWarning: 'Esto elimina tu cuenta, tu perfil y tus listas personales, y te quita de las listas compartidas. No se puede deshacer.',
    typeEmail: 'Escribe tu correo para confirmar',
    typeEmailHint: 'Debe coincidir exactamente con el correo mostrado arriba.',
    checkbox: 'Entiendo que la eliminación es permanente.',
    deleteBtn: 'Eliminar mi cuenta definitivamente',
    cancel: 'Cancelar y cerrar sesión',
    reauthTitle: 'Inicia sesión de nuevo para continuar',
    reauthIntro: 'Por seguridad, la eliminación requiere un inicio de sesión reciente. Vuelve a iniciar sesión con la misma cuenta.',
    reauthBtn: 'Iniciar sesión de nuevo',
    progressTitle: 'Eliminando…',
    progressHint: 'No cierres esta página.',
    stepLists: 'Eliminando tus listas de compras…',
    stepProfile: 'Eliminando tu perfil…',
    stepAccount: 'Eliminando tu cuenta de inicio de sesión…',
    doneTitle: 'Cuenta eliminada',
    doneText: 'Tu cuenta y tus datos fueron eliminados. Puedes cerrar esta página.',
    signingIn: 'Iniciando sesión…',
    errFields: 'Ingresa tu correo y contraseña.',
    errCreds: 'Correo o contraseña incorrectos.',
    errPopup: 'Se canceló el inicio de sesión.',
    errNetwork: 'Sin conexión. Revisa tu internet e inténtalo de nuevo.',
    errMany: 'Demasiados intentos. Espera unos minutos e inténtalo de nuevo.',
    errMismatch: 'Inicia sesión con la misma cuenta que estás eliminando.',
    errDenied: 'El servidor rechazó una de las operaciones. Tu cuenta NO fue eliminada. Inténtalo de nuevo o elimínala en la app.',
    errGeneric: 'Algo salió mal. Tu cuenta NO fue eliminada. Inténtalo de nuevo.'
  }
};

const $ = (id) => document.getElementById(id);
const t = (key) => T[lang][key];

// ---------------------------------------------------------------- language switch
function setLang(next) {
  lang = T[next] ? next : 'pt';
  document.documentElement.lang = { pt: 'pt-BR', en: 'en', es: 'es' }[lang];
  document.querySelectorAll('section[data-lang]').forEach((s) => { s.hidden = s.dataset.lang !== lang; });
  document.querySelectorAll('.lang-switch a').forEach((a) => {
    a.setAttribute('aria-current', String(a.dataset.lang === lang));
  });
  document.querySelectorAll('[data-i18n]').forEach((el) => { el.textContent = t(el.dataset.i18n); });
}
function langFromHash() {
  const h = location.hash.slice(1);
  return T[h] ? h : null;
}
window.addEventListener('hashchange', () => { const l = langFromHash(); if (l) setLang(l); });
const nav = (navigator.language || 'pt').slice(0, 2);
setLang(langFromHash() || (T[nav] ? nav : 'pt'));

// ---------------------------------------------------------------- UI state
function show(step) {
  document.querySelectorAll('[data-step]').forEach((el) => { el.hidden = el.id !== `step-${step}`; });
  $(`step-${step}`).querySelector('h3').focus();
}
function setError(key, code) {
  $('error').textContent = key ? t(key) + (code ? ` (${code})` : '') : '';
}
function setStatus(text) { $('status').textContent = text || ''; }

function errorKey(code) {
  switch (code) {
    case 'auth/invalid-credential':
    case 'auth/invalid-login-credentials':
    case 'auth/wrong-password':
    case 'auth/user-not-found':
    case 'auth/invalid-email': return 'errCreds';
    case 'auth/popup-closed-by-user':
    case 'auth/cancelled-popup-request': return 'errPopup';
    case 'auth/network-request-failed':
    case 'unavailable': return 'errNetwork';
    case 'auth/too-many-requests': return 'errMany';
    case 'auth/user-mismatch': return 'errMismatch';
    case 'permission-denied': return 'errDenied';
    default: return 'errGeneric';
  }
}

function providerOf(user) {
  const ids = user.providerData.map((p) => p.providerId);
  return ids.includes('google.com') && !ids.includes('password') ? 'google.com'
    : ids.includes('password') ? 'password' : ids[0];
}

function onSignedIn(user) {
  authAt = Date.now();
  setStatus('');
  setError(null);
  $('who').textContent = user.email || user.uid;
  $('confirm-email').value = '';
  $('confirm-check').checked = false;
  updateDeleteButton();
  show('confirm');
}

// ---------------------------------------------------------------- sign-in
$('google-btn').addEventListener('click', async () => {
  setError(null);
  setStatus(t('signingIn'));
  try {
    const { user } = await signInWithPopup(auth, new GoogleAuthProvider());
    onSignedIn(user);
  } catch (e) {
    setStatus('');
    setError(errorKey(e.code), e.code);
  }
});

$('email-form').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const email = $('email').value.trim();
  const password = $('password').value;
  if (!email || !password) return setError('errFields');
  setError(null);
  setStatus(t('signingIn'));
  try {
    const { user } = await signInWithEmailAndPassword(auth, email, password);
    $('password').value = '';
    onSignedIn(user);
  } catch (e) {
    setStatus('');
    setError(errorKey(e.code), e.code);
  }
});

document.querySelectorAll('[data-action="signout"]').forEach((b) => b.addEventListener('click', async () => {
  await signOut(auth).catch(() => {});
  setError(null);
  setStatus('');
  show('signin');
}));

// ---------------------------------------------------------------- confirmation (double)
function updateDeleteButton() {
  const user = auth.currentUser;
  const typed = $('confirm-email').value.trim().toLowerCase();
  const expected = (user?.email || user?.uid || '').toLowerCase();
  $('delete-btn').disabled = !(user && typed === expected && $('confirm-check').checked);
}
$('confirm-email').addEventListener('input', updateDeleteButton);
$('confirm-check').addEventListener('change', updateDeleteButton);

$('confirm-form').addEventListener('submit', (ev) => {
  ev.preventDefault();
  updateDeleteButton();
  if ($('delete-btn').disabled) return;
  // Same guard as DeleteAccountUseCase: require a fresh sign-in BEFORE any destructive step,
  // otherwise user.delete() could fail after the data is already gone.
  if (Date.now() - authAt > RECENT_LOGIN_WINDOW_MS) return askReauth();
  runDeletion();
});

// ---------------------------------------------------------------- re-authentication
function askReauth() {
  setStatus('');
  $('reauth-password').hidden = providerOf(auth.currentUser) !== 'password';
  $('reauth-pass').value = '';
  show('reauth');
}

$('reauth-form').addEventListener('submit', async (ev) => {
  ev.preventDefault();
  const user = auth.currentUser;
  setError(null);
  setStatus(t('signingIn'));
  try {
    if (providerOf(user) === 'password') {
      const pass = $('reauth-pass').value;
      if (!pass) { setStatus(''); return setError('errFields'); }
      await reauthenticateWithCredential(user, EmailAuthProvider.credential(user.email, pass));
    } else {
      await reauthenticateWithPopup(user, new GoogleAuthProvider());
    }
    authAt = Date.now();
    runDeletion();
  } catch (e) {
    setStatus('');
    setError(errorKey(e.code), e.code);
  }
});

// ---------------------------------------------------------------- deletion
// Firestore cleanup, mirroring DeleteAccountUseCase. Idempotent, so a retry after a
// re-sign-in simply finds less to delete.
async function deleteUserData(uid) {
  $('progress-text').textContent = t('stepLists');
  // Same query as ShoppingRepositoryImpl.getAll(): whereArrayContains("users", uid).
  const lists = await getDocs(query(collection(db, 'shopping'), where('users', 'array-contains', uid)));
  for (const list of lists.docs) {
    const users = list.data().users || [];
    if (users.length <= 1) {
      // Sole member: delete the list with its items (items first, while membership still
      // grants access to the products subcollection under the rules).
      const items = await getDocs(collection(list.ref, 'products'));
      for (const item of items.docs) await deleteDoc(item.ref);
      await deleteDoc(list.ref);
    } else {
      // Shared list: leave it for the others; drop this user from users and roles.
      await updateDoc(list.ref, { users: arrayRemove(uid), [`roles.${uid}`]: deleteField() });
    }
  }

  $('progress-text').textContent = t('stepProfile');
  const common = await getDocs(collection(db, 'users', uid, 'common-products'));
  for (const item of common.docs) await deleteDoc(item.ref);
  await deleteDoc(doc(db, 'users', uid));
}

async function runDeletion() {
  const user = auth.currentUser;
  if (!user) return show('signin');
  setError(null);
  setStatus('');
  show('progress');
  try {
    await deleteUserData(user.uid);
    $('progress-text').textContent = t('stepAccount');
    await deleteUser(user);
    // Only reached once user.delete() resolved.
    show('done');
  } catch (e) {
    if (e.code === 'auth/requires-recent-login') {
      askReauth();
      return;
    }
    show('confirm');
    setError(errorKey(e.code), e.code);
  }
}

$('panel').hidden = false;
