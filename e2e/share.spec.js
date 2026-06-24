// M6 — a share link round-trips in the real browser: an adventure is encoded
// into a #a= URL, and opening that URL in a fresh context imports it and starts
// playing the same adventure.
const { test, expect } = require('@playwright/test');

test('a share link encodes an adventure and opening it imports + plays it', async ({ page, browser }) => {
  await page.goto('/index.html');

  const card = page.locator('.adventure-card', { hasText: 'Hajimemashite' });
  await expect(card).toBeVisible(); // wait for first-run seed + render
  await card.getByRole('button', { name: 'Share' }).click();

  // The banner exposes the share URL.
  const shareUrl = await page.locator('.share-url').inputValue();
  expect(shareUrl).toContain('#a=');

  // A fresh recipient (separate context = no shared localStorage) opens the link.
  const recipient = await browser.newContext();
  try {
    const page2 = await recipient.newPage();
    await page2.goto(shareUrl);

    // It routes straight into the player on the shared adventure.
    await expect(page2.locator('.player')).toBeVisible();
    await expect(page2.locator('.passage').first()).toContainText('はじめまして');
  } finally {
    await recipient.close();
  }
});
