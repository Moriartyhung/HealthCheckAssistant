import { afterEach, beforeEach, expect, it, vi } from 'vitest'

const worker = vi.hoisted(() => ({ start: vi.fn().mockResolvedValue(undefined) }))
vi.mock('../mocks/browser', () => ({ worker }))
beforeEach(() => { vi.resetModules(); vi.clearAllMocks() })
afterEach(() => { vi.unstubAllEnvs(); vi.unstubAllGlobals() })

it('defaults to real API even in development', async () => {
  vi.stubEnv('DEV', true); vi.stubEnv('VITE_USE_MSW', undefined)
  const runtime = await import('./runtime')
  expect(runtime.USE_MSW).toBe(false)
  await runtime.prepareApiMode()
  expect(worker.start).not.toHaveBeenCalled()
})
it('explicit mock mode starts MSW before rendering', async () => {
  vi.stubEnv('DEV', true); vi.stubEnv('VITE_USE_MSW', 'true')
  const runtime = await import('./runtime')
  expect(runtime.USE_MSW).toBe(true)
  await runtime.prepareApiMode()
  expect(worker.start).toHaveBeenCalledWith({ onUnhandledFrame: 'bypass', quiet: true })
})
it('production never starts the demo worker', async () => {
  vi.stubEnv('DEV', false); vi.stubEnv('VITE_USE_MSW', 'true')
  const runtime = await import('./runtime')
  expect(runtime.USE_MSW).toBe(false)
  await runtime.prepareApiMode()
  expect(worker.start).not.toHaveBeenCalled()
})
it('real mode unregisters only the old MSW worker', async () => {
  vi.stubEnv('VITE_USE_MSW', 'false')
  const oldMock = { active: { scriptURL: 'http://localhost:5173/mockServiceWorker.js' }, unregister: vi.fn().mockResolvedValue(true) }
  const unrelated = { active: { scriptURL: 'http://localhost:5173/other-worker.js' }, unregister: vi.fn() }
  vi.stubGlobal('navigator', { serviceWorker: { getRegistrations: vi.fn().mockResolvedValue([oldMock, unrelated]) } })
  await (await import('./runtime')).prepareApiMode()
  expect(oldMock.unregister).toHaveBeenCalledOnce()
  expect(unrelated.unregister).not.toHaveBeenCalled()
})
