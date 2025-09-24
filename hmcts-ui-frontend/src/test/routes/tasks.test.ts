import request from 'supertest';

// Mock the services layer used by routes
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

describe('tasks routes', () => {
  beforeEach(() => {
    mocked.listTasks.mockReset();
    mocked.getTask.mockReset();
    mocked.createTask.mockReset();
    mocked.updateTaskStatus.mockReset();
    mocked.deleteTask.mockReset();
  });

  it('GET /tasks renders list and applies client-side status filter', async () => {
    mocked.listTasks.mockResolvedValue([
      { id: '1', title: 'Task A', status: 'OPEN', dueAt: null } as any,
      { id: '2', title: 'Task B', status: 'DONE', dueAt: null } as any,
    ]);

    const res = await request(app).get('/tasks?status=OPEN&sort=dueDate');

    expect(res.status).toBe(200);
    expect(mocked.listTasks).toHaveBeenCalledWith({
      sort: 'dueDate',
      page: undefined,
      page_size: undefined,
    });

    // Should show the OPEN task
    expect(res.text).toContain('Task A');
    expect(res.text).toContain('/tasks/1');

    // Should NOT show the DONE task
    // (use robust selectors instead of a single letter)
    expect(res.text).not.toContain('/tasks/2');
    expect(res.text).not.toMatch(/>\s*Task B\s*</);
  });

  it('GET /tasks/calendar renders calendar', async () => {
    mocked.listTasks.mockResolvedValue([
      { id: '1', title: 'A', status: 'OPEN', dueAt: '2025-09-30T17:00:00Z' } as any,
    ]);
    const res = await request(app).get('/tasks/calendar?status=OPEN');
    expect(res.status).toBe(200);
    // Look for a calendar container hook
    expect(res.text).toMatch(/app-cal|Calendar/i);
  });

  it('POST /tasks creates and redirects to details', async () => {
    mocked.createTask.mockResolvedValue({ id: '9', title: 'X', status: 'OPEN', dueAt: null } as any);
    const res = await request(app).post('/tasks').send('title=X');
    expect(res.status).toBeGreaterThanOrEqual(300); // 302
    expect(res.header.location).toMatch(/\/tasks\/9$/);
  });

  it('POST /tasks/:id/start triggers status update and redirects', async () => {
    mocked.updateTaskStatus.mockResolvedValue({ id: '1', title: 'A', status: 'IN_PROGRESS' } as any);
    const res = await request(app).post('/tasks/1/start');
    expect(mocked.updateTaskStatus).toHaveBeenCalledWith('1', 'IN_PROGRESS');
    expect(res.status).toBeGreaterThanOrEqual(300);
  });

  it('POST /tasks/:id/complete triggers status update and redirects', async () => {
    mocked.updateTaskStatus.mockResolvedValue({ id: '1', title: 'A', status: 'DONE' } as any);
    const res = await request(app).post('/tasks/1/complete');
    expect(mocked.updateTaskStatus).toHaveBeenCalledWith('1', 'DONE');
    expect(res.status).toBeGreaterThanOrEqual(300);
  });

  it('POST /tasks/:id/delete deletes and redirects', async () => {
    mocked.deleteTask.mockResolvedValue(undefined as any);
    const res = await request(app).post('/tasks/9/delete');
    expect(mocked.deleteTask).toHaveBeenCalledWith('9');
    expect(res.status).toBeGreaterThanOrEqual(300);
    expect(res.header.location).toBe('/tasks');
  });

  it('handles upstream API failure gracefully on list', async () => {
    mocked.listTasks.mockRejectedValue(new Error('boom'));
    const res = await request(app).get('/tasks');
    // Your route may render an error page with 200 or set 5xx. Accept either; assert copy.
    expect([200, 500, 502]).toContain(res.status);
    expect(res.text).toMatch(/Something went wrong|Failed to load tasks/i);
  });
});
