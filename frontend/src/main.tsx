import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Provider } from 'react-redux'
import { BrowserRouter } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import { store } from './store'
import App from './App'
import './styles/globals.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Provider store={store}>
      <BrowserRouter>
        {/* Single global Toaster - Phase 2 fix: prevents duplicate toasts */}
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 3000,
            style: {
              borderRadius: '10px',
              background: '#1e293b',
              color: '#f1f5f9',
              border: '1px solid rgba(255,255,255,0.08)',
            },
            success: { duration: 3000, iconTheme: { primary: '#10b981', secondary: '#f1f5f9' } },
            error:   { duration: 3000, iconTheme: { primary: '#ef4444', secondary: '#f1f5f9' } },
          }}
        />
        <App />
      </BrowserRouter>
    </Provider>
  </StrictMode>,
)
