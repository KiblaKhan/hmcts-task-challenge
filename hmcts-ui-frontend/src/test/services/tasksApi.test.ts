/**
 * Service tests: mock the axios instance returned by axios.create()
 * so our module-level `http` is set up correctly at import time.
 */
type AxiosInstanceLike = {
  get: jest.Mock, post: jest.Mock, put: jest.Mock, delete: jest.Mock
};

const axiosInstance: AxiosInstanceLike = {
  get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn(),
};

jest.mock('axios', () => ({
  __esModule: true,
  default: { create: jest.fn(() => axiosInstance) },
}));

import { listTasks, createTask, updateTaskStatus, deleteTask } from '../../main/services/tasksApi';

describe('services/tasksApi (axios instance)', () => {
  beforeEach(() => {
    axiosInstance.get.mockReset();
    axiosInstance.post.mockReset();
    axiosInstance.put.mockReset();
    axiosInstance.delete.mockReset();
  });

  it('listTasks calls GET /tasks with sort', async () => {
    axiosInstance.get.mockResolvedValue({ data: [{ id: '1', title: 'A', status: 'OPEN', dueAt: null }] });
    const res = await listTasks({ sort: 'dueDate' });
    expect(axiosInstance.get).toHaveBeenCalledWith('/tasks', {
      params: { page: undefined, page_size: undefined, sort: 'dueDate' },
    });
    expect(res[0].title).toBe('A');
  });

  it('createTask posts payload', async () => {
    axiosInstance.post.mockResolvedValue({ data: { id: '9', title: 'X', status: 'OPEN', dueAt: null } });
    const res = await createTask({ title: 'X' });
    expect(axiosInstance.post).toHaveBeenCalledWith('/tasks', { title: 'X' });
    expect(res.id).toBe('9');
  });

  it('updateTaskStatus uses PUT /tasks/{id}/status', async () => {
    axiosInstance.put.mockResolvedValue({ data: { id: '3', title: 'Y', status: 'DONE' } });
    const res = await updateTaskStatus('3', 'DONE');
    expect(axiosInstance.put).toHaveBeenCalledWith('/tasks/3/status', { status: 'DONE' });
    expect(res.status).toBe('DONE');
  });

  it('deleteTask sends DELETE /tasks/{id}', async () => {
    axiosInstance.delete.mockResolvedValue({ data: '' });
    await deleteTask('4');
    expect(axiosInstance.delete).toHaveBeenCalledWith('/tasks/4');
  });
});
