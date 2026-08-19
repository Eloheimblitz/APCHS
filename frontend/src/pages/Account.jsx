import { useState } from 'react';
import api, { getSession } from '../api/client';

const emptyForm = {
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
};

export default function Account() {
  const session = getSession();
  const [form, setForm] = useState(emptyForm);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setMessage('');
    setError('');

    if (form.newPassword !== form.confirmPassword) {
      setError('New password and confirmation do not match.');
      return;
    }

    setLoading(true);
    try {
      await api.put('/users/me/password', {
        currentPassword: form.currentPassword,
        newPassword: form.newPassword
      });
      setForm(emptyForm);
      setMessage('Password changed successfully. Use the new password next time you sign in.');
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to change password.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">Signed in as {session?.role}</p>
          <h1>Account</h1>
        </div>
      </header>

      <section className="account-grid">
        <article className="form-card account-summary">
          <h2>Profile</h2>
          <div className="detail-grid">
            <div>
              <span>Username</span>
              <strong>{session?.username}</strong>
            </div>
            <div>
              <span>Role</span>
              <strong>{session?.role}</strong>
            </div>
          </div>
        </article>

        <article className="form-card">
          <h2>Change password</h2>
          <form className="password-form" onSubmit={submit}>
            <label className="field">
              <span>Current password</span>
              <input
                type="password"
                value={form.currentPassword}
                onChange={(event) => setForm({ ...form, currentPassword: event.target.value })}
                required
              />
            </label>
            <label className="field">
              <span>New password</span>
              <input
                type="password"
                value={form.newPassword}
                onChange={(event) => setForm({ ...form, newPassword: event.target.value })}
                minLength={8}
                required
              />
            </label>
            <label className="field">
              <span>Confirm new password</span>
              <input
                type="password"
                value={form.confirmPassword}
                onChange={(event) => setForm({ ...form, confirmPassword: event.target.value })}
                minLength={8}
                required
              />
            </label>

            {message && <div className="alert success">{message}</div>}
            {error && <div className="alert error">{error}</div>}

            <button disabled={loading}>{loading ? 'Updating...' : 'Update password'}</button>
          </form>
        </article>
      </section>
    </div>
  );
}
