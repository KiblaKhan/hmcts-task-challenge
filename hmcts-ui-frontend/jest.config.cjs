/** @type {import('jest').Config} */
module.exports = {
  // If you're on ts-jest:
  preset: 'ts-jest',
  testEnvironment: 'node',
  setupFilesAfterEnv: ['<rootDir>/src/test/jest.setup.ts'],

  // Your tests are under src/test
  roots: ['<rootDir>/src/test'],
  testMatch: ['**/*.test.ts'],

  // point to the actual config.ts
  setupFiles: ['<rootDir>/src/test/config.ts'],

  // load expect matchers here
  setupFilesAfterEnv: ['<rootDir>/src/test/a11y/axe-setup.ts'],

  // If you use path aliases, keep these:
  moduleNameMapper: {
    '^services/(.*)$': '<rootDir>/src/main/services/$1',
    '^routes/(.*)$': '<rootDir>/src/main/routes/$1',
    '^modules/(.*)$': '<rootDir>/src/main/modules/$1',
  },
  collectCoverage: true,
  collectCoverageFrom: [
    'src/main/**/*.ts',
    '!src/main/server.ts',           // skip entrypoint
    '!src/main/development.ts',      // skip dev-only
    '!src/main/**/__mocks__/**'
  ],
  coverageThreshold: {
    global: { branches: 60, functions: 80, lines: 85, statements: 85 },
  },
};

