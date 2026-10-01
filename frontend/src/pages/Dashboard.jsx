import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import ChartPanel from '../components/ChartPanel';
import StatCard from '../components/StatCard';
import {
  ActivityIcon,
  BarChartIcon,
  CalendarIcon,
  HeartPulseIcon,
  HouseholdIcon,
  MapPinIcon,
  SyringeIcon
} from '../components/Icon';

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

  const genderBreakdown = [
    ['Male', summary.genderDistribution?.MALE ?? 0],
    ['Female', summary.genderDistribution?.FEMALE ?? 0]
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

      <div className="dashboard-hero-row">
        <ChartPanel
          title="Survey count by study area"
          data={summary.surveyCountByStudyArea}
          icon={BarChartIcon}
          tone="blue"
        />
        <div className="dashboard-sidebar">
          <StatCard
            label="Total households surveyed"
            value={summary.totalHouseholdsSurveyed}
            tone="blue"
            icon={HouseholdIcon}
            breakdown={genderBreakdown}
          />
          <StatCard
            label="Study areas covered"
            value={summary.totalStudyAreasCovered}
            tone="teal"
            icon={MapPinIcon}
          />
        </div>
      </div>

      <section className="chart-grid">
        <ChartPanel title="Age" data={summary.ageDistribution} icon={CalendarIcon} tone="amber" />
        <ChartPanel
          title="Child vaccination"
          data={summary.childVaccinationDistribution}
          type="pie"
          icon={SyringeIcon}
          tone="green"
        />
        <ChartPanel
          title="Respondent vaccination"
          data={summary.respondentVaccinationDistribution}
          type="pie"
          icon={SyringeIcon}
          tone="teal"
        />
        <ChartPanel title="Existing conditions" data={summary.conditionsCount} icon={HeartPulseIcon} tone="rose" />
        <ChartPanel title="Common symptoms" data={summary.commonSymptomsCount} icon={ActivityIcon} tone="amber" />
      </section>
    </div>
  );
}
