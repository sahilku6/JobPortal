/**
 * ESLint Configuration for Testing
 * 
 * This file extends your ESLint config with testing-specific rules
 * Merge this into your .eslintrc.js or .eslintrc.json
 */

module.exports = {
  // This goes in the "overrides" section of your main ESLint config
  overrides: [
    {
      files: ['src/**/*.{test,spec}.{ts,tsx}'],
      env: {
        jest: true,
        vitest: true,
      },
      extends: ['plugin:vitest/recommended'],
      rules: {
        // Vitest best practices
        'vitest/no-disabled-tests': 'warn',
        'vitest/no-focused-tests': 'error',
        'vitest/no-identical-title': 'error',
        'vitest/prefer-to-be': 'warn',
        'vitest/prefer-to-have-length': 'warn',
        
        // Testing library
        'testing-library/prefer-screen-queries': 'warn',
        'testing-library/no-wait-for-empty-callback': 'error',
        'testing-library/no-unnecessary-act': 'warn',
        
        // General rules
        '@typescript-eslint/no-explicit-any': 'off',
        'no-undef': 'off', // Handled by vitest globals
      },
    },
  ],
};
