import { describe, it, expect } from 'vitest';
import { renderHook } from '@testing-library/react';
import { Provider } from 'react-redux';
import { useAppDispatch, useAppSelector } from '@/shared/hooks/redux';
import { createTestStore } from '@/__tests__/utils/test-utils';

/**
 * Redux Hooks Unit Tests
 * Tests custom Redux hooks with proper store context
 */
describe('Redux Hooks', () => {
  describe('useAppDispatch', () => {
    it('should return dispatch function', () => {
      const store = createTestStore();
      
      const wrapper = ({ children }: any) => (
        <Provider store={store}>{children}</Provider>
      );

      const { result } = renderHook(() => useAppDispatch(), {
        wrapper,
      });

      expect(result.current).toBeDefined();
      expect(typeof result.current).toBe('function');
    });
  });

  describe('useAppSelector', () => {
    it('should select state from store', () => {
      const preloadedState = {
        auth: {
          user: {
            id: '1',
            firstName: 'John',
            lastName: 'Doe',
            email: 'john@example.com',
            role: 'JOB_SEEKER' as const,
            phoneNumber: '+1234567890',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString(),
          },
          isAuthenticated: true,
          loading: false,
          error: null,
        },
        jobs: { jobs: [], selectedJob: null, loading: false, error: null },
        applications: {
          applications: [],
          selectedApplication: null,
          loading: false,
          error: null,
        },
        notifications: { notifications: [], unreadCount: 0 },
        ui: { sidebarOpen: true, activeFilters: {} },
      };

      const store = createTestStore(preloadedState as any);

      const wrapper = ({ children }: any) => (
        <Provider store={store}>{children}</Provider>
      );

      const { result } = renderHook(
        () => useAppSelector(state => state.auth.user),
        { wrapper }
      );

      expect(result.current).toEqual(preloadedState.auth.user);
      expect(result.current?.id).toBe('1');
    });
  });
});
