const API_BASE = '/api';

function getToken() {
  return localStorage.getItem('upi_token');
}

export async function api(path, options = {}) {
  const token = getToken();
  const headers = {
    'Content-Type': 'application/json',
    ...options.headers,
  };
  if (token) headers['Authorization'] = `Bearer ${token}`;
  const res = await fetch(API_BASE + path, { ...options, headers });
  if (!res.ok) {
    const err = new Error(res.statusText || 'Request failed');
    err.status = res.status;
    try { err.body = await res.json(); } catch { err.body = await res.text(); }
    throw err;
  }
  if (res.status === 204) return null;
  return res.json();
}

export const auth = {
  login: (body) => api('/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  register: (body) => api('/auth/register', { method: 'POST', body: JSON.stringify(body) }),
};

export const upi = {
  balance: () => api('/upi/balance'),
  pay: (body) => api('/upi/pay', { method: 'POST', body: JSON.stringify(body) }),
  history: (page = 0, size = 20) => api(`/upi/history?page=${page}&size=${size}`),
};

export const admin = {
  fraudLogs: (userId, page = 0, size = 50) => {
    let path = `/admin/fraud-logs?page=${page}&size=${size}`;
    if (userId) path += `&userId=${userId}`;
    return api(path);
  },
  dashboard: () => api('/admin/dashboard'),
};
