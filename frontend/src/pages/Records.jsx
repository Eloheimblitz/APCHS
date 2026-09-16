import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import api, { downloadBlob, getSession } from '../api/client';
import { labelize, labelizeList, optionSets } from '../utils/surveyConfig';

const emptyFilters = {
  surveyId: '',
  fromDate: '',
  toDate: '',
  studyArea: '',
  cookingFuel: ''
};

const PAGE_SIZE = 20;

export default function Records() {
  const [filters, setFilters] = useState(emptyFilters);
  const [records, setRecords] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [reloadToken, setReloadToken] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const session = getSession();
  const isAdmin = session?.role === 'ADMIN';
  const isFirstRun = useRef(true);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, reloadToken]);

  useEffect(() => {
    if (isFirstRun.current) {
      isFirstRun.current = false;
      return;
    }
    const handle = setTimeout(() => {
      setPage(0);
      setReloadToken((t) => t + 1);
    }, 400);
    return () => clearTimeout(handle);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters.surveyId]);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const { data } = await api.get('/surveys', { params: { ...activeFilters(), page, size: PAGE_SIZE } });
      setRecords(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch {
      setError('Unable to load survey records.');
    } finally {
      setLoading(false);
    }
  }

  function applyFilters(event) {
    event?.preventDefault();
    setPage(0);
    setReloadToken((t) => t + 1);
  }

  function clearFilters() {
    setFilters(emptyFilters);
    setPage(0);
    setReloadToken((t) => t + 1);
  }

  function goToPage(target) {
    setPage(Math.max(0, Math.min(target, totalPages - 1)));
  }

  function paginationItems() {
    const numbers = [];
    const add = (p) => { if (!numbers.includes(p)) numbers.push(p); };
    add(0);
    for (let p = page - 1; p <= page + 1; p++) {
      if (p >= 0 && p < totalPages) add(p);
    }
    add(totalPages - 1);
    numbers.sort((a, b) => a - b);

    const items = [];
    numbers.forEach((p, idx) => {
      if (idx > 0 && p - numbers[idx - 1] > 1) items.push({ type: 'ellipsis', key: `e-${p}` });
      items.push({ type: 'page', value: p, key: `p-${p}` });
    });
    return items;
  }

  async function remove(id) {
    if (!confirm('Delete this survey record?')) return;
    await api.delete(`/surveys/${id}`);
    setReloadToken((t) => t + 1);
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

      <form className="filters" onSubmit={applyFilters}>
        <input
          type="text"
          placeholder="Search Survey ID"
          value={filters.surveyId}
          onChange={(e) => setFilter('surveyId', e.target.value)}
        />
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
        <button type="button" className="secondary-button" onClick={clearFilters}>Clear</button>
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
                <span>Age/Gender</span><strong>{record.age ?? '-'} / {labelize(record.gender || '') || '-'}</strong>
                <span>Cooking</span><strong>{labelizeList(record.primaryCookingFuel)}</strong>
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
                  <td>{labelizeList(record.primaryCookingFuel)}</td>
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

      {!loading && totalElements > 0 && (
        <div className="pagination">
          <button type="button" className="secondary-button" onClick={() => goToPage(page - 1)} disabled={page === 0}>
            Previous
          </button>
          <div className="pagination-pages">
            {paginationItems().map((item) =>
              item.type === 'ellipsis' ? (
                <span key={item.key} className="pagination-ellipsis">&hellip;</span>
              ) : (
                <button
                  key={item.key}
                  type="button"
                  className={item.value === page ? 'pagination-page active' : 'pagination-page'}
                  onClick={() => goToPage(item.value)}
                >
                  {item.value + 1}
                </button>
              )
            )}
          </div>
          <button
            type="button"
            className="secondary-button"
            onClick={() => goToPage(page + 1)}
            disabled={page >= totalPages - 1}
          >
            Next
          </button>
          <span className="pagination-summary">{totalElements} record{totalElements === 1 ? '' : 's'}</span>
        </div>
      )}

      {!loading && totalElements === 0 && <p className="muted">No survey records found.</p>}
    </div>
  );
}
