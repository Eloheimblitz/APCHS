import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { clearSession, getSession } from '../api/client';
import { useEffect, useState } from 'react';

export default function AppLayout() {
  const navigate = useNavigate();
  const session = getSession();
  const isAdmin = session?.role === 'ADMIN';
  const [online, setOnline] = useState(navigator.onLine);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    const update = () => setOnline(navigator.onLine);
    window.addEventListener('online', update);
    window.addEventListener('offline', update);
    return () => {
      window.removeEventListener('online', update);
      window.removeEventListener('offline', update);
    };
  }, []);

  function logout() {
    clearSession();
    navigate('/login');
  }

  if (!isAdmin) {
    return (
      <div className="field-shell">
        <header className="field-topbar">
          <div className="brand-group">
            <img src="/logo.png" alt="" className="brand-logo" />
            <div>
              <p className="eyebrow">APCHS Field</p>
              <h1>Household Survey</h1>
            </div>
          </div>
          <div className="field-user-panel">
            <span className={`connection-status ${online ? 'online' : 'offline'}`}>
              {online ? 'Online' : 'Offline'}
            </span>
            <strong>{session?.username}</strong>
            <button className="ghost-button" onClick={logout}>Sign out</button>
          </div>
        </header>
        <nav className="field-nav" aria-label="Surveyor navigation">
          <NavLink to="/surveys/new">New</NavLink>
          <NavLink to="/surveys">Records</NavLink>
          <NavLink to="/offline-queue">Sync</NavLink>
          <NavLink to="/account">Account</NavLink>
        </nav>
        <main className="field-content">
          <Outlet />
        </main>
      </div>
    );
  }

  return (
    <div className="app-shell">
      <header className="mobile-topbar">
        <div className="brand-group">
          <img src="/logo.png" alt="" className="brand-logo" />
          <strong>APCHS</strong>
        </div>
        <button type="button" className="menu-toggle" aria-label={menuOpen ? 'Close menu' : 'Open menu'} aria-expanded={menuOpen} onClick={() => setMenuOpen((value) => !value)}>
          <span />
          <span />
          <span />
        </button>
      </header>
      {menuOpen && <div className="sidebar-overlay" onClick={() => setMenuOpen(false)} />}
      <aside className={`sidebar ${menuOpen ? 'open' : ''}`}>
        <div className="brand-group">
          <img src="/logo.png" alt="" className="brand-logo" />
          <div>
            <p className="eyebrow">APCHS</p>
            <h1>Air Pollution Health Survey</h1>
          </div>
        </div>
        <nav onClick={() => setMenuOpen(false)}>
          <NavLink to="/dashboard">Dashboard</NavLink>
          <NavLink to="/surveys/new">Add Survey</NavLink>
          <NavLink to="/surveys">Records</NavLink>
          <NavLink to="/offline-queue">Pending Sync</NavLink>
          <NavLink to="/users">Users</NavLink>
          <NavLink to="/account">Account</NavLink>
        </nav>
        <div className="user-card">
          <span className={`connection-status ${online ? 'online' : 'offline'}`}>
            {online ? 'Online' : 'Offline'}
          </span>
          <strong>{session?.username}</strong>
          <span>{session?.role}</span>
          <button className="ghost-button" onClick={logout}>Sign out</button>
        </div>
      </aside>
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}
