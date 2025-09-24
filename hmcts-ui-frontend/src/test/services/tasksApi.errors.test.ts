/**
 * Mocks axios at the module level so tasksApi.ts's `axios.create(...)`
 * returns our fake client with jest fns.
 */
describe('tasksApi: env + error mapping', () => {
    let api: typeof import('../../main/services/tasksApi');
    let client: {
        get: jest.Mock;
        post: jest.Mock;
        put: jest.Mock;
        patch: jest.Mock;
        delete: jest.Mock;
    };

    async function loadFresh() {
        jest.resetModules();

        client = {
            get: jest.fn(),
            post: jest.fn(),
            put: jest.fn(),
            patch: jest.fn(),
            delete: jest.fn(),
        };

        // Mock the entire axios module (default export + named create)
        jest.doMock('axios', () => ({
            __esModule: true,
            default: { create: jest.fn(() => client) },
            create: jest.fn(() => client),
        }));

        // Import AFTER mocking so tasksApi picks up our mocked axios.create()
        api = await import('../../main/services/tasksApi');
    }

    beforeEach(async () => {
        jest.restoreAllMocks();
        await loadFresh();
    });

    it('updateTaskStatus rethrows response errors with message', async () => {
        // Simulate server 400 with message "bad"
        client.put.mockRejectedValueOnce({ response: { status: 400, data: { message: 'bad' } } });
        // Ensure we don't accidentally succeed on fallback
        client.patch.mockRejectedValueOnce(new Error('nope'));

        await expect(api.updateTaskStatus('abc', 'OPEN' as any)).rejects.toThrow(/bad/i);
    });

    it('updateTaskStatus propagates network errors (no response)', async () => {
        client.put.mockRejectedValueOnce(new Error('Network Error'));
        await expect(api.updateTaskStatus('abc', 'OPEN' as any)).rejects.toThrow('Network Error');
    });

    it('falls back to PATCH when PUT is 405/404/400', async () => {
        client.put.mockRejectedValueOnce({ response: { status: 405 } });
        client.patch.mockResolvedValueOnce({ data: { id: 'abc', title: 't', status: 'IN_PROGRESS' } });

        const res = await api.updateTaskStatus('abc', 'IN_PROGRESS' as any);
        expect(res.id).toBe('abc');
        expect(client.patch).toHaveBeenCalledWith('/tasks/abc', { status: 'IN_PROGRESS' });
    });

    it('falls back to POST /complete when PUT fails and PATCH fails', async () => {
        client.put.mockRejectedValueOnce({ response: { status: 404 } });
        client.patch.mockRejectedValueOnce(new Error('nope'));
        client.post.mockResolvedValueOnce({ data: { id: 'abc', title: 't', status: 'DONE' } });

        const res = await api.updateTaskStatus('abc', 'DONE' as any);
        expect(res.status).toBe('DONE');
        expect(client.post).toHaveBeenCalledWith('/tasks/abc/complete');
    });

    it('falls back to POST /start when PUT fails and PATCH fails', async () => {
        // Use the same api/client initialized in beforeEach
        client.put.mockRejectedValueOnce({ response: { status: 404 } });
        client.patch.mockRejectedValueOnce(new Error('nope'));
        client.post.mockResolvedValueOnce({ data: { id: 'abc', title: 't', status: 'IN_PROGRESS' } } as any);

        const res = await api.updateTaskStatus('abc', 'IN_PROGRESS' as any);
        expect(res.status).toBe('IN_PROGRESS');
        expect(client.post).toHaveBeenCalledWith('/tasks/abc/start');
    });
});
