const DB_NAME = 'apchs_offline';
const DB_VERSION = 1;
const STORE_NAME = 'pending_surveys';

function openDb() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);

    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        const store = db.createObjectStore(STORE_NAME, { keyPath: 'id', autoIncrement: true });
        store.createIndex('status', 'status');
        store.createIndex('createdAt', 'createdAt');
      }
    };

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function withStore(mode, callback) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(STORE_NAME, mode);
    const store = transaction.objectStore(STORE_NAME);
    const result = callback(store);

    transaction.oncomplete = () => {
      db.close();
      resolve(result);
    };
    transaction.onerror = () => {
      db.close();
      reject(transaction.error);
    };
  });
}

export async function addPendingSurvey(payload, username) {
  const record = {
    payload,
    username,
    status: 'PENDING',
    error: '',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };

  return withStore('readwrite', (store) => store.add(record));
}

export async function getPendingSurveys() {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(STORE_NAME, 'readonly');
    const store = transaction.objectStore(STORE_NAME);
    const request = store.getAll();

    request.onsuccess = () => {
      db.close();
      resolve(request.result.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)));
    };
    request.onerror = () => {
      db.close();
      reject(request.error);
    };
  });
}

export async function getPendingSurvey(id) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(STORE_NAME, 'readonly');
    const store = transaction.objectStore(STORE_NAME);
    const request = store.get(id);

    request.onsuccess = () => {
      db.close();
      resolve(request.result || null);
    };
    request.onerror = () => {
      db.close();
      reject(request.error);
    };
  });
}

export async function updatePendingSurvey(id, changes) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const transaction = db.transaction(STORE_NAME, 'readwrite');
    const store = transaction.objectStore(STORE_NAME);
    const getRequest = store.get(id);

    getRequest.onsuccess = () => {
      const existing = getRequest.result;
      if (!existing) {
        resolve();
        return;
      }
      store.put({ ...existing, ...changes, updatedAt: new Date().toISOString() });
    };

    transaction.oncomplete = () => {
      db.close();
      resolve();
    };
    transaction.onerror = () => {
      db.close();
      reject(transaction.error);
    };
  });
}

export async function deletePendingSurvey(id) {
  return withStore('readwrite', (store) => store.delete(id));
}

export async function countPendingSurveys() {
  const records = await getPendingSurveys();
  return records.filter((record) => record.status !== 'SYNCED').length;
}

export function isNetworkError(error) {
  return !error.response || error.code === 'ERR_NETWORK' || error.message === 'Network Error';
}
