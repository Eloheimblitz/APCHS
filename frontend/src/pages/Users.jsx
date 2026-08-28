import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { clearSession, getSession } from '../api/client';

const blankUser = {
  username: '',
  password: '',
  role: 'SURVEYOR'
};

export default function Users() {
  const navigate = useNavigate();
  const session = getSession();
  const [users, setUsers] = useState([]);
  const [form, setForm] = useState(blankUser);
  const [passwords, setPasswords] = useState({});
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (session?.role === 'ADMIN') {
      loadUsers();
    }
  }, [session?.role]);

  async function loadUsers() {
    setError('');
    try {
      const { data } = await api.get('/users');
      setUsers(data);
    } catch (err) {
      if ([401, 403].includes(err.response?.status)) {
        clearSession();
        navigate('/login', { replace: true, state: { message: 'Please sign in again as admin.' } });
        return;
      }
      setError(err.response?.data?.message || 'Unable to load users.');
    }
  }

  async function createUser(event) {
    event.preventDefault();
    setLoading(true);
    setError('');
    setMessage('');
    try {
      await api.post('/users', form);
      setForm(blankUser);
      setMessage('User created.');
      await loadUsers();
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to create user.');
    } finally {
      setLoading(false);
    }
  }

  async function updateUser(id, changes) {
    setError('');
    setMessage('');
    try {
      await api.put(`/users/${id}`, changes);
      setMessage('User updated.');
      await loadUsers();
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to update user.');
    }
  }

  async function deleteUser(user) {
    if (!confirm(`Delete user "${user.username}"? This cannot be undone.`)) return;
    setError('');
    setMessage('');
    try {
      await api.delete(`/users/${user.id}`);
      setMessage('User deleted.');
      await loadUsers();
    } catch (err) {
      setError(err.response?.data?.message || 'Unable to delete user.');
    }
  }

  async function resetPassword(user) {
    const password = passwords[user.id];
    if (!password || password.length < 8) {
      setError('Password must be at least 8 characters.');
      return;
    }
    await updateUser(user.id, { password });
    setPasswords((current) => ({ ...current, [user.id]: '' }));
  }

  if (session?.role !== 'ADMIN') {
    return (
      <div className="page">
        <div className="alert error">Only admins can manage users.</div>
      </div>
    );
  }

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">Admin controls</p>
          <h1>User Management</h1>
        </div>
      </header>

      <section className="form-card">
        <h2>Create user</h2>
        <form className="user-create-form" onSubmit={createUser}>
          <label className="field">
            <span>Username</span>
            <input value={form.username} onChange={(event) => setForm({ ...form, username: event.target.value })} required />
          </label>
          <label className="field">
            <span>Password</span>
            <input type="password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} minLength={8} required />
          </label>
          <label className="field">
            <span>Role</span>
            <select value={form.role} onChange={(event) => setForm({ ...form, role: event.target.value })}>
              <option value="SURVEYOR">Surveyor</option>
              <option value="ADMIN">Admin</option>
            </select>
          </label>
          <button disabled={loading}>{loading ? 'Creating...' : 'Create user'}</button>
        </form>
      </section>

      {message && <div className="alert success">{message}</div>}
      {error && <div className="alert error">{error}</div>}

      <section className="table-card">
        <table className="users-table">
          <thead>
            <tr>
              <th>Username</th>
              <th>Role</th>
              <th>Status</th>
              <th>Reset Password</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id}>
                <td>
                  <strong>{user.username}</strong>
                  {user.username === session.username && <span className="self-chip">You</span>}
                </td>
                <td>
                  <select value={user.role} onChange={(event) => updateUser(user.id, { role: event.target.value })}>
                    <option value="SURVEYOR">Surveyor</option>
                    <option value="ADMIN">Admin</option>
                  </select>
                </td>
                <td>
                  <select value={user.enabled ? 'true' : 'false'} onChange={(event) => updateUser(user.id, { enabled: event.target.value === 'true' })}>
                    <option value="true">Enabled</option>
                    <option value="false">Disabled</option>
                  </select>
                </td>
                <td>
                  <div className="password-reset">
                    <input
                      type="password"
                      placeholder="New password"
                      value={passwords[user.id] || ''}
                      onChange={(event) => setPasswords((current) => ({ ...current, [user.id]: event.target.value }))}
                    />
                    <button className="secondary-button" onClick={() => resetPassword(user)}>Reset</button>
                  </div>
                </td>
                <td>
                  {user.username !== session.username && (
                    <button className="action-button delete-action" onClick={() => deleteUser(user)}>Delete</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  );
}
