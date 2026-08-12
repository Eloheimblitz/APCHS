import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../api/client';
import SurveyForm from '../components/SurveyForm';
import { addPendingSurvey, isNetworkError } from '../utils/offlineStore';
import { getSession } from '../api/client';

export default function SurveyPage({ mode }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const [record, setRecord] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const session = getSession();

  useEffect(() => {
    if (mode === 'edit') {
      api.get(`/surveys/${id}`).then(({ data }) => setRecord(data)).catch(() => setError('Unable to load survey record.'));
    }
  }, [id, mode]);

  async function save(values) {
    setLoading(true);
    setError('');
    setFieldErrors({});

    if (mode === 'create' && !navigator.onLine) {
      await addPendingSurvey(values, getSession()?.username || 'unknown');
      navigate('/offline-queue', { state: { message: 'Survey saved offline and marked for sync.' } });
      setLoading(false);
      return;
    }

    try {
      const { data } = mode === 'edit'
        ? await api.put(`/surveys/${id}`, values)
        : await api.post('/surveys', values);
      navigate(`/surveys/${data.id}`);
    } catch (err) {
      if (mode === 'create' && isNetworkError(err)) {
        await addPendingSurvey(values, getSession()?.username || 'unknown');
        navigate('/offline-queue', { state: { message: 'Network unavailable. Survey saved offline and marked for sync.' } });
        return;
      }
      setError(err.response?.data?.message || 'Unable to save survey. Check required fields.');
      setFieldErrors(err.response?.data?.fields || {});
    } finally {
      setLoading(false);
    }
  }

  if (mode === 'edit' && !record && !error) return <div className="page"><p>Loading survey...</p></div>;

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">{mode === 'edit' ? 'Edit record' : 'New household survey'}</p>
          <h1>{mode === 'edit' ? record?.surveyId : 'Add Survey'}</h1>
        </div>
      </header>
      {error && (
        <div className="alert error">
          <strong>{error}</strong>
          {Object.keys(fieldErrors).length > 0 && (
            <ul>
              {Object.entries(fieldErrors).slice(0, 8).map(([field, message]) => (
                <li key={field}>{message}</li>
              ))}
            </ul>
          )}
        </div>
      )}
      <SurveyForm initialValues={mode === 'edit' ? record || undefined : { surveyorId: session?.username || '' }} lockedFields={mode === 'create' ? ['surveyorId'] : []} onSubmit={save} loading={loading} submitLabel={mode === 'edit' ? 'Update survey' : 'Submit survey'} unlockAllSections={mode === 'edit'} />
    </div>
  );
}
