import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import api from '../api/client';
import { deletePendingSurvey, getPendingSurveys, updatePendingSurvey } from '../utils/offlineStore';
import { labelize } from '../utils/surveyConfig';

export default function OfflineQueue() {
  const location = useLocation();
  const [records, setRecords] = useState([]);
  const [message, setMessage] = useState(location.state?.message || '');
  const [error, setError] = useState('');
  const [syncing, setSyncing] = useState(false);

  useEffect(() => {
    load();
    function onOnline() {
      syncAll();
    }
    window.addEventListener('online', onOnline);
    return () => window.removeEventListener('online', onOnline);
  }, []);

  async function load() {
    setRecords(await getPendingSurveys());
  }

  async function syncAll() {
    if (!navigator.onLine) {
      setError('You are offline. Connect to the internet or local server, then sync again.');
      return;
    }

    setSyncing(true);
    setError('');
    setMessage('');
    let synced = 0;
    let failed = 0;

    const pending = await getPendingSurveys();
    for (const record of pending) {
      try {
        await updatePendingSurvey(record.id, { status: 'SYNCING', error: '' });
        await api.post('/surveys', record.payload);
        await deletePendingSurvey(record.id);
        synced += 1;
      } catch (err) {
        failed += 1;
        await updatePendingSurvey(record.id, {
          status: 'FAILED',
          error: err.response?.data?.message || 'Sync failed. Check connection and required fields.'
        });
      }
    }

    setSyncing(false);
    await load();
    if (synced > 0) setMessage(`${synced} survey${synced === 1 ? '' : 's'} synced successfully.`);
    if (failed > 0) setError(`${failed} survey${failed === 1 ? '' : 's'} could not sync.`);
  }

  async function remove(id) {
    if (!confirm('Remove this pending offline survey?')) return;
    await deletePendingSurvey(id);
    await load();
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">Offline submissions</p>
          <h1>Pending Sync</h1>
        </div>
        <button onClick={syncAll} disabled={syncing || records.length === 0}>
          {syncing ? 'Syncing...' : 'Sync now'}
        </button>
      </header>

      {message && <div className="alert success">{message}</div>}
      {error && <div className="alert error">{error}</div>}

      <section className="table-card">
        {records.length === 0 ? (
          <p className="empty-state">No pending offline surveys.</p>
        ) : (
          <table className="offline-table">
            <thead>
              <tr>
                <th>Saved</th>
                <th>Surveyor</th>
                <th>Study Area</th>
                <th>Age/Gender</th>
                <th>Status</th>
                <th>Error</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {records.map((record) => (
                <tr key={record.id}>
                  <td>{new Date(record.createdAt).toLocaleString()}</td>
                  <td>{record.username}</td>
                  <td>{labelize(record.payload.studyArea || '')}</td>
                  <td>{record.payload.age || '-'} / {labelize(record.payload.gender || '') || '-'}</td>
                  <td><span className={`sync-badge ${record.status.toLowerCase()}`}>{labelize(record.status)}</span></td>
                  <td>{record.error || '-'}</td>
                  <td className="row-actions">
                    {record.status !== 'SYNCING' && (
                      <Link className="action-button edit-action" to={`/offline-queue/${record.id}/edit`}>Edit</Link>
                    )}
                    <button onClick={() => remove(record.id)}>Remove</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
