import express from 'express';
import { Nunjucks } from '../../main/modules/nunjucks';

const logSpy = jest.spyOn(console, 'log').mockImplementation(() => { });
const errSpy = jest.spyOn(console, 'error').mockImplementation(() => { });
const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => { });
afterAll(() => { logSpy.mockRestore(); errSpy.mockRestore(); warnSpy.mockRestore(); });

describe('Nunjucks bootstrap', () => {
  it('enables nunjucks in dev and prod modes without throwing', () => {
    const app1 = express();
    expect(() => new Nunjucks(true).enableFor(app1)).not.toThrow();
    const app2 = express();
    expect(() => new Nunjucks(false).enableFor(app2)).not.toThrow();
    expect(app1.get('view engine')).toBe('njk');
    expect(app2.get('view engine')).toBe('njk');
  });
});
