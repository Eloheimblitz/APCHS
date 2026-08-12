import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import api, { saveSession } from '../api/client';

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ username: '', password: '' });
  const notice = location.state?.message;
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setError('');
    setLoading(true);
    try {
      const { data } = await api.post('/auth/login', form);
      saveSession(data);
      const intendedPath = location.state?.from?.pathname;
      const defaultPath = data.role === 'ADMIN' ? '/dashboard' : '/surveys/new';
      const adminOnlyPaths = ['/', '/dashboard', '/users'];
      const nextPath = data.role === 'ADMIN' || !adminOnlyPaths.includes(intendedPath) ? intendedPath || defaultPath : defaultPath;
      navigate(nextPath, { replace: true });
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed. Check username and password.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="login-screen">
      <section className="login-panel">
        <div className="login-brand-panel">
          <div>
            <span className="login-brand-mark">A</span>
          </div>
          <div>
            <p className="eyebrow" style={{ color: 'rgba(255,255,255,0.78)' }}>Community Health Survey</p>
            <h2>Air Pollution Household Data Collection</h2>
            <p>Field data from household interviews feeds directly into risk analysis and reporting for the study area.</p>
          </div>
          <div className="login-brand-stats">
            <strong>JWT-secured accounts</strong>
            <strong>Works offline in the field</strong>
            <strong>Role-based access for admins and surveyors</strong>
          </div>
        </div>
        <div className="login-form-panel">
          <div>
            <p className="eyebrow">Sign in</p>
            <h1>Welcome back</h1>
            <p className="muted">Data entered here is used for health survey analysis and should be handled with care.</p>
          </div>
          <form onSubmit={submit}>
            {notice && <div className="alert success">{notice}</div>}
            <label>
              Username
              <input value={form.username} onChange={(e) => setForm({ ...form, username: e.target.value })} required />
            </label>
            <label>
              Password
              <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
            </label>
            {error && <div className="alert error">{error}</div>}
            <button disabled={loading}>{loading ? 'Signing in...' : 'Sign in'}</button>
          </form>
        </div>
      </section>
    </main>
  );
}
