import { toHaveNoViolations } from 'jest-axe';
import { TextEncoder, TextDecoder } from 'util';

// Polyfill for jsdom environment (needed by supertest -> superagent -> formidable -> cuid2)
if (!(global as any).TextEncoder) {
    (global as any).TextEncoder = TextEncoder;
}
if (!(global as any).TextDecoder) {
    (global as any).TextDecoder = TextDecoder as unknown as { new(): TextDecoder };
}

expect.extend(toHaveNoViolations);
