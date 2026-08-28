import { emptyHealthItem } from '../utils/surveyConfig';

const CLEARED_DETAILS = { visitedHospital: null, hospitalNames: '', ipd: null, opd: null, missedSchoolOrWork: null, daysMissed: '' };

export default function HealthItemTable({ catalog, items = [], onChange, mode = 'catalog', readOnly = false }) {
  const rows = mode === 'catalog'
    ? catalog.map((entry) => ({
        rowKey: entry.key,
        label: entry.label,
        item: items.find((current) => current.key === entry.key) || emptyHealthItem(entry.key)
      }))
    : items.map((item, index) => ({ rowKey: index, label: null, item }));

  function applyChange(item, field, value) {
    const next = { ...item, [field]: value };
    return field === 'present' && value === false ? { ...next, ...CLEARED_DETAILS } : next;
  }

  function updateItem(rowKey, field, value) {
    if (mode === 'catalog') {
      const next = catalog.map((entry) => {
        const current = items.find((row) => row.key === entry.key) || emptyHealthItem(entry.key);
        return entry.key === rowKey ? applyChange(current, field, value) : current;
      });
      onChange(next);
    } else {
      const next = items.map((item, index) => (index === rowKey ? applyChange(item, field, value) : item));
      onChange(next);
    }
  }

  return (
    <div className="table-card health-item-table">
      <table>
        <thead>
          <tr>
            <th>{mode === 'catalog' ? 'Item' : 'Description'}</th>
            {mode === 'catalog' && <th>Present</th>}
            <th>Visited Hospital</th>
            <th>Hospital Name(s)</th>
            <th>IPD</th>
            <th>OPD</th>
            <th>Miss School/Work</th>
            <th>Days Missed</th>
          </tr>
        </thead>
        <tbody>
          {rows.map(({ rowKey, label, item }) => {
            const showDetails = mode !== 'catalog' || item.present !== false;
            return (
              <tr key={rowKey}>
                <td>
                  {mode === 'catalog' ? (
                    <strong>{label}</strong>
                  ) : readOnly ? (
                    item.description || '-'
                  ) : (
                    <input
                      type="text"
                      value={item.description ?? ''}
                      placeholder={`Issue ${Number(rowKey) + 1}`}
                      onChange={(e) => updateItem(rowKey, 'description', e.target.value)}
                    />
                  )}
                </td>
                {mode === 'catalog' && (
                  <td><YesNoCell value={item.present} onChange={(value) => updateItem(rowKey, 'present', value)} readOnly={readOnly} /></td>
                )}
                <td>
                  {showDetails ? (
                    <YesNoCell value={item.visitedHospital} onChange={(value) => updateItem(rowKey, 'visitedHospital', value)} readOnly={readOnly} />
                  ) : <span className="muted">-</span>}
                </td>
                <td>
                  {!showDetails ? (
                    <span className="muted">-</span>
                  ) : readOnly ? (
                    item.hospitalNames || '-'
                  ) : (
                    <input type="text" value={item.hospitalNames ?? ''} onChange={(e) => updateItem(rowKey, 'hospitalNames', e.target.value)} />
                  )}
                </td>
                <td>
                  {showDetails ? (
                    <YesNoCell value={item.ipd} onChange={(value) => updateItem(rowKey, 'ipd', value)} readOnly={readOnly} />
                  ) : <span className="muted">-</span>}
                </td>
                <td>
                  {showDetails ? (
                    <YesNoCell value={item.opd} onChange={(value) => updateItem(rowKey, 'opd', value)} readOnly={readOnly} />
                  ) : <span className="muted">-</span>}
                </td>
                <td>
                  {showDetails ? (
                    <YesNoCell value={item.missedSchoolOrWork} onChange={(value) => updateItem(rowKey, 'missedSchoolOrWork', value)} readOnly={readOnly} />
                  ) : <span className="muted">-</span>}
                </td>
                <td>
                  {!showDetails ? (
                    <span className="muted">-</span>
                  ) : readOnly ? (
                    item.daysMissed ?? '-'
                  ) : (
                    <input
                      type="number"
                      min="0"
                      value={item.daysMissed ?? ''}
                      onChange={(e) => updateItem(rowKey, 'daysMissed', e.target.value === '' ? '' : Number(e.target.value))}
                    />
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

function YesNoCell({ value, onChange, readOnly }) {
  if (readOnly) {
    return <span>{value === true ? 'Yes' : value === false ? 'No' : '-'}</span>;
  }
  return (
    <div className="yn-toggle">
      <button type="button" className={value === true ? 'active' : ''} onClick={() => onChange(true)}>Y</button>
      <button type="button" className={value === false ? 'active' : ''} onClick={() => onChange(false)}>N</button>
    </div>
  );
}
