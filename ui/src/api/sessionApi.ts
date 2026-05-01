import { SessionResponse } from '../types/session';

const BASE_URL = import.meta.env.VITE_SESSION_API_BASE_URL || '';

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`HTTP ${response.status}: ${text || response.statusText}`);
  }

  return response.json() as Promise<T>;
}

export function createSession(): Promise<SessionResponse> {
  return request<SessionResponse>(`${BASE_URL}/sessions`, {
    method: 'POST',
  });
}

export function simulateSession(sessionId: string): Promise<SessionResponse> {
  return request<SessionResponse>(`${BASE_URL}/sessions/${sessionId}/simulate`, {
    method: 'POST',
  });
}

export function getSession(sessionId: string): Promise<SessionResponse> {
  return request<SessionResponse>(`${BASE_URL}/sessions/${sessionId}`);
}