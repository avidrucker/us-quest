// #19 — export an adventure to an .edn file and import it back, in the browser.
const { test, expect } = require('@playwright/test');
const fs = require('node:fs');

test('export downloads an .edn file; importing it adds a copy (non-destructive)', async ({ page }) => {
  await page.goto('/index.html');
  const card = page.locator('.adventure-card', { hasText: 'Hajimemashite' });
  await expect(card).toBeVisible(); // wait for first-run seed + render
  const before = await page.locator('.adventure-card').count(); // relative, not hardcoded

  // Export → a real file download.
  const [download] = await Promise.all([
    page.waitForEvent('download'),
    card.getByRole('button', { name: 'Export' }).click(),
  ]);
  expect(download.suggestedFilename()).toBe('hajimemashite-a-first-meeting-in-japanese.edn');

  const filePath = await download.path();
  const contents = fs.readFileSync(filePath, 'utf8');
  expect(contents).toContain(':adventure/id');
  expect(contents).toContain('はじめまして');

  // Import that same file. The id collides with the original demo, so it's
  // added under a fresh id (non-destructive) — the library grows by exactly one.
  await page.setInputFiles('#import-file-input', filePath);
  await expect(page.locator('.notice.ok')).toContainText('Imported');
  await expect(page.locator('.adventure-card')).toHaveCount(before + 1);
});

test('importing a non-adventure file shows an error and leaves the library intact', async ({ page }, testInfo) => {
  await page.goto('/index.html');
  await expect(page.locator('.adventure-card', { hasText: 'Hajimemashite' })).toBeVisible();
  const before = await page.locator('.adventure-card').count();

  const junk = testInfo.outputPath('junk.edn');
  fs.writeFileSync(junk, '{:hello "not an adventure"}');

  await page.setInputFiles('#import-file-input', junk);
  await expect(page.locator('.notice.error')).toBeVisible();
  await expect(page.locator('.adventure-card')).toHaveCount(before);
});
