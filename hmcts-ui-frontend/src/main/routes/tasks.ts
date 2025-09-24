import express, { Application, Request, Response } from 'express';
import { listTasks, getTask, createTask, updateTaskStatus, deleteTask, TaskStatus, Task } from '../services/tasksApi';

function toDateOnly(iso?: string | null): string | null {
  if (!iso) return null;
  if (iso.length >= 10) return iso.slice(0, 10);
  try { return new Date(iso).toISOString().slice(0, 10); } catch { return null; }
}

function withFlags(tasks: Task[]) {
  const today = new Date().toISOString().slice(0, 10);
  return tasks.map(t => {
    const ymd = toDateOnly(t.dueAt);
    const overdue = !!(ymd && ymd < today);
    const dueToday = !!(ymd && ymd === today);
    return { ...t, _ymd: ymd, _overdue: overdue, _dueToday: dueToday };
  });
}

export default function registerTasks(app: Application) {
  const router = express.Router();
  const parseBody = express.urlencoded({ extended: true });

  // LIST
  router.get('/', async (req: Request, res: Response) => {
    try {
      const status = req.query.status as TaskStatus | undefined;
      const sortQ = (req.query.sort as string | undefined);
      const sort = sortQ === 'dueDate' || sortQ === 'status' ? sortQ : undefined;
      const page = req.query.page ? parseInt(String(req.query.page), 10) : undefined;
      const page_size = req.query.page_size ? parseInt(String(req.query.page_size), 10) : undefined;

      const raw = await listTasks({ /* API supports sorting/paging only */ sort, page, page_size });
      let tasks = withFlags(raw);

      // Client-side filter by status (API may not support status filter)
      if (status) {
        tasks = tasks.filter(t => t.status === status);
      }

      res.render('tasks/list.njk', { tasks, status, sort, page, page_size });
    } catch (e: any) {
      console.error('[tasks] GET /tasks failed:', e?.message || e, e?.stack || '');
      res.status(502).render('error.njk', { message: 'Failed to load tasks', error: e?.message ?? String(e) });
    }
  });

  // NEW
  router.get('/new', (_req, res) => {
    res.render('tasks/form.njk', { values: {}, errors: {} });
  });

  router.post('/', parseBody, async (req, res) => {
    const { title, description, dueAt } = req.body;
    const values = { title, description, dueAt };
    try {
      if (!title || String(title).trim().length === 0) {
        return res.status(422).render('tasks/form.njk', { values, errors: { title: 'Enter a title' } });
      }
      const task = await createTask({
        title: String(title).trim(),
        description: description?.trim() || undefined,
        dueAt: dueAt || undefined
      });
      res.redirect(`/tasks/${encodeURIComponent(task.id)}`);
    } catch (e: any) {
      res.status(400).render('tasks/form.njk', { values, errors: { global: e?.message ?? String(e) } });
    }
  });

  // CALENDAR
  router.get('/calendar', async (req, res) => {
    try {
      const status = req.query.status as TaskStatus | undefined;
      // before: only YYYY-MM
      // const m = (req.query.month as string) ?? now.toISOString().slice(0, 7);
      // if (!/^\d{4}-\d{2}$/.test(m)) throw new Error('Invalid month query param');

      const rawMonth = (req.query.month as string) ?? new Date().toISOString().slice(0, 7);
      let monthStr: string;

      if (/^\d{4}-\d{2}$/.test(rawMonth)) {
        monthStr = rawMonth;                // YYYY-MM
      } else if (/^\d{4}-\d{2}-\d{2}$/.test(rawMonth)) {
        monthStr = rawMonth.slice(0, 7);    // YYYY-MM-DD -> YYYY-MM
      } else {
        throw new Error('Invalid month query param');
      }
      const base = new Date(monthStr + '-01T00:00:00Z');
      if (isNaN(base.getTime())) throw new Error('Invalid month query param');

      const year = base.getUTCFullYear();
      const month = base.getUTCMonth();

      const first = new Date(Date.UTC(year, month, 1));
      const firstDay = (first.getUTCDay() + 6) % 7; // Mon=0
      const start = new Date(Date.UTC(year, month, 1 - firstDay));
      const end = new Date(start);
      end.setUTCDate(start.getUTCDate() + 42); // exclusive

      // Build 6-week grid
      const days: any[] = [];
      for (let i = 0; i < 42; i++) {
        const d = new Date(start);
        d.setUTCDate(start.getUTCDate() + i);
        const ymd = d.toISOString().slice(0, 10);
        days.push({ date: ymd, inMonth: d.getUTCMonth() === month, day: d.getUTCDate() });
      }

      let raw: Task[] = [];
      try {
        raw = await listTasks({ page: 1, page_size: 500, sort: 'dueDate' });
      } catch (err: any) {
        console.warn('[tasks] calendar: API rejected paging; retrying without it', err?.message || err);
        raw = await listTasks({ sort: 'dueDate' });
      }
      let flagged = withFlags(raw);

      // Client-side status filter
      if (status) {
        flagged = flagged.filter(t => t.status === status);
      }

      // Month filter for compact list
      const monthStart = new Date(Date.UTC(year, month, 1));
      const monthEnd = new Date(Date.UTC(year, month + 1, 1));
      const monthStartYMD = monthStart.toISOString().slice(0, 10);
      const monthEndYMD = monthEnd.toISOString().slice(0, 10);
      const monthTasks = flagged.filter(t => t._ymd && t._ymd >= monthStartYMD && t._ymd < monthEndYMD);

      // byDate map for the 6-week grid
      const startYMD = start.toISOString().slice(0, 10);
      const endYMD = end.toISOString().slice(0, 10);
      const byDate: Record<string, any[]> = {};
      for (const t of flagged) {
        if (!t._ymd) continue;
        if (t._ymd >= startYMD && t._ymd < endYMD) {
          (byDate[t._ymd] ||= []).push(t);
        }
      }

      const prevMonth = new Date(Date.UTC(year, month - 1, 1)).toISOString().slice(0, 7);
      const nextMonth = new Date(Date.UTC(year, month + 1, 1)).toISOString().slice(0, 7);
      const monthLabel = new Date(Date.UTC(year, month, 1)).toLocaleString('en-GB', { month: 'long', year: 'numeric', timeZone: 'UTC' });

      res.render('tasks/calendar.njk', { days, monthStr, monthLabel, prevMonth, nextMonth, byDate, monthTasks, status });
    } catch (e: any) {
      console.error('[tasks] GET /tasks/calendar failed:', e?.message || e, e?.stack || '');
      res.status(502).render('error.njk', { message: 'Failed to load calendar', error: e?.message ?? String(e) });
    }
  });

  // DETAILS
  router.get('/:id', async (req, res) => {
    try {
      const raw = await getTask(req.params.id);
      const [flagged] = withFlags([raw]);
      res.render('tasks/details.njk', { task: flagged, back: req.query.back });
    } catch (e: any) {
      res.status(404).render('error.njk', { message: 'Task not found', error: e?.message ?? String(e) });
    }
  });

  // ACTIONS
  router.post('/:id/start', parseBody, async (req, res) => {
    try { await updateTaskStatus(req.params.id, 'IN_PROGRESS'); res.redirect(`/tasks/${encodeURIComponent(req.params.id)}`); }
    catch (e: any) { console.error('[tasks] start failed', e?.message || e); res.status(400).render('error.njk', { message: 'Could not start task', error: e?.message ?? String(e) }); }
  });

  router.post('/:id/complete', parseBody, async (req, res) => {
    try { await updateTaskStatus(req.params.id, 'DONE'); res.redirect(`/tasks/${encodeURIComponent(req.params.id)}`); }
    catch (e: any) { console.error('[tasks] complete failed', e?.message || e); res.status(400).render('error.njk', { message: 'Could not complete task', error: e?.message ?? String(e) }); }
  });

  router.post('/:id/delete', parseBody, async (req, res) => {
    try { await deleteTask(req.params.id); res.redirect('/tasks'); }
    catch (e: any) { console.error('[tasks] delete failed', e?.message || e); res.status(400).render('error.njk', { message: 'Could not delete task', error: e?.message ?? String(e) }); }
  });

  app.use('/tasks', router);
}
