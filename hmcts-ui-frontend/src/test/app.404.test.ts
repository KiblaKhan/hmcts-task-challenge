import request from 'supertest';
import { app } from '../main/app';

let errSpy: jest.SpyInstance;
beforeAll(() => { errSpy = jest.spyOn(console, 'error').mockImplementation(() => { }); });
afterAll(() => { errSpy.mockRestore(); });

describe('app: 404 branch', () => {
    it('returns 404 on unknown route', async () => {
        await request(app).get('/__definitely_not_a_route__').expect(404);
    });
});
