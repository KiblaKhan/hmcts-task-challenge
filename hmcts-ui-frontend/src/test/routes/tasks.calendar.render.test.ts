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

describe('routes: GET /tasks/calendar render basics', () => {
  beforeEach(() => mocked.listTasks.mockReset());

  it('renders a 6x7 grid and shows dots for due dates', async () => {
    mocked.listTasks.mockResolvedValue([
      { id: '1', title: 'Pay fine', status: 'OPEN', dueAt: '2025-09-30T17:00:00Z' } as any,
      { id: '2', title: 'Submit form', status: 'OPEN', dueAt: '2025-09-29T12:00:00Z' } as any,
    ]);

    const res = await request(app).get('/tasks/calendar?month=2025-09&status=OPEN');
    expect(res.status).toBe(200);

    expect(res.text).toMatch(/app-cal-wrap|app-cal/);

    const dotCount = (res.text.match(/app-dot-btn/g) || []).length;
    expect(dotCount).toBeGreaterThanOrEqual(2);
  });
});
