// @vitest-environment jsdom
import { afterEach, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { ProgressSummary } from './ProgressSummary'
afterEach(cleanup)
it('exposes completion percentage to assistive technology', () => {
  render(<ProgressSummary completed={1} total={5}/> )
  expect(screen.getByRole('progressbar').getAttribute('aria-valuenow')).toBe('20')
})
