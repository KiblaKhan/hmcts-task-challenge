// Simple Playwright E2E example (requires the app + API running)
import { test, expect } from '@playwright/test';

test.describe('Tasks UI (smoke)', () => {
  test('list -> filter -> calendar -> details', async ({ page }) => {
    process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';
    await page.goto('https://localhost:3100/tasks', { waitUntil: 'networkidle' });
    await expect(page.getByRole('heading', { name: 'Tasks' })).toBeVisible();

    await page.selectOption('#status', 'OPEN');
    await page.click('button:has-text("Apply")');

    const calButton = page.locator('a:has-text("Calendar view"), a[aria-label="Calendar view"]');
    await calButton.first().click();

    await expect(page.getByRole('heading', { name: /Tasks calendar/i })).toBeVisible();

    const dot = page.locator('.app-dot-btn').first();
    if (await dot.isVisible()) {
      await dot.click();
      await expect(page.locator('.app-cal-overlay .app-popcard')).toBeVisible();
      await page.click('.app-popcard__close');
    }
  });
});
