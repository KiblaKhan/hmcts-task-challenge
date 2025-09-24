export function muteConsole(levels: (keyof Console)[] = ['log', 'warn', 'error']) {
    const restores = levels.map(l => jest.spyOn(console, l).mockImplementation(() => { }));
    return () => restores.forEach(r => r.mockRestore());
}
// in a test file
const unmute = muteConsole();
afterAll(unmute);