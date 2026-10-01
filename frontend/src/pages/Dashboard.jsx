import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import ChartPanel from '../components/ChartPanel';
import StatCard from '../components/StatCard';

export default function Dashboard() {
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/dashboard/summary')
      .then(({ data }) => setSummary(data))
      .catch(() => setError('Unable to load dashboard summary.'));
  }, []);

  if (error) return <div className="page"><div className="alert error">{error}</div></div>;
  if (!summary) return <div className="page"><p>Loading dashboard...</p></div>;

  const cards = [
    ['Total households surveyed', summary.totalHouseholdsSurveyed, 'blue'],
    ['Study areas covered', summary.totalStudyAreasCovered, 'teal']
  ];

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">Field survey overview</p>
          <h1>Dashboard</h1>
        </div>
        <Link className="button-link" to="/surveys/new">Add survey</Link>
      </header>

      <section className="chart-grid">
        {cards.map(([label, value, tone]) => <StatCard key={label} label={label} value={value} tone={tone} />)}
        <ChartPanel title="Survey count by study area" data={summary.surveyCountByStudyArea} />
        <ChartPanel title="Gender" data={summary.genderDistribution} type="pie" />
        <ChartPanel title="Age" data={summary.ageDistribution} />
        <ChartPanel title="Child vaccination" data={summary.childVaccinationDistribution} type="pie" />
        <ChartPanel title="Respondent vaccination" data={summary.respondentVaccinationDistribution} type="pie" />
        <ChartPanel title="Existing conditions" data={summary.conditionsCount} />
        <ChartPanel title="Common symptoms" data={summary.commonSymptomsCount} />
      </section>
    </div>
  );
}
