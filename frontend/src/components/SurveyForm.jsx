import { useState } from 'react';
import FormField from './FormField';
import HealthItemTable from './HealthItemTable';
import SurveyIdField from './SurveyIdField';
import { defaultSurvey, sections } from '../utils/surveyConfig';

export default function SurveyForm({ initialValues = {}, lockedFields = [], onSubmit, submitLabel = 'Save survey', loading = false, unlockAllSections = false }) {
  const [values, setValues] = useState({ ...defaultSurvey, ...initialValues });
  const [activeSection, setActiveSection] = useState(0);
  const [maxUnlockedSection, setMaxUnlockedSection] = useState(unlockAllSections ? sections.length - 1 : 0);
  const [gpsStatus, setGpsStatus] = useState('');
  const [gpsLoading, setGpsLoading] = useState(false);
  const [validationErrors, setValidationErrors] = useState([]);
  const [surveyIdTaken, setSurveyIdTaken] = useState(false);
  const [surveyIdChecking, setSurveyIdChecking] = useState(false);
  const surveyIdSectionIndex = sections.findIndex((s) => s.fields.some((f) => f.type === 'surveyIdNumber'));

  function handleSurveyIdDuplicate(taken) {
    setSurveyIdTaken(taken);
    if (taken) {
      setValidationErrors([{ sectionIndex: surveyIdSectionIndex, name: 'surveyId', label: 'Survey ID is already in use - enter a different number or use Suggest' }]);
      setActiveSection(surveyIdSectionIndex);
      setMaxUnlockedSection((max) => Math.max(max, surveyIdSectionIndex));
    }
  }

  function surveyIdBlockingError(sectionIndex) {
    if (surveyIdChecking) {
      return { sectionIndex, name: 'surveyId', label: 'Still checking Survey ID availability - wait a moment and try again' };
    }
    if (surveyIdTaken) {
      return { sectionIndex, name: 'surveyId', label: 'Survey ID is already in use - enter a different number or use Suggest' };
    }
    return null;
  }

  function update(name, value) {
    setValues((current) => ({ ...current, [name]: value }));
    setValidationErrors([]);
  }

  function updateHealthItems(name, items) {
    setValues((current) => ({ ...current, [name]: items }));
  }

  function visible(field) {
    return isVisible(field, values);
  }

  async function captureGps() {
    if (!navigator.geolocation) {
      setGpsStatus('GPS is not available in this browser.');
      return;
    }
    if (!window.isSecureContext) {
      setGpsStatus('GPS requires HTTPS. Use the deployed https:// site.');
      return;
    }
    setGpsLoading(true);
    setGpsStatus(navigator.onLine ? 'Capturing GPS location...' : 'Searching GPS offline. Keep Location on and stay in an open area if possible...');

    try {
      const position = await getCurrentGpsPosition({ enableHighAccuracy: true, timeout: navigator.onLine ? 45000 : 90000, maximumAge: 0 });
      applyGpsPosition(position, setValues);
      setGpsStatus('GPS location captured.');
    } catch (firstError) {
      if (firstError.code === firstError.PERMISSION_DENIED) {
        setGpsStatus(gpsErrorMessage(firstError));
        setGpsLoading(false);
        return;
      }

      try {
        setGpsStatus('Still searching for GPS. First offline location can take 2 to 3 minutes...');
        const offlineSearchMs = navigator.onLine ? 60000 : 180000;
        const watchedPosition = await watchGpsPosition({ enableHighAccuracy: true, timeout: offlineSearchMs, maximumAge: 0 }, offlineSearchMs);
        applyGpsPosition(watchedPosition, setValues);
        setGpsStatus('GPS location captured.');
      } catch (watchError) {
        if (watchError.code === watchError.PERMISSION_DENIED) {
          setGpsStatus(gpsErrorMessage(watchError));
          setGpsLoading(false);
          return;
        }

        try {
          const recentPosition = await getCurrentGpsPosition({ enableHighAccuracy: false, timeout: 5000, maximumAge: 10 * 60 * 1000 });
          applyGpsPosition(recentPosition, setValues);
          setGpsStatus('Recent phone location captured. Check it before saving if you moved far from this household.');
        } catch {
          setGpsStatus(gpsErrorMessage(watchError || firstError));
        }
      }
    } finally {
      setGpsLoading(false);
    }
  }

  function goNext() {
    const missing = requiredMissingInSection(sections[activeSection], activeSection, values);
    if (missing.length > 0) {
      setValidationErrors(missing);
      return;
    }
    const blockingSurveyId = surveyIdBlockingError(activeSection);
    if (blockingSurveyId) {
      setValidationErrors([blockingSurveyId]);
      return;
    }
    setValidationErrors([]);
    setActiveSection((value) => {
      const next = value + 1;
      setMaxUnlockedSection((max) => Math.max(max, next));
      return next;
    });
  }

  function goToSection(index) {
    if (index > maxUnlockedSection) return;
    setValidationErrors([]);
    setActiveSection(index);
  }

  function submit(event) {
    event.preventDefault();
    if (values.consentObtained !== true) return;
    const missing = requiredMissing(values);
    if (missing.length > 0) {
      setValidationErrors(missing);
      setActiveSection(missing[0].sectionIndex);
      setMaxUnlockedSection((max) => Math.max(max, missing[0].sectionIndex));
      return;
    }
    const blockingSurveyId = surveyIdBlockingError(surveyIdSectionIndex);
    if (blockingSurveyId) {
      setValidationErrors([blockingSurveyId]);
      setActiveSection(surveyIdSectionIndex);
      setMaxUnlockedSection((max) => Math.max(max, surveyIdSectionIndex));
      return;
    }
    onSubmit(values);
  }

  function preventImplicitSubmit(event) {
    if (event.key !== 'Enter') return;
    const tag = event.target.tagName;
    if (tag === 'TEXTAREA' || tag === 'BUTTON') return;
    event.preventDefault();
  }

  const section = sections[activeSection];
  const progress = Math.round(((activeSection + 1) / sections.length) * 100);

  return (
    <form className="survey-form" onSubmit={submit} onKeyDown={preventImplicitSubmit}>
      <div className="progress-card">
        <div>
          <strong>{section.title}</strong>
          <span>{progress}% complete</span>
        </div>
        <div className="progress-track"><span style={{ width: `${progress}%` }} /></div>
      </div>

      <div className="section-tabs">
        {sections.map((item, index) => (
          <button
            type="button"
            key={item.title}
            className={index === activeSection ? 'active' : ''}
            disabled={index > maxUnlockedSection}
            onClick={() => goToSection(index)}
          >
            {index + 1}
          </button>
        ))}
      </div>

      <section className="form-card">
        <h2>{section.title}</h2>
        {section.help && <p className="muted">{section.help}</p>}
        {activeSection === 0 && (
          <div className="gps-row">
            <button type="button" className="secondary-button" onClick={captureGps} disabled={gpsLoading}>
              {gpsLoading ? 'Capturing...' : 'Capture GPS'}
            </button>
            <span>{gpsStatus}</span>
          </div>
        )}
        {values.consentObtained === false && (
          <div className="alert error">Survey cannot be submitted without consent.</div>
        )}
        {validationErrors.length > 0 && (
          <div className="alert error">
            <strong>Complete the required fields marked * in this section before continuing.</strong>
            <ul>
              {validationErrors.slice(0, 6).map((item) => (
                <li key={`${item.sectionIndex}-${item.name}`}>{item.label}</li>
              ))}
              {validationErrors.length > 6 && <li>{validationErrors.length - 6} more required fields</li>}
            </ul>
          </div>
        )}
        {section.type === 'healthTable' && (
          <HealthItemTable
            catalog={section.catalog}
            items={values[section.catalogKey] || []}
            onChange={(items) => updateHealthItems(section.catalogKey, items)}
            mode={section.catalog ? 'catalog' : 'freeform'}
          />
        )}
        <div className="form-grid">
          {section.fields.filter(visible).map((field) => (
            field.type === 'surveyIdNumber' ? (
              <SurveyIdField key={field.name} value={values[field.name]} onChange={(value) => update(field.name, value)} disabled={isLocked(field, lockedFields)} required={field.required} onDuplicateChange={handleSurveyIdDuplicate} onCheckingChange={setSurveyIdChecking} />
            ) : (
              <FormField key={field.name} field={field} value={values[field.name]} onChange={(value) => update(field.name, value)} disabled={isLocked(field, lockedFields)} />
            )
          ))}
        </div>
      </section>

      <div className="form-actions sticky-actions">
        <button type="button" className="secondary-button" disabled={activeSection === 0} onClick={() => goToSection(activeSection - 1)}>Previous</button>
        {activeSection < sections.length - 1 ? (
          <button key="next-button" type="button" onClick={goNext}>Next</button>
        ) : (
          <button key="submit-button" type="submit" disabled={loading || values.consentObtained !== true}>{loading ? 'Saving...' : submitLabel}</button>
        )}
      </div>
    </form>
  );
}

