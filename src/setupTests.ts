import '@testing-library/jest-dom';
import { beforeAll, afterEach, afterAll } from 'vitest';
import { server } from './mocks/server';
import { db } from './mocks/handlers';

// MSW lifecycle — `onUnhandledRequest: 'warn'` keeps existing tests green.
beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }));
afterEach(() => { server.resetHandlers(); db.reset(); });
afterAll(() => server.close());
