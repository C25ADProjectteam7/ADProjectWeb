export function getAccessToken() {
  return localStorage.getItem('accessToken');
}

export function getRole() {
  return localStorage.getItem('role');
}

export function getFullName() {
  return localStorage.getItem('fullName');
}

export function isAuthenticated() {
  return Boolean(getAccessToken());
}

export function logout() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('role');
  localStorage.removeItem('fullName');

  window.location.href = '/login';
}
