import { Navigate, Route, Routes } from 'react-router-dom';
import { getSession } from './api/client';
import AppLayout from './layouts/AppLayout';
import Account from './pages/Account';
import ProtectedRoute from './components/ProtectedRoute';
import Dashboard from './pages/Dashboard';
import Login from './pages/Login';
import NotFound from './pages/NotFound';
import OfflineQueue from './pages/OfflineQueue';
import Records from './pages/Records';
import SurveyDetail from './pages/SurveyDetail';
import SurveyPage from './pages/SurveyPage';
import Users from './pages/Users';

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<RoleLanding />} />
          <Route path="/dashboard" element={<AdminOnly><Dashboard /></AdminOnly>} />
          <Route path="/surveys/new" element={<SurveyPage mode="create" />} />
          <Route path="/surveys" element={<Records />} />
          <Route path="/surveys/:id" element={<SurveyDetail />} />
          <Route path="/surveys/:id/edit" element={<SurveyPage mode="edit" />} />
          <Route path="/users" element={<AdminOnly><Users /></AdminOnly>} />
          <Route path="/account" element={<Account />} />
          <Route path="/offline-queue" element={<OfflineQueue />} />
          <Route path="/offline-queue/:id/edit" element={<SurveyPage mode="edit-pending" />} />
        </Route>
      </Route>
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}


function RoleLanding() {
  const session = getSession();
  return <Navigate to={session?.role === 'ADMIN' ? '/dashboard' : '/surveys/new'} replace />;
}

function AdminOnly({ children }) {
  const session = getSession();
  return session?.role === 'ADMIN' ? children : <Navigate to="/surveys/new" replace />;
}
