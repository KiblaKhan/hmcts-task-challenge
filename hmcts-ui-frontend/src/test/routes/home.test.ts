import request from 'supertest';
import { app } from '../../main/app';

const logSpy = jest.spyOn(console, 'log').mockImplementation(() => { });
const errSpy = jest.spyOn(console, 'error').mockImplementation(() => { });
const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => { });
afterAll(() => { logSpy.mockRestore(); errSpy.mockRestore(); warnSpy.mockRestore(); });

describe('home route', () => {
  it('GET / renders welcome page', async () => {
    const res = await request(app).get('/');
    expect(res.status).toBe(200);
    // smoke check for logo or heading present in your layout/home
    expect(res.text).toMatch(/Welcome|HMCTS|Tasks/i);
  });
});
