import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe, toHaveNoViolations } from 'jest-axe';
import { OrderForm } from './OrderForm';
import { orderFactory } from '../../test/factories/orderFactory';

expect.extend(toHaveNoViolations);

describe('OrderForm', () => {
  it('renders the form with all labeled input fields', () => {
    render(<OrderForm />);

    expect(screen.getByRole('heading', { name: /faça sua encomenda/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/nome completo/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/telefone/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/endereço completo/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/cep/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/tipo de boneca desejada/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/detalhes da boneca/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/data desejada para receber/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /enviar pelo whatsapp/i })).toBeInTheDocument();
  });

  it('marks required fields with aria-required', () => {
    render(<OrderForm />);

    expect(screen.getByLabelText(/nome completo/i)).toHaveAttribute('aria-required', 'true');
    expect(screen.getByLabelText(/^email/i)).toHaveAttribute('aria-required', 'true');
    expect(screen.getByLabelText(/telefone/i)).toHaveAttribute('aria-required', 'true');
  });

  it('shows validation errors for empty required fields on submit', async () => {
    const user = userEvent.setup();
    render(<OrderForm />);

    await user.click(screen.getByRole('button', { name: /enviar pelo whatsapp/i }));

    expect(await screen.findByText(/nome é obrigatório/i)).toBeInTheDocument();
    expect(screen.getByText(/email é obrigatório/i)).toBeInTheDocument();
    expect(screen.getByText(/telefone é obrigatório/i)).toBeInTheDocument();
  });

  it('shows email validation error for invalid email', async () => {
    const user = userEvent.setup();
    render(<OrderForm />);

    await user.type(screen.getByLabelText(/^email/i), 'invalid-email');
    await user.click(screen.getByRole('button', { name: /enviar pelo whatsapp/i }));

    expect(await screen.findByText(/email inválido/i)).toBeInTheDocument();
  });

  it('clears error when user starts typing in a field', async () => {
    const user = userEvent.setup();
    render(<OrderForm />);

    await user.click(screen.getByRole('button', { name: /enviar pelo whatsapp/i }));
    expect(await screen.findByText(/nome é obrigatório/i)).toBeInTheDocument();

    await user.type(screen.getByLabelText(/nome completo/i), 'Maria');
    expect(screen.queryByText(/nome é obrigatório/i)).not.toBeInTheDocument();
  });

  it('opens WhatsApp with formatted message on valid submission', async () => {
    const user = userEvent.setup();
    const mockOpen = vi.spyOn(window, 'open').mockImplementation(() => null);
    const mockCallback = vi.fn();

    render(<OrderForm onSubmitSuccess={mockCallback} />);

    const orderData = orderFactory();

    await user.type(screen.getByLabelText(/nome completo/i), orderData.name);
    await user.type(screen.getByLabelText(/^email/i), orderData.email);
    await user.type(screen.getByLabelText(/telefone/i), orderData.phone);
    await user.type(screen.getByLabelText(/endereço completo/i), orderData.address);
    await user.type(screen.getByLabelText(/cep/i), orderData.postalCode);
    await user.type(screen.getByLabelText(/tipo de boneca desejada/i), orderData.orderScope);
    await user.type(screen.getByLabelText(/detalhes da boneca/i), orderData.orderScopeDetail);
    await user.type(screen.getByLabelText(/data desejada para receber/i), orderData.receiveDate);

    await user.click(screen.getByRole('button', { name: /enviar pelo whatsapp/i }));

    await waitFor(() => {
      expect(mockOpen).toHaveBeenCalledWith(
        expect.stringContaining('wa.me'),
        '_blank',
        'noopener,noreferrer'
      );
    });

    expect(mockCallback).toHaveBeenCalledWith(orderData);
    expect(await screen.findByText(/redirecionando para o whatsapp/i)).toBeInTheDocument();

    mockOpen.mockRestore();
  });

  it('has accessible error messages linked via aria-describedby', async () => {
    const user = userEvent.setup();
    render(<OrderForm />);

    await user.click(screen.getByRole('button', { name: /enviar pelo whatsapp/i }));

    await waitFor(() => {
      const nameInput = screen.getByLabelText(/nome completo/i);
      expect(nameInput).toHaveAttribute('aria-invalid', 'true');
      expect(nameInput).toHaveAttribute('aria-describedby', 'name-error');
    });
  });

  it('has no accessibility violations', async () => {
    const { container } = render(<OrderForm />);
    const results = await axe(container);
    
    expect(results).toHaveNoViolations();
  });

  it('focuses the first invalid field on empty submit', async () => {
    const user = userEvent.setup();
    render(<OrderForm />);

    await user.click(screen.getByRole('button', { name: /enviar pelo whatsapp/i }));

    await waitFor(() => {
      expect(screen.getByLabelText(/nome completo/i)).toHaveFocus();
    });
  });
});
