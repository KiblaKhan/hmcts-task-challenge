import request from 'supertest';

jest.mock('../../main/services/tasksApi', () => ({
  __esModule: true,
  listTasks: jest.fn(),
  getTask: jest.fn(),
  createTask: jest.fn(),
  updateTaskStatus: jest.fn(),
  deleteTask: jest.fn(),
}));

import * as api from '../../main/services/tasksApi';
import { app } from '../../main/app';
const mocked = api as jest.Mocked<typeof api>;

const logSpy = jest.spyOn(console, 'log').mockImplementation(() => { });
const errSpy = jest.spyOn(console, 'error').mockImplementation(() => { });
const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => { });
afterAll(() => { logSpy.mockRestore(); errSpy.mockRestore(); warnSpy.mockRestore(); });

describe('routes: GET /tasks/:id', () => {
  beforeEach(() => {
    mocked.getTask.mockReset();
  });

  it('renders details page', async () => {
    mocked.getTask.mockResolvedValue({ id: '1', title: 'Pay fine', status: 'OPEN', dueAt: null } as any);
    const res = await request(app).get('/tasks/1');
    expect(res.status).toBe(200);
    expect(res.text).toContain('Pay fine');
    expect(res.text).toMatch(/Start|Complete|Delete|Back/i);
  });

  it('shows error page when upstream returns 404/boom', async () => {
    mocked.getTask.mockRejectedValue(Object.assign(new Error('not found'), { response: { status: 404 } }));
    const res = await request(app).get('/tasks/zzz');
    expect([404, 500, 502]).toContain(res.status);
    expect(res.text).toMatch(/Something went wrong|Not found|Failed/i);
  });
});
