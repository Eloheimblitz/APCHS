import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 30000
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('apchs_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export function saveSession(session) {
  localStorage.setItem('apchs_token', session.token);
  localStorage.setItem('apchs_user', JSON.stringify({ username: session.username, role: session.role }));
}

export function getSession() {
  const user = localStorage.getItem('apchs_user');
  const token = localStorage.getItem('apchs_token');
  return token && user ? { token, ...JSON.parse(user) } : null;
}

export function clearSession() {
  localStorage.removeItem('apchs_token');
  localStorage.removeItem('apchs_user');
}

export function downloadBlob(data, filename, contentType) {
  const blob = new Blob([data], { type: contentType });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}

export default api;
