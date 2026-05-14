import React, { ReactElement } from 'react';
import { render, RenderOptions } from '@testing-library/react';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { configureStore } from '@reduxjs/toolkit';

/**
 * Create a minimal test store
 * In production, import actual reducers from your project Redux slices
 * For now, this creates a store with minimal placeholder reducers
 */
export function createTestStore(preloadedState?: any) {
  // Placeholder reducers - Replace with actual imports when setting up real tests
  const placeholderReducer = (state = {}) => state;
  
  return configureStore({
    reducer: {
      auth: placeholderReducer,
      jobs: placeholderReducer,
      applications: placeholderReducer,
      notifications: placeholderReducer,
      ui: placeholderReducer,
    } as any,
    preloadedState,
  });
}


interface ExtendedRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  preloadedState?: any;
  store?: ReturnType<typeof createTestStore>;
}

/**
 * Custom render function that includes providers (Redux, Router, Toast)
 * Use this for testing components that depend on Redux, routing, or toast
 */
export function renderWithProviders(
  ui: ReactElement,
  {
    preloadedState = {},
    store = createTestStore(preloadedState),
    ...renderOptions
  }: ExtendedRenderOptions = {}
) {
  function Wrapper({ children }: { children: React.ReactNode }) {
    return (
      <Provider store={store}>
        <BrowserRouter>
          <>
            {children}
            <Toaster />
          </>
        </BrowserRouter>
      </Provider>
    );
  }

  return { ...render(ui, { wrapper: Wrapper, ...renderOptions }), store };
}

/**
 * Mock API response factory
 * Usage: createMockApiResponse({ data: { id: 1 } })
 */
export function createMockApiResponse<T>(data: T) {
  return Promise.resolve({
    data,
    status: 200,
    statusText: 'OK',
    headers: {},
    config: {} as any,
  });
}

/**
 * Mock API error response factory
 */
export function createMockApiError(message: string, statusCode = 400) {
  return Promise.reject({
    response: {
      data: { message },
      status: statusCode,
      statusText: statusCode === 400 ? 'Bad Request' : 'Error',
      headers: {},
      config: {} as any,
    },
  });
}

// Re-export everything from React Testing Library for convenience
export * from '@testing-library/react';
export { default as userEvent } from '@testing-library/user-event';
