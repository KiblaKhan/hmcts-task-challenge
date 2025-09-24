/** @jest-environment jsdom */

import request from 'supertest';
import { app } from '../../main/app';
import { axe } from 'jest-axe';
import './axe-setup';
import { JSDOM } from 'jsdom';

const logSpy = jest.spyOn(console, 'log').mockImplementation(() => { });
const errSpy = jest.spyOn(console, 'error').mockImplementation(() => { });
const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => { });
afterAll(() => { logSpy.mockRestore(); errSpy.mockRestore(); warnSpy.mockRestore(); });

describe('a11y: tasks pages', () => {
  it('list page has no obvious accessibility violations', async () => {
    const res = await request(app).get('/tasks?status=OPEN&sort=dueDate');
    // Parse HTML, then mount into the *global* document (jsdom env)
    const { window } = new JSDOM(res.text);
    document.body.innerHTML = window.document.body.innerHTML;

    const results = await axe(document.body);
    expect(results).toHaveNoViolations();
  });

  it('calendar page has no obvious accessibility violations', async () => {
    const res = await request(app).get('/tasks/calendar?month=2025-09&status=OPEN');
    const { window } = new JSDOM(res.text);
    document.body.innerHTML = window.document.body.innerHTML;

    const results = await axe(document.body);
    expect(results).toHaveNoViolations();
  });
});
