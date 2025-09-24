// Jest test bootstrap
process.env.NODE_ENV = process.env.NODE_ENV || 'test';
process.env.TASKS_API_URL = process.env.TASKS_API_URL || 'http://localhost:8080';
// GOV.UK frontend JS is not needed for SSR route tests; keep environment quiet.
