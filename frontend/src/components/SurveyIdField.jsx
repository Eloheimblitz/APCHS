import { useState } from 'react';
import api from '../api/client';

const PREFIX = `APCHS-${new Date().getFullYear()}-`;

export default function SurveyIdField({ value, onChange, disabled = false, required = false }) {
  const [loading, setLoading] = useState(false);
  const numberPart = value && value.startsWith(PREFIX) ? value.slice(PREFIX.length) : '';

  function handleNumberChange(raw) {
    const digitsOnly = raw.replace(/\D/g, '');
    if (digitsOnly === '') {
      onChange('');
      return;
    }
    onChange(`${PREFIX}${digitsOnly.padStart(3, '0')}`);
  }

  async function suggest() {
    setLoading(true);
    try {
      const { data } = await api.get('/surveys/next-id');
      onChange(data.surveyId);
    } catch {
      // Ignore - surveyor can still enter a number manually.
    } finally {
      setLoading(false);
    }
  }

  return (
    <label className="field">
      <span>Survey ID{required && ' *'}</span>
      <div className="survey-id-input">
        <span className="survey-id-prefix">{PREFIX}</span>
        <input
          type="text"
          inputMode="numeric"
          value={numberPart}
          onChange={(e) => handleNumberChange(e.target.value)}
          placeholder="001"
          disabled={disabled}
          required={required}
        />
        {!disabled && (
          <button type="button" className="secondary-button" onClick={suggest} disabled={loading}>
            {loading ? '...' : 'Suggest'}
          </button>
        )}
      </div>
    </label>
  );
}
