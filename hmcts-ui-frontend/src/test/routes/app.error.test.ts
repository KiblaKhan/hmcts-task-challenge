import request from 'supertest';
import { app } from '../../main/app';

const logSpy = jest.spyOn(console, 'log').mockImplementation(() => { });
const errSpy = jest.spyOn(console, 'error').mockImplementation(() => { });
const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => { });
afterAll(() => { logSpy.mockRestore(); errSpy.mockRestore(); warnSpy.mockRestore(); });

// Add a route that throws synchronously -> bubbles to Express' error handler
app.get('/__boom', (_req, _res, next) => next(new Error('kaboom')));

describe('app error handler', () => {
    it('returns a 5xx on thrown errors (Express default handler)', async () => {
        const res = await request(app).get('/__boom');
        expect(res.status).toBeGreaterThanOrEqual(500);
        // Default Express error page in test/dev:
        expect(res.text).toContain('<title>Error</title>');
        expect(res.text).toContain('kaboom');
    });
});
