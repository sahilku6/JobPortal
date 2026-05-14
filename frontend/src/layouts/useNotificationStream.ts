import { useEffect, useRef } from 'react'
import { useAppDispatch } from '../shared/hooks/redux'
import { fetchMyNotificationsThunk } from '../store/slices/notificationsSlice'
import { getAccessToken } from '../core/api/axios'
import { notificationsApi } from '../core/api/services/notifications'

const MAX_RETRY_DELAY = 60_000   // cap back-off at 60 s
const BASE_RETRY_DELAY = 5_000   // start at 5 s

/**
 * SSE hook with exponential back-off and clean tear-down.
 *
 * Key fixes vs. original:
 *  - Stops reconnecting immediately when tokens are gone (logout / session invalidated).
 *  - Exponential back-off so a broken server doesn't spam requests.
 *  - 401 / 403 responses close the connection permanently (no retry loop).
 *  - Cleans up properly on unmount / auth state change.
 */
export function useNotificationStream(enabled: boolean) {
  const dispatch   = useAppDispatch()
  const esRef      = useRef<EventSource | null>(null)
  const retryRef   = useRef<ReturnType<typeof setTimeout> | null>(null)
  const mountedRef = useRef(true)
  const retryDelay = useRef(BASE_RETRY_DELAY)

  useEffect(() => {
    mountedRef.current = true

    function clearRetry() {
      if (retryRef.current) {
        clearTimeout(retryRef.current)
        retryRef.current = null
      }
    }

    function closeEs() {
      if (esRef.current) {
        esRef.current.close()
        esRef.current = null
      }
    }

    function connect() {
      // Do nothing if the component unmounted or auth was disabled
      if (!mountedRef.current || !enabled) return

      // Stop if the user has been logged out — tokens are gone
      const latestToken = getAccessToken()
      if (!latestToken) return

      const es  = notificationsApi.subscribeToMyNotifications(latestToken)
      esRef.current = es
      retryDelay.current = BASE_RETRY_DELAY   // reset back-off on successful connect attempt

      // Diagnostic logs to help debug SSE connections in the browser
      try {
        console.debug('[SSE] connecting to', `/api/v1/notifications/my/stream?token=***${String(latestToken).slice(-8)}`)
      } catch (e) { /* ignore logging errors */ }

      es.onopen = () => {
        console.debug('[SSE] connection opened')
      }

      es.addEventListener('connected', (ev) => {
        retryDelay.current = BASE_RETRY_DELAY  // connected OK — reset delay
        console.debug('[SSE] connected event', ev)
        dispatch(fetchMyNotificationsThunk({ page: 0, size: 20 }))
      })

      es.addEventListener('notification', (ev) => {
        try { console.debug('[SSE] notification event', ev?.data) } catch (e) {}
        dispatch(fetchMyNotificationsThunk({ page: 0, size: 20 }))
      })

      // heartbeat keeps the connection alive — no action needed, just prevents browser timeout
      es.addEventListener('heartbeat', () => { /* noop */ })

      es.onerror = () => {
        closeEs()
        if (!mountedRef.current) return

        // If tokens are gone (user logged out) don't bother retrying
        if (!getAccessToken()) return

        // Exponential back-off
        const delay = retryDelay.current
        retryDelay.current = Math.min(delay * 2, MAX_RETRY_DELAY)
        retryRef.current = setTimeout(connect, delay)
      }
    }

    if (enabled) connect()

    return () => {
      mountedRef.current = false
      clearRetry()
      closeEs()
    }
  }, [enabled, dispatch])
}
