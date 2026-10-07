export const USE_MSW = import.meta.env.DEV && import.meta.env.VITE_USE_MSW === 'true'
export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/+$/, '')

export async function prepareApiMode() {
  if (USE_MSW) {
    const { worker } = await import('../mocks/browser')
    await worker.start({ onUnhandledFrame: 'bypass', quiet: true })
    return
  }
  // Remove the demo registration when returning to real mode; preserve other workers.
  if (typeof navigator !== 'undefined' && 'serviceWorker' in navigator) {
    const registrations = await navigator.serviceWorker.getRegistrations()
    await Promise.all(registrations.filter(registration =>
      [registration.active, registration.waiting, registration.installing].some(worker =>
        worker && new URL(worker.scriptURL).pathname === '/mockServiceWorker.js'
      )
    ).map(registration => registration.unregister()))
  }
}
