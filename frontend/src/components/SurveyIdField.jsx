import { useState } from 'react';
import api from '../api/client';

const PREFIX = `APCHS-${new Date().getFullYear()}-`;

export default function SurveyIdField({ value, onChange, disabled = false, required = false, onDuplicateChange = () => {} }) {
  const [loading, setLoading] = useState(false);
  const [checking, setChecking] = useState(false);
  const [duplicate, setDuplicate] = useState(false);
  const numberPart = value && value.startsWith(PREFIX) ? value.slice(PREFIX.length) : '';

  function handleNumberChange(raw) {
    const digitsOnly = raw.replace(/\D/g, '');
    setDuplicate(false);
    onDuplicateChange(false);
    if (digitsOnly === '') {
      onChange('');
      return;
    }
    onChange(`${PREFIX}${digitsOnly}`);
  }

  async function handleNumberBlur() {
    if (!numberPart) return;
    const padded = numberPart.padStart(3, '0');
    if (padded !== numberPart) {
      onChange(`${PREFIX}${padded}`);
    }
    await checkDuplicate(`${PREFIX}${padded}`);
  }

  async function checkDuplicate(surveyId) {
    setChecking(true);
    try {
      const { data } = await api.get('/surveys/check-id', { params: { surveyId } });
      setDuplicate(data.exists);
      onDuplicateChange(data.exists);
    } catch {
      // Ignore - offline or network error, fall back to server-side check on submit.
    } finally {
      setChecking(false);
    }
  }

  async function suggest() {
    setLoading(true);
    setDuplicate(false);
    onDuplicateChange(false);
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
          onBlur={handleNumberBlur}
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
      {checking && <span className="field-hint">Checking availability...</span>}
      {duplicate && !checking && (
        <span className="field-error">This Survey ID is already in use. Enter a different number or use Suggest.</span>
      )}
    </label>
  );
}
