import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/client';
import ChartPanel from '../components/ChartPanel';
import DashboardFilters from '../components/DashboardFilters';
import StatCard from '../components/StatCard';
import {
  ActivityIcon,
  BarChartIcon,
  CalendarIcon,
  HeartPulseIcon,
  HouseholdIcon,
  MapPinIcon,
  SyringeIcon,
  UsersIcon
} from '../components/Icon';

const emptyFilters = { studyArea: '', fromDate: '', toDate: '', gender: '', ageGroup: '' };

export default function Dashboard() {
  const [filters, setFilters] = useState(emptyFilters);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters]);

  async function load() {
    setLoading(true);
    setError('');
    try {
      const activeFilters = Object.fromEntries(Object.entries(filters).filter(([, v]) => v !== ''));
      const { data } = await api.get('/dashboard/summary', { params: activeFilters });
      setSummary(data);
    } catch {
      setError('Unable to load dashboard summary.');
    } finally {
      setLoading(false);
    }
  }

  function updateFilter(name, value) {
    setFilters((current) => ({ ...current, [name]: value }));
  }

  function resetFilters() {
    setFilters(emptyFilters);
  }

  if (error) return <div className="page"><div className="alert error">{error}</div></div>;

  const total = summary?.totalHouseholdsSurveyed ?? 0;
  const male = summary?.genderDistribution?.MALE ?? 0;
  const female = summary?.genderDistribution?.FEMALE ?? 0;
  const vaccinatedChildren = summary?.childVaccinationDistribution?.YES ?? 0;
  const vaccinatedRespondents = summary?.respondentVaccinationDistribution?.YES ?? 0;
  const pct = (value) => (total ? `${Math.round((value / total) * 100)}% of respondents` : undefined);

  const kpis = [
    { label: 'Total Households Surveyed', value: total, tone: 'blue', icon: HouseholdIcon },
    { label: 'Study Areas Covered', value: summary?.totalStudyAreasCovered ?? 0, tone: 'teal', icon: MapPinIcon },
    { label: 'Male Respondents', value: male, tone: 'blue', icon: UsersIcon, hint: pct(male) },
    { label: 'Female Respondents', value: female, tone: 'rose', icon: UsersIcon, hint: pct(female) },
    { label: 'Vaccinated Children', value: vaccinatedChildren, tone: 'green', icon: SyringeIcon, hint: pct(vaccinatedChildren) },
    { label: 'Vaccinated Respondents', value: vaccinatedRespondents, tone: 'teal', icon: SyringeIcon, hint: pct(vaccinatedRespondents) }
  ];

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">Field survey overview</p>
          <h1>Survey Dashboard</h1>
          <p className="muted dashboard-subtitle">Household-level air pollution and health indicators across study areas</p>
        </div>
        <Link className="button-link" to="/surveys/new">Add survey</Link>
      </header>

      <DashboardFilters filters={filters} onChange={updateFilter} onReset={resetFilters} />

      {loading && !summary ? (
        <p>Loading dashboard...</p>
      ) : (
        <>
          <section className="kpi-grid">
            {kpis.map((kpi) => (
              <StatCard key={kpi.label} label={kpi.label} value={kpi.value} tone={kpi.tone} icon={kpi.icon} hint={kpi.hint} />
            ))}
          </section>

          {total === 0 ? (
            <div className="chart-card dashboard-empty-state">
              <strong>No records match the current filters</strong>
              <span>Try adjusting or resetting the filters above.</span>
            </div>
          ) : (
            <>
              <h2 className="dashboard-section-title">Survey Coverage</h2>
              <ChartPanel
                title="Households Surveyed by Study Area"
                data={summary.surveyCountByStudyArea}
                icon={BarChartIcon}
                tone="blue"
                height={220}
              />

              <h2 className="dashboard-section-title">Respondent Demographics</h2>
              <section className="dashboard-pair-grid">
                <ChartPanel title="Age Distribution" data={summary.ageDistribution} icon={CalendarIcon} tone="amber" />
                <ChartPanel title="Gender Distribution" data={summary.genderDistribution} icon={UsersIcon} tone="blue" />
              </section>

              <h2 className="dashboard-section-title">Vaccination Coverage</h2>
              <section className="dashboard-pair-grid">
                <ChartPanel
                  title="Child Vaccination"
                  data={summary.childVaccinationDistribution}
                  type="stacked"
                  icon={SyringeIcon}
                  tone="green"
                />
                <ChartPanel
                  title="Respondent Vaccination"
                  data={summary.respondentVaccinationDistribution}
                  type="stacked"
                  icon={SyringeIcon}
                  tone="teal"
                />
              </section>

              <h2 className="dashboard-section-title">Health Profile</h2>
              <ChartPanel
                title="Existing Conditions"
                data={summary.conditionsCount}
                icon={HeartPulseIcon}
                tone="rose"
                horizontal
                sortByValue
              />
              <ChartPanel
                title="Common Symptoms"
                data={summary.commonSymptomsCount}
                icon={ActivityIcon}
                tone="amber"
                horizontal
                sortByValue
              />
            </>
          )}
        </>
      )}
    </div>
  );
}
