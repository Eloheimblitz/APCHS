import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { downloadBlob, getSession } from '../api/client';
import { labelize, optionSets } from '../utils/surveyConfig';

const emptyFilters = {
  fromDate: '',
  toDate: '',
  studyArea: '',
  cookingFuel: ''
};

export default function Records() {
  const [filters, setFilters] = useState(emptyFilters);
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const session = getSession();
  const isAdmin = session?.role === 'ADMIN';

  useEffect(() => {
    load();
  }, []);

  async function load(event) {
    event?.preventDefault();
    setLoading(true);
    setError('');
    try {
      const { data } = await api.get('/surveys', { params: activeFilters() });
      setRecords(data);
    } catch {
      setError('Unable to load survey records.');
    } finally {
      setLoading(false);
    }
  }

  async function remove(id) {
    if (!confirm('Delete this survey record?')) return;
    await api.delete(`/surveys/${id}`);
    load();
  }

  async function exportFile(type) {
    const responseType = 'blob';
    const endpoint = type === 'csv' ? '/export/surveys.csv' : '/export/surveys.xlsx';
    const { data } = await api.get(endpoint, { params: activeFilters(), responseType });
    downloadBlob(data, `surveys.${type}`, type === 'csv' ? 'text/csv' : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
  }

  function activeFilters() {
    return Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== ''));
  }

  function setFilter(name, value) {
    setFilters((current) => ({ ...current, [name]: value }));
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">{isAdmin ? 'Submitted household records' : 'My submitted surveys'}</p>
          <h1>{isAdmin ? 'Survey Records' : 'My Records'}</h1>
        </div>
        <Link className="button-link" to="/surveys/new">Add survey</Link>
      </header>

      <form className="filters" onSubmit={load}>
        <input type="date" value={filters.fromDate} onChange={(e) => setFilter('fromDate', e.target.value)} />
        <input type="date" value={filters.toDate} onChange={(e) => setFilter('toDate', e.target.value)} />
        <select value={filters.studyArea} onChange={(e) => setFilter('studyArea', e.target.value)}>
          <option value="">Study area</option>
          {optionSets.studyArea.map((item) => <option key={item} value={item}>{labelize(item)}</option>)}
        </select>
        <select value={filters.cookingFuel} onChange={(e) => setFilter('cookingFuel', e.target.value)}>
          <option value="">Cooking</option>
          {optionSets.cookingFuel.map((item) => <option key={item} value={item}>{labelize(item)}</option>)}
        </select>
        <button>Apply</button>
        <button type="button" className="secondary-button" onClick={() => setFilters(emptyFilters)}>Clear</button>
      </form>

      {isAdmin && (
        <div className="export-row">
          <button className="secondary-button" onClick={() => exportFile('csv')}>Export CSV</button>
          <button className="secondary-button" onClick={() => exportFile('xlsx')}>Export Excel</button>
        </div>
      )}

      {error && <div className="alert error">{error}</div>}
      {!loading && (
        <div className="records-mobile-list">
          {records.map((record) => (
            <article className="record-card" key={record.id}>
              <div className="record-card-header">
                <div>
                  <strong>{record.surveyId}</strong>
                  <span>{record.surveyDate} - {labelize(record.studyArea || '')}</span>
                </div>
              </div>
              <div className="record-card-grid">
                <span>Age/Gender</span><strong>{record.age || '-'} / {labelize(record.gender || '') || '-'}</strong>
                <span>Cooking</span><strong>{labelize(record.primaryCookingFuel || '') || '-'}</strong>
                <span>Symptoms</span><strong>{record.mainSymptomsSummary}</strong>
              </div>
              <div className="row-actions">
                <Link className="action-button view-action" to={`/surveys/${record.id}`}>View</Link>
                <Link className="action-button edit-action" to={`/surveys/${record.id}/edit`}>Edit</Link>
                {isAdmin && <button className="action-button delete-action" onClick={() => remove(record.id)}>Delete</button>}
              </div>
            </article>
          ))}
        </div>
      )}
      <div className="table-card records-table">
        {loading ? <p>Loading records...</p> : (
          <table>
            <thead>
              <tr>
                <th>Survey ID</th>
                <th>Date</th>
                <th>Study Area</th>
                <th>Age</th>
                <th>Gender</th>
                <th>Cooking</th>
                <th>Main Symptoms</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {records.map((record) => (
                <tr key={record.id}>
                  <td>{record.surveyId}</td>
                  <td>{record.surveyDate}</td>
                  <td>{labelize(record.studyArea || '')}</td>
                  <td>{record.age}</td>
                  <td>{labelize(record.gender || '')}</td>
                  <td>{labelize(record.primaryCookingFuel || '')}</td>
                  <td>{record.mainSymptomsSummary}</td>
                  <td className="actions-cell">
                    <div className="row-actions">
                      <Link className="action-button view-action" to={`/surveys/${record.id}`}>View</Link>
                      <Link className="action-button edit-action" to={`/surveys/${record.id}/edit`}>Edit</Link>
                      {isAdmin && <button className="action-button delete-action" onClick={() => remove(record.id)}>Delete</button>}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
