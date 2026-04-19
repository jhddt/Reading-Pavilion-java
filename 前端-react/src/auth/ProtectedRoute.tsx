import type { PropsWithChildren } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './AuthProvider'
import { hasAnyRole, type AppRole } from './roles'

type ProtectedRouteProps = PropsWithChildren<{
  allowedRoles?: AppRole[]
}>

export function ProtectedRoute({ children, allowedRoles }: ProtectedRouteProps) {
  const { isAuthenticated, user } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`} replace />
  }

  if (allowedRoles && allowedRoles.length > 0 && !hasAnyRole(user?.role, allowedRoles)) {
    return <Navigate to="/dashboard" replace />
  }

  return <>{children}</>
}
