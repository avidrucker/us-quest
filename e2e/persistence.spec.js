// M4 — library persistence survives a real page reload (localStorage).
const { test, expect } = require('@playwright/test');

const STORE_KEY = 'us-quest/library';

test.beforeEach(async ({ page }) => {
  await page.goto('/index.html');
});

test('a saved adventure survives a page reload', async ({ page }) => {
  const title = 'E2E persistence probe';

  // Create + save a new adventure.
  await page.getByRole('button', { name: '+ New adventure' }).click();
  await expect(page.locator('.editor')).toBeVisible();
  await page.locator('.title-input').fill(title);
  await page.getByRole('button', { name: /save/i }).click();

  // Back on the library, the new adventure is listed.
  const card = page.locator('.adventure-card', { hasText: title });
  await expect(card).toBeVisible();

  // It was written to localStorage...
  const stored = await page.evaluate((k) => localStorage.getItem(k), STORE_KEY);
  expect(stored).toContain(title);

  // ...and it's still there after a full reload.
  await page.reload();
  await expect(page.locator('.adventure-card', { hasText: title })).toBeVisible();
});

test('first run seeds the built-in demos into the library', async ({ page }) => {
  // Count is relative, not hardcoded — built-in demos get added over time.
  await expect(page.locator('.adventure-card', { hasText: 'Hajimemashite' })).toBeVisible();
  expect(await page.locator('.adventure-card').count()).toBeGreaterThanOrEqual(3);
});
