import { afterEach, expect, it, vi } from 'vitest'
afterEach(() => {vi.unstubAllEnvs(); vi.unstubAllGlobals(); vi.resetModules()})
it('blocks management operations and catalog in MSW mode before fetch', async () => {
  vi.resetModules(); vi.stubEnv('DEV', true); vi.stubEnv('VITE_USE_MSW', 'true')
  const fetch = vi.fn(); vi.stubGlobal('fetch', fetch)
  const api = await import('./patientManagementApi')
  for (const result of [api.listPatients(), api.getManagedPatient('P900'), api.createPatient({patientId: 'P900', patientName: '姓名'}), api.updatePatientName('P900', '姓名'), api.deletePatient('P900'), api.getExamCatalog()]) {
    await expect(result).rejects.toThrow('模擬模式下停用')
  }
  expect(fetch).not.toHaveBeenCalled()
})
