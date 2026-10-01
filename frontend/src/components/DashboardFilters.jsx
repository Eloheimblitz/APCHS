import { labelize, optionSets } from '../utils/surveyConfig';

const AGE_GROUPS = ['Under 18', '18-34', '35-49', '50-64', '65+'];

export default function DashboardFilters({ filters, onChange, onReset }) {
  return (
    <form className="filters" onSubmit={(e) => e.preventDefault()}>
      <select value={filters.studyArea} onChange={(e) => onChange('studyArea', e.target.value)}>
        <option value="">Study area</option>
        {optionSets.studyArea.map((item) => <option key={item} value={item}>{labelize(item)}</option>)}
      </select>
      <input
        type="date"
        aria-label="From date"
        value={filters.fromDate}
        onChange={(e) => onChange('fromDate', e.target.value)}
      />
      <input
        type="date"
        aria-label="To date"
        value={filters.toDate}
        onChange={(e) => onChange('toDate', e.target.value)}
      />
      <select value={filters.gender} onChange={(e) => onChange('gender', e.target.value)}>
        <option value="">Gender</option>
        {optionSets.gender.map((item) => <option key={item} value={item}>{labelize(item)}</option>)}
      </select>
      <select value={filters.ageGroup} onChange={(e) => onChange('ageGroup', e.target.value)}>
        <option value="">Age group</option>
        {AGE_GROUPS.map((item) => <option key={item} value={item}>{item}</option>)}
      </select>
      <button type="button" className="secondary-button" onClick={onReset}>Reset filters</button>
    </form>
  );
}
