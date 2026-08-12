import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import api from '../api/client';
import HealthItemTable from '../components/HealthItemTable';
import { labelize, sections } from '../utils/surveyConfig';

export default function SurveyDetail() {
  const { id } = useParams();
  const [record, setRecord] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get(`/surveys/${id}`)
      .then(({ data }) => setRecord(data))
      .catch(() => setError('Unable to load survey detail.'));
  }, [id]);

  if (error) return <div className="page"><div className="alert error">{error}</div></div>;
  if (!record) return <div className="page"><p>Loading survey detail...</p></div>;

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">{record.householdId}</p>
          <h1>{record.surveyId}</h1>
        </div>
        <div className="header-actions">
          <Link className="secondary-link" to="/surveys">Records</Link>
          <Link className="button-link" to={`/surveys/${record.id}/edit`}>Edit</Link>
        </div>
      </header>

      {sections.map((section) => (
        <section className="detail-card" key={section.title}>
          <h2>{section.title}</h2>
          {section.type === 'healthTable' && (
            <HealthItemTable
              catalog={section.catalog}
              items={record[section.catalogKey] || []}
              mode={section.catalog ? 'catalog' : 'freeform'}
              readOnly
            />
          )}
          <div className="detail-grid">
            {section.fields.map((field) => (
              <div key={field.name}>
                <span>{field.label}</span>
                <strong>{format(record[field.name])}</strong>
              </div>
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}

function format(value) {
  if (value === true) return 'Yes';
  if (value === false) return 'No';
  if (value === null || value === undefined || value === '') return '-';
  return typeof value === 'string' ? labelize(value) : value;
}
