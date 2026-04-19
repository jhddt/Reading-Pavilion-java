import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthProvider'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { AppShell } from './components/AppShell'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { DashboardPage } from './pages/DashboardPage'
import { EssaysPage } from './pages/EssaysPage'
import { EssayCreatePage } from './pages/EssayCreatePage'
import { EssayDetailPage } from './pages/EssayDetailPage'
import { EssayManualReviewPage } from './pages/EssayManualReviewPage'
import { ReviewsPage } from './pages/ReviewsPage'
import { ReviewDetailPage } from './pages/ReviewDetailPage'
import { ReviewAnnotationPage } from './pages/ReviewAnnotationPage'
import { ReviewRerunPage } from './pages/ReviewRerunPage'
import { ReviewManualPage } from './pages/ReviewManualPage'
import { DimensionsPage } from './pages/DimensionsPage'
import { DimensionLibraryPage } from './pages/DimensionLibraryPage'
import { RuleCreatePage } from './pages/RuleCreatePage'
import { RuleEditPage } from './pages/RuleEditPage'
import { AuditLogsPage } from './pages/AuditLogsPage'
import { UserImportPage } from './pages/UserImportPage'
import { NotFoundPage } from './pages/NotFoundPage'

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <AppShell />
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="essays" element={<EssaysPage />} />
          <Route path="essays/create" element={<EssayCreatePage />} />
          <Route path="essays/:essayId" element={<EssayDetailPage />} />
          <Route
            path="essays/:essayId/manual-review"
            element={
              <ProtectedRoute allowedRoles={['TEACHER', 'ADMIN']}>
                <EssayManualReviewPage />
              </ProtectedRoute>
            }
          />
          <Route path="reviews" element={<ReviewsPage />} />
          <Route path="reviews/:reviewId" element={<ReviewAnnotationPage />} />
          <Route path="reviews/:reviewId/summary" element={<ReviewDetailPage />} />
          <Route path="reviews/:reviewId/rerun" element={<ReviewRerunPage />} />
          <Route
            path="reviews/:reviewId/manual"
            element={
              <ProtectedRoute allowedRoles={['TEACHER', 'ADMIN']}>
                <ReviewManualPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="dimensions"
            element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <DimensionsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="dimension-library"
            element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <DimensionLibraryPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="dimensions/create"
            element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <RuleCreatePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="dimensions/:ruleId/edit"
            element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <RuleEditPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="audit-logs"
            element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <AuditLogsPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="users/import"
            element={
              <ProtectedRoute allowedRoles={['ADMIN']}>
                <UserImportPage />
              </ProtectedRoute>
            }
          />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </AuthProvider>
  )
}
