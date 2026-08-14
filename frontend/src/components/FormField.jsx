import { labelize } from '../utils/surveyConfig';

export default function FormField({ field, value, onChange, disabled = false }) {
  const options = field.options || [];

  if (field.type === 'boolean') {
    return (
      <label className="field">
        <span>{field.label}{field.required && ' *'}</span>
        <select value={value === true ? 'true' : value === false ? 'false' : ''} onChange={(e) => onChange(e.target.value === '' ? null : e.target.value === 'true')} required={field.required} disabled={disabled}>
          <option value="">Select</option>
          <option value="true">Yes</option>
          <option value="false">No</option>
        </select>
      </label>
    );
  }

  if (field.type === 'select') {
    return (
      <label className="field">
        <span>{field.label}{field.required && ' *'}</span>
        <select value={value ?? ''} onChange={(e) => onChange(e.target.value)} required={field.required} disabled={disabled}>
          <option value="">Select</option>
          {options.map((option) => (
            <option key={option} value={option}>{labelize(option)}</option>
          ))}
        </select>
      </label>
    );
  }

  if (field.type === 'multiselect') {
    const selected = Array.isArray(value) ? value : [];
    function toggle(option) {
      if (selected.includes(option)) {
        onChange(selected.filter((item) => item !== option));
      } else {
        onChange([...selected, option]);
      }
    }
    return (
      <label className="field field-wide">
        <span>{field.label}{field.required && ' *'}</span>
        <div className="checkbox-group">
          {options.map((option) => (
            <label key={option} className="checkbox-option">
              <input type="checkbox" checked={selected.includes(option)} onChange={() => toggle(option)} disabled={disabled} />
              <span>{labelize(option)}</span>
            </label>
          ))}
        </div>
      </label>
    );
  }

  if (field.type === 'textarea') {
    return (
      <label className="field field-wide">
        <span>{field.label}</span>
        <textarea value={value ?? ''} onChange={(e) => onChange(e.target.value)} rows={4} disabled={disabled} />
      </label>
    );
  }

  return (
    <label className="field">
      <span>{field.label}{field.required && ' *'}</span>
      <input
        type={field.type || 'text'}
        step={field.step}
        value={value ?? ''}
        onChange={(e) => onChange(field.type === 'number' ? numericOrBlank(e.target.value) : e.target.value)}
        required={field.required}
        disabled={disabled}
      />
    </label>
  );
}

function numericOrBlank(value) {
  return value === '' ? '' : Number(value);
}
