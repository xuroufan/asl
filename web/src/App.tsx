import { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './store/authStore'
import { TranslationProvider } from './i18n/useTranslation'
import { Layout } from './components/Layout'
import { ErrorBoundary } from './components/ErrorBoundary'
import { LoginPage } from './pages/LoginPage'

// Eager: LoginPage (first paint)
// Lazy: all other pages (code splitting)
const MarketPage = lazy(() => import('./pages/MarketPage').then(m => ({ default: m.MarketPage })))
const TradingPage = lazy(() => import('./pages/TradingPage').then(m => ({ default: m.TradingPage })))
const PositionsPage = lazy(() => import('./pages/PositionsPage').then(m => ({ default: m.PositionsPage })))
const SettingsPage = lazy(() => import('./pages/SettingsPage').then(m => ({ default: m.SettingsPage })))

function PageLoading() {
  return (
    <div className="flex items-center justify-center h-48">
      <div className="animate-spin w-5 h-5 border-2 border-brand border-t-transparent rounded-full" />
    </div>
  )
}

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const isLoggedIn = useAuthStore(s => s.isLoggedIn)
  if (!isLoggedIn) return <Navigate to="/login" replace />
  return <>{children}</>
}

export default function App() {
  return (
    <TranslationProvider>
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route path="/market" element={
            <ErrorBoundary><Suspense fallback={<PageLoading />}><MarketPage /></Suspense></ErrorBoundary>
          } />
          <Route path="/trading" element={
            <ErrorBoundary><Suspense fallback={<PageLoading />}><TradingPage /></Suspense></ErrorBoundary>
          } />
          <Route path="/positions" element={
            <ErrorBoundary><Suspense fallback={<PageLoading />}><PositionsPage /></Suspense></ErrorBoundary>
          } />
          <Route path="/settings" element={
            <ErrorBoundary><Suspense fallback={<PageLoading />}><SettingsPage /></Suspense></ErrorBoundary>
          } />
        </Route>
        <Route path="*" element={<Navigate to="/market" replace />} />
      </Routes>
    </BrowserRouter>
    </TranslationProvider>
  )
}
