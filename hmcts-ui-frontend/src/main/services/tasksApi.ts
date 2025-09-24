import axios from 'axios';

export type TaskStatus = 'OPEN' | 'IN_PROGRESS' | 'DONE';
export type SortKey = 'dueDate' | 'status';

export interface Task {
  id: string;
  title: string;
  description?: string | null;
  status: TaskStatus;
  dueAt?: string | null;
}

const BASE_URL = process.env.TASKS_API_URL || 'http://localhost:8080';
const http = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' }
});

function cleanParams(params: Record<string, any>) {
  const out: Record<string, any> = {};
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== '') out[k] = v;
  }
  return out;
}

export interface ListOptions {
  status?: TaskStatus;
  page?: number;
  page_size?: number;
  sort?: SortKey; // 'dueDate' | 'status'
}

export async function listTasks(arg?: ListOptions | TaskStatus): Promise<Task[]> {
  let opts: ListOptions = {};
  if (typeof arg === 'string') {
    opts = { status: arg };
  } else if (typeof arg === 'object' && arg !== null) {
    opts = arg;
  }

  const params = cleanParams({
    status: opts.status,
    page: opts.page,
    page_size: opts.page_size,
    sort: opts.sort
  });

  const { data } = await http.get<Task[]>('/tasks', { params });
  return data;
}

export async function getTask(id: string): Promise<Task> {
  const { data } = await http.get<Task>(`/tasks/${encodeURIComponent(id)}`);
  return data;
}

export async function createTask(payload: { title: string; description?: string; dueAt?: string }): Promise<Task> {
  const { data } = await http.post<Task>('/tasks', payload);
  return data;
}

// Primary: PUT /tasks/{id}/status (with fallbacks for older APIs)
export async function updateTaskStatus(id: string, status: TaskStatus): Promise<Task> {
  const encId = encodeURIComponent(id);

  try {
    const { data } = await http.put<Task>(`/tasks/${encId}/status`, { status });
    return data;
  } catch (err: any) {
    const resp = err?.response as { status?: number; data?: unknown } | undefined;
    const code: number | undefined = resp?.status;
    const body = resp?.data;

    const bodyMsg =
      typeof body === 'string'
        ? body.trim()
        : body && typeof body === 'object'
          ? String((body as any).message ?? (body as any).error ?? '').trim() || undefined
          : undefined;

    const errMsg =
      typeof err?.message === 'string' && err.message.trim() ? err.message.trim() : undefined;

    // 1) Network / no response: just bubble up the original error message
    if (!resp) {
      throw new Error(errMsg || 'Request failed');
    }

    // 2) 400 with a server-provided message: rethrow that (tests expect /bad/i)
    if (code === 400 && bodyMsg) {
      throw new Error(bodyMsg);
    }

    // 3) Legacy/method-not-supported fallbacks
    if (code === 404 || code === 405 || code === 400) {
      try {
        const patched = await http.patch<Task>(`/tasks/${encId}`, { status });
        return patched.data;
      } catch { /* ignore and try verb-based fallbacks */ }

      try {
        if (status === 'IN_PROGRESS') {
          const started = await http.post<Task>(`/tasks/${encId}/start`);
          return started.data;
        }
        if (status === 'DONE') {
          const completed = await http.post<Task>(`/tasks/${encId}/complete`);
          return completed.data;
        }
      } catch { /* ignore; fall through to final throw */ }
    }

    // 4) Final: throw the best available message
    throw new Error(bodyMsg || errMsg || (code ? `HTTP ${code}` : 'Request failed'));
  }
}

export async function deleteTask(id: string): Promise<void> {
  await http.delete(`/tasks/${encodeURIComponent(id)}`);
}
