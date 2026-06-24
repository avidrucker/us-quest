// M3 — playing an adventure: start passage, choose-and-append (+scroll target),
// Back, the ending payoff, and Start over. Driven through the deterministic
// built-in Japanese adventure.
const { test, expect } = require('@playwright/test');

test.beforeEach(async ({ page }) => {
  await page.goto('/index.html');
  const card = page.locator('.adventure-card', { hasText: 'Hajimemashite' });
  await expect(card).toBeVisible(); // wait for first-run seed + render
  await card.getByRole('button', { name: 'Play ▸' }).click();
});

test('starting a playthrough shows the start passage', async ({ page }) => {
  await expect(page.locator('.player')).toBeVisible();
  await expect(page.locator('.passage.active')).toContainText('はじめまして');
  await expect(page.locator('.trail .passage')).toHaveCount(1);
});

test('choosing appends the next passage (and marks a scroll target)', async ({ page }) => {
  await page.locator('.btn.choice', { hasText: 'hajimemashite' }).click();
  await expect(page.locator('.trail .passage')).toHaveCount(2);
  await expect(page.locator('.passage.active')).toContainText(/introduce yourself/i);
  // the #8 append-and-scroll target rides on the active passage
  await expect(page.locator('#active-passage')).toBeVisible();
});

test('Back returns to the previous passage', async ({ page }) => {
  await page.locator('.btn.choice', { hasText: 'hajimemashite' }).click();
  await expect(page.locator('.trail .passage')).toHaveCount(2);
  await page.getByRole('button', { name: 'Back' }).click();
  await expect(page.locator('.trail .passage')).toHaveCount(1);
  await expect(page.locator('.passage.active')).toContainText('はじめまして');
});

test('a wrong answer loops back, the correct path reaches the ending + Start over', async ({ page }) => {
  // wrong answer → reaction with a single "Try again" that returns to the question
  await page.locator('.btn.choice', { hasText: 'arigatou' }).click();
  await expect(page.locator('.passage.active')).toContainText('not a greeting');
  await page.getByRole('button', { name: 'Try again' }).click();
  await expect(page.locator('.passage.active')).toContainText('はじめまして');

  // correct path to the success ending
  await page.locator('.btn.choice', { hasText: 'hajimemashite' }).click();
  await page.locator('.btn.choice', { hasText: 'desu' }).click();
  await page.locator('.btn.choice', { hasText: 'yoroshiku' }).click();

  await expect(page.locator('.ending')).toBeVisible();
  await expect(page.locator('.ending-note')).toContainText('the end');
  await expect(page.locator('.passage.active')).toContainText('Yatta');

  // Start over resets to the first passage
  await page.getByRole('button', { name: 'Start over' }).click();
  await expect(page.locator('.ending')).toHaveCount(0);
  await expect(page.locator('.trail .passage')).toHaveCount(1);
  await expect(page.locator('.passage.active')).toContainText('はじめまして');
});
