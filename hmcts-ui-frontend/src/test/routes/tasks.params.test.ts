/**
 * Module-mock axios so tasksApi.ts' axios.create() returns our fake client.
 * Import the service only AFTER mocking.
 */
describe('services/tasksApi: listTasks params & branches', () => {
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

        jest.doMock('axios', () => ({
            __esModule: true,
            default: { create: jest.fn(() => client) },
            create: jest.fn(() => client),
        }));

        api = await import('../../main/services/tasksApi');
    }

    beforeEach(async () => {
        jest.restoreAllMocks();
        await loadFresh();
    });

    it('accepts string arg and cleans empty params', async () => {
        client.get.mockResolvedValueOnce({ data: [] });

        // string signature
        await api.listTasks('OPEN');
        expect(client.get).toHaveBeenCalledWith('/tasks', { params: { status: 'OPEN' } });

        // object signature with empty values that should be dropped
        client.get.mockResolvedValueOnce({ data: [] });

        await api.listTasks({
            status: 'OPEN',
            page: undefined as any,   // dropped
            page_size: null as any,   // dropped
            sort: 'status',
        });

        expect(client.get).toHaveBeenCalledWith('/tasks', {
            params: { status: 'OPEN', sort: 'status' },
        });
    });

    it('handles undefined arg (no filters)', async () => {
        client.get.mockResolvedValueOnce({ data: [] });

        await api.listTasks(undefined);
        expect(client.get).toHaveBeenCalledWith('/tasks', { params: {} });
    });

    it('passes through numeric paging values', async () => {
        client.get.mockResolvedValueOnce({ data: [] });

        await api.listTasks({ page: 2, page_size: 50 });
        expect(client.get).toHaveBeenCalledWith('/tasks', {
            params: { page: 2, page_size: 50 },
        });
    });
});
