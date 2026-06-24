// Playwright smoke tests for the browser-only surfaces the node unit suite
// can't reach: localStorage persistence, share-hash import, and player/editor UX.
//
// The `webServer` block builds and serves the app via shadow-cljs `npm run dev`
// (first compile can take ~a minute). Tests use the system Chrome (`channel:
// 'chrome'`) so no Playwright browser download is needed.
const { defineConfig } = require('@playwright/test');

const PORT = process.env.PORT || 8080;
const BASE = `http://localhost:${PORT}`;

module.exports = defineConfig({
  testDir: 'e2e',
  fullyParallel: false,
  workers: 1, // one dev server; keep runs deterministic
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 1, // tolerate occasional cold first-render hiccups
  timeout: 30_000,
  expect: { timeout: 10_000 },
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    baseURL: BASE,
    channel: 'chrome',
    permissions: ['clipboard-read', 'clipboard-write'],
    viewport: { width: 900, height: 820 },
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chrome' }],
  webServer: {
    command: 'npm run dev',
    // Wait on the COMPILED bundle, not the static index.html — dev-http serves
    // index.html immediately (before the ~45s first compile), so gating on it
    // races the build and the app boots with no JS. js/main.js exists only once
    // the build completes.
    url: `${BASE}/js/main.js`,
    reuseExistingServer: !process.env.CI,
    timeout: 180_000,
  },
});