function isLocked(field, lockedFields) {
  return field.readOnly || lockedFields.includes(field.name);
}

function requiredMissingInSection(section, sectionIndex, values) {
  const missing = [];
  section.fields.forEach((field) => {
    if (!field.required || !isVisible(field, values)) return;
    const value = values[field.name];
    const isEmpty = value === null || value === undefined || value === '' || (Array.isArray(value) && value.length === 0);
    if (isEmpty) {
      missing.push({ sectionIndex, name: field.name, label: `${section.title}: ${field.label}` });
    }
  });
  return missing;
}

function requiredMissing(values) {
  const missing = [];
  sections.forEach((section, sectionIndex) => {
    missing.push(...requiredMissingInSection(section, sectionIndex, values));
  });
  return missing;
}

function isVisible(field, values) {
  if (!field.showWhen) return true;
  return Object.entries(field.showWhen).every(([key, expected]) => {
    const actual = values[key];
    return Array.isArray(actual) ? actual.includes(expected) : actual === expected;
  });
}

function getCurrentGpsPosition(options) {
  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(resolve, reject, options);
  });
}

function watchGpsPosition(options, timeoutMs) {
  return new Promise((resolve, reject) => {
    let settled = false;
    let lastError = null;
    let watchId = null;
    let timerId = null;
    const finish = (callback, value) => {
      if (settled) return;
      settled = true;
      if (watchId !== null) navigator.geolocation.clearWatch(watchId);
      if (timerId !== null) clearTimeout(timerId);
      callback(value);
    };

    watchId = navigator.geolocation.watchPosition(
      (position) => finish(resolve, position),
      (error) => {
        lastError = error;
        if (error.code === error.PERMISSION_DENIED) {
          finish(reject, error);
        }
      },
      options
    );

    timerId = window.setTimeout(() => {
      const timeoutError = lastError || { code: 3 };
      finish(reject, timeoutError);
    }, timeoutMs);
  });
}

function applyGpsPosition(position, setValues) {
  setValues((current) => ({
    ...current,
    latitude: Number(position.coords.latitude.toFixed(7)),
    longitude: Number(position.coords.longitude.toFixed(7)),
    gpsAccuracy: Number(position.coords.accuracy.toFixed(1))
  }));
}

function gpsErrorMessage(error) {
  if (error.code === 1) {
    return 'Location is blocked. Allow Location for this site in your phone browser settings, then try again.';
  }
  if (error.code === 2) {
    return 'Location unavailable. Keep phone Location/GPS on, move near a window or open area, then try again. Use Grid ID if GPS is still unavailable.';
  }
  if (error.code === 3) {
    return 'GPS timed out. First offline GPS can take several minutes. Stay in an open area, try again, or enter the Grid ID.';
  }
  return 'Location could not be captured. You can enter the Grid ID manually.';
}
