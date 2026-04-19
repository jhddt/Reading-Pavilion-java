export type AppRole = 'STUDENT' | 'TEACHER' | 'ADMIN'

export function normalizeRole(role?: number | string | null): AppRole | null {
  if (role == null) return null

  const text = String(role).trim()
  const normalizedText = text.startsWith('ROLE_') ? text.slice(5) : text
  const upper = normalizedText.toUpperCase()

  if (text === '1' || upper === 'STUDENT') return 'STUDENT'
  if (text === '2' || upper === 'TEACHER') return 'TEACHER'
  if (text === '3' || upper === 'ADMIN') return 'ADMIN'

  return null
}

export function hasAnyRole(
  role: number | string | null | undefined,
  allowedRoles: AppRole[],
): boolean {
  const normalized = normalizeRole(role)
  return normalized != null && allowedRoles.includes(normalized)
}
