import { useRef, useState } from 'react';
import api from '../api/client';

const CURRENT_YEAR_PREFIX = `APCHS-${new Date().getFullYear()}-`;
const PREFIX_PATTERN = /^(APCHS-\d{4}-)/;

export default function SurveyIdField({ value, onChange, disabled = false, required = false, onDuplicateChange = () => {}, onCheckingChange = () => {} }) {
  const [loading, setLoading] = useState(false);
  const [checking, setChecking] = useState(false);
  const [duplicate, setDuplicate] = useState(false);
  const requestIdRef = useRef(0);

  const existingPrefixMatch = value && value.match(PREFIX_PATTERN);
  const prefix = existingPrefixMatch ? existingPrefixMatch[1] : CURRENT_YEAR_PREFIX;
  const numberPart = existingPrefixMatch ? value.slice(prefix.length) : '';

  function handleNumberChange(raw) {
    const digitsOnly = raw.replace(/\D/g, '');
    setDuplicate(false);
    onDuplicateChange(false);
    if (digitsOnly === '') {
      onChange('');
      return;
    }
    onChange(`${prefix}${digitsOnly}`);
  }

  async function handleNumberBlur() {
    if (!numberPart) return;
    const padded = numberPart.padStart(3, '0');
    const paddedValue = `${prefix}${padded}`;
    if (padded !== numberPart) {
      onChange(paddedValue);
    }
    await checkDuplicate(paddedValue);
  }

  async function checkDuplicate(surveyId) {
    const requestId = ++requestIdRef.current;
    setChecking(true);
    onCheckingChange(true);
    try {
      const { data } = await api.get('/surveys/check-id', { params: { surveyId } });
      if (requestIdRef.current !== requestId) return;
      setDuplicate(data.exists);
      onDuplicateChange(data.exists);
    } catch {
      // Ignore - offline or network error, fall back to server-side check on submit.
    } finally {
      if (requestIdRef.current === requestId) {
        setChecking(false);
        onCheckingChange(false);
      }
    }
  }

  async function suggest() {
    setLoading(true);
    requestIdRef.current++;
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
        <span className="survey-id-prefix">{prefix}</span>
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
