/**
 * Test Data Factories
 * Create realistic mock data for tests
 */

export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: 'JOB_SEEKER' | 'RECRUITER' | 'ADMIN';
  phoneNumber: string;
  createdAt: string;
  updatedAt: string;
}

export interface Job {
  id: string;
  title: string;
  company: string;
  location: string;
  salary: string;
  jobType: string;
  description: string;
  applicants: number;
  createdAt: string;
}

export interface JobApplication {
  id: string;
  jobId: string;
  applicantId: string;
  status: 'APPLIED' | 'SHORTLISTED' | 'REJECTED' | 'ACCEPTED';
  appliedAt: string;
}

/**
 * User Factory
 */
export function createMockUser(overrides: Partial<User> = {}): User {
  return {
    id: '1',
    firstName: 'John',
    lastName: 'Doe',
    email: 'john@example.com',
    role: 'JOB_SEEKER',
    phoneNumber: '+1234567890',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    ...overrides,
  };
}

/**
 * Job Factory
 */
export function createMockJob(overrides: Partial<Job> = {}): Job {
  return {
    id: '1',
    title: 'Senior Software Engineer',
    company: 'Tech Corp',
    location: 'Remote',
    salary: '120000-150000',
    jobType: 'Full-time',
    description: 'We are looking for a Senior Software Engineer...',
    applicants: 25,
    createdAt: new Date().toISOString(),
    ...overrides,
  };
}

/**
 * Job Application Factory
 */
export function createMockJobApplication(
  overrides: Partial<JobApplication> = {}
): JobApplication {
  return {
    id: '1',
    jobId: '1',
    applicantId: '1',
    status: 'APPLIED',
    appliedAt: new Date().toISOString(),
    ...overrides,
  };
}

/**
 * Create multiple mock items
 */
export function createMockUsers(count: number): User[] {
  return Array.from({ length: count }, (_, i) =>
    createMockUser({
      id: String(i + 1),
      email: `user${i + 1}@example.com`,
      firstName: `User${i + 1}`,
    })
  );
}

export function createMockJobs(count: number): Job[] {
  return Array.from({ length: count }, (_, i) =>
    createMockJob({
      id: String(i + 1),
      title: `Job ${i + 1}`,
      company: `Company ${i + 1}`,
    })
  );
}

/**
 * Auth State Fixtures
 */
export const authStateFixtures = {
  unauthenticated: {
    currentUser: null,
    isAuthenticated: false,
    accessToken: null,
    refreshToken: null,
    loading: false,
    error: null,
  },
  authenticated: {
    currentUser: createMockUser(),
    isAuthenticated: true,
    accessToken: 'mock-access-token',
    refreshToken: 'mock-refresh-token',
    loading: false,
    error: null,
  },
  loading: {
    currentUser: null,
    isAuthenticated: false,
    accessToken: null,
    refreshToken: null,
    loading: true,
    error: null,
  },
  error: {
    currentUser: null,
    isAuthenticated: false,
    accessToken: null,
    refreshToken: null,
    loading: false,
    error: 'Authentication failed',
  },
};

/**
 * Jobs State Fixtures
 */
export const jobsStateFixtures = {
  empty: {
    jobs: [],
    selectedJob: null,
    loading: false,
    error: null,
  },
  withJobs: {
    jobs: createMockJobs(5),
    selectedJob: createMockJob(),
    loading: false,
    error: null,
  },
  loading: {
    jobs: [],
    selectedJob: null,
    loading: true,
    error: null,
  },
};
