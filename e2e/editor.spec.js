// M5 — the authoring editor surfaces validation problems live, and the
// route-based preview plays the unsaved working copy and returns.
const { test, expect } = require('@playwright/test');

test.beforeEach(async ({ page }) => {
  await page.goto('/index.html');
  const card = page.locator('.adventure-card', { hasText: 'Hajimemashite' });
  await expect(card).toBeVisible(); // wait for first-run seed + render
  await card.getByRole('button', { name: 'Edit' }).click();
  await expect(page.locator('.editor')).toBeVisible();
});

test('validation updates live as the working copy changes', async ({ page }) => {
  // A valid demo opens "ready to play".
  await expect(page.locator('.validation.ok')).toContainText('Ready to play');

  // Adding an unlinked passage makes it unreachable → a problem appears live.
  await page.getByRole('button', { name: '+ Add passage' }).click();
  await expect(page.locator('.validation.warn')).toBeVisible();
});

test('Preview plays the unsaved working copy and Back to editing returns', async ({ page }) => {
  await page.getByRole('button', { name: 'Preview ▸' }).click();
  await expect(page.locator('.player')).toBeVisible();

  const back = page.getByRole('button', { name: '← Back to editing' });
  await expect(back).toBeVisible();
  await back.click();

  await expect(page.locator('.editor')).toBeVisible();
});
