process.env.NODE_ENV = 'test';

// Silence the "Loaded route files" spam (and any other console.log during tests)
beforeAll(() => {
    jest.spyOn(console, 'log').mockImplementation(() => { });
});
afterAll(() => {
    (console.log as any).mockRestore?.();
});
