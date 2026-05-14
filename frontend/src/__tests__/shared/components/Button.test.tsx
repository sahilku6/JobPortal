import { describe, it, expect, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/__tests__/utils/test-utils';

/**
 * UI Component Unit Tests Example
 * This demonstrates testing a simple button component
 * Adapt to your actual Button component structure
 */

// Example Button component for reference
const Button = ({
  onClick,
  children,
  disabled = false,
  variant = 'primary',
  className = '',
}: {
  onClick?: () => void;
  children: React.ReactNode;
  disabled?: boolean;
  variant?: 'primary' | 'secondary';
  className?: string;
}) => (
  <button
    onClick={onClick}
    disabled={disabled}
    className={`px-4 py-2 rounded ${variant === 'primary' ? 'bg-blue-500 text-white' : 'bg-gray-200'} ${className}`}
    data-testid="button"
  >
    {children}
  </button>
);

describe('Button Component', () => {
  it('should render with text', () => {
    renderWithProviders(<Button>Click me</Button>);
    
    expect(screen.getByRole('button', { name: /click me/i })).toBeInTheDocument();
  });

  it('should call onClick handler when clicked', async () => {
    const user = userEvent.setup();
    const handleClick = vi.fn();

    renderWithProviders(<Button onClick={handleClick}>Click me</Button>);

    await user.click(screen.getByRole('button', { name: /click me/i }));

    expect(handleClick).toHaveBeenCalledOnce();
  });

  it('should be disabled when disabled prop is true', () => {
    renderWithProviders(<Button disabled>Click me</Button>);

    const button = screen.getByRole('button', { name: /click me/i });
    expect(button).toBeDisabled();
  });

  it('should not call onClick when disabled', async () => {
    const user = userEvent.setup();
    const handleClick = vi.fn();

    renderWithProviders(
      <Button onClick={handleClick} disabled>
        Click me
      </Button>
    );

    await user.click(screen.getByRole('button', { name: /click me/i }));

    expect(handleClick).not.toHaveBeenCalled();
  });

  it('should apply correct variant styles', () => {
    const { rerender } = renderWithProviders(
      <Button variant="primary">Click me</Button>
    );

    let button = screen.getByTestId('button');
    expect(button).toHaveClass('bg-blue-500');

    rerender(<Button variant="secondary">Click me</Button>);

    button = screen.getByTestId('button');
    expect(button).toHaveClass('bg-gray-200');
  });
});
