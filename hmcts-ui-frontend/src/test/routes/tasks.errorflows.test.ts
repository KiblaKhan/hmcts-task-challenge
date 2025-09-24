import request from 'supertest';
import { app } from '../../main/app';
import * as api from '../../main/services/tasksApi';

let errSpy: jest.SpyInstance;
beforeAll(() => { errSpy = jest.spyOn(console, 'error').mockImplementation(() => { }); });
afterAll(() => { errSpy.mockRestore(); });

describe('tasks routes: error flows', () => {
    beforeEach(() => {
        jest.restoreAllMocks();
    });

    it('POST /tasks returns 422 when title is missing (local validation)', async () => {
        // Local validation fires before hitting the API
        await request(app)
            .post('/tasks')
            .type('form')
            .send({ title: '' })
            .expect(422);
    });

    it('POST /tasks returns 400 when upstream createTask throws', async () => {
        jest.spyOn(api, 'createTask').mockRejectedValueOnce(new Error('boom'));
        await request(app)
            .post('/tasks')
            .type('form')
            .send({ title: 'Hello' }) // valid so it reaches the API
            .expect(400);
    });

    it('POST /tasks/:id/delete returns 400 when upstream delete fails', async () => {
        jest.spyOn(api, 'deleteTask').mockRejectedValueOnce(new Error('boom'));
        await request(app).post('/tasks/abc/delete').expect(400);
    });

    it('POST /tasks/:id/start returns 400 when updateTaskStatus fails', async () => {
        jest.spyOn(api, 'updateTaskStatus').mockRejectedValueOnce(new Error('boom'));
        await request(app).post('/tasks/abc/start').expect(400);
    });

    it('POST /tasks/:id/complete returns 400 when updateTaskStatus fails', async () => {
        jest.spyOn(api, 'updateTaskStatus').mockRejectedValueOnce(new Error('boom'));
        await request(app).post('/tasks/abc/complete').expect(400);
    });
});
