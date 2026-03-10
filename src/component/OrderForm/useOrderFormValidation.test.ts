import { describe, it, expect } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useOrderFormValidation } from './useOrderFormValidation';
import type { OrderFormData } from './OrderForm';
import { orderFactory } from '../../test/factories/orderFactory';

/**
 * Tests for useOrderFormValidation — extracted validation logic
 * from OrderForm for single-responsibility and testability.
 */
describe('useOrderFormValidation', () => {
  const validData: OrderFormData = orderFactory();

  // ── formData & handleChange ─────────────────────────────────

  it('initialises with empty form data', () => {
    const { result } = renderHook(() => useOrderFormValidation());

    expect(result.current.formData).toEqual({
      name: '',
      email: '',
      phone: '',
      address: '',
      postalCode: '',
      orderScope: '',
      orderScopeDetail: '',
      receiveDate: '',
    });
  });

  it('updates a field via handleChange', () => {
    const { result } = renderHook(() => useOrderFormValidation());

    act(() => {
      result.current.handleChange({
        target: { name: 'name', value: 'Maria' },
      } as React.ChangeEvent<HTMLInputElement>);
    });

    expect(result.current.formData.name).toBe('Maria');
  });

  it('clears the field error when the user types', () => {
    const { result } = renderHook(() => useOrderFormValidation());

    // Trigger validation to produce errors
    act(() => { result.current.validate(); });
    expect(result.current.errors.name).toBeDefined();

    // Type into name field → error should clear
    act(() => {
      result.current.handleChange({
        target: { name: 'name', value: 'A' },
      } as React.ChangeEvent<HTMLInputElement>);
    });

    expect(result.current.errors.name).toBeUndefined();
  });

  // ── validate() — required fields ───────────────────────────

  it('returns errors for all empty required fields', () => {
    const { result } = renderHook(() => useOrderFormValidation());

    let errors!: ReturnType<typeof result.current.validate>;
    act(() => { errors = result.current.validate(); });

    expect(errors.name).toBe('Nome é obrigatório');
    expect(errors.email).toBe('Email é obrigatório');
    expect(errors.phone).toBe('Telefone é obrigatório');
    expect(errors.address).toBe('Endereço é obrigatório');
    expect(errors.postalCode).toBe('CEP é obrigatório');
    expect(errors.orderScope).toBe('Tipo de boneca é obrigatório');
    expect(errors.orderScopeDetail).toBe('Detalhes do pedido são obrigatórios');
    expect(errors.receiveDate).toBe('Data de entrega é obrigatória');
  });

  it('returns no errors for valid data', () => {
    const { result } = renderHook(() => useOrderFormValidation());

    // Fill all fields with valid data
    Object.entries(validData).forEach(([key, value]) => {
      act(() => {
        result.current.handleChange({
          target: { name: key, value },
        } as React.ChangeEvent<HTMLInputElement>);
      });
    });

    let errors!: ReturnType<typeof result.current.validate>;
    act(() => { errors = result.current.validate(); });

    expect(Object.keys(errors)).toHaveLength(0);
  });

  // ── validate() — format rules ──────────────────────────────

  it('rejects name longer than 200 characters', () => {
    const { result } = renderHook(() => useOrderFormValidation());

    act(() => {
      result.current.handleChange({
        target: { name: 'name', value: 'A'.repeat(201) },
      } as React.ChangeEvent<HTMLInputElement>);
    });

    let errors!: ReturnType<typeof result.current.validate>;
    act(() => { errors = result.current.validate(); });

    expect(errors.name).toBe('Nome deve ter no máximo 200 caracteres');
  });

  it('rejects invalid email format', () => {
    const { result } = renderHook(() => useOrderFormValidation());

    act(() => {
      result.current.handleChange({
        target: { name: 'email', value: 'not-an-email' },
      } as React.ChangeEvent<HTMLInputElement>);
    });

    let errors!: ReturnType<typeof result.current.validate>;
    act(() => { errors = result.current.validate(); });

    expect(errors.email).toBe('Email inválido');
  });

  it('rejects phone with fewer than 10 digits', () => {
    const { result } = renderHook(() => useOrderFormValidation());

    act(() => {
      result.current.handleChange({
        target: { name: 'phone', value: '123456789' },
      } as React.ChangeEvent<HTMLInputElement>);
    });

    let errors!: ReturnType<typeof result.current.validate>;
    act(() => { errors = result.current.validate(); });

    expect(errors.phone).toBe('Telefone deve ter 10 ou 11 dígitos');
  });

  it('accepts phone with 10 digits', () => {
    const { result } = renderHook(() => useOrderFormValidation());

    act(() => {
      result.current.handleChange({
        target: { name: 'phone', value: '1199999888' },
      } as React.ChangeEvent<HTMLInputElement>);
    });

    let errors!: ReturnType<typeof result.current.validate>;
    act(() => { errors = result.current.validate(); });

    expect(errors.phone).toBeUndefined();
  });

  it('accepts phone with 11 digits', () => {
    const { result } = renderHook(() => useOrderFormValidation());

    act(() => {
      result.current.handleChange({
        target: { name: 'phone', value: '11999998888' },
      } as React.ChangeEvent<HTMLInputElement>);
    });

    let errors!: ReturnType<typeof result.current.validate>;
    act(() => { errors = result.current.validate(); });

    expect(errors.phone).toBeUndefined();
  });

  it('rejects phone with more than 11 digits', () => {
    const { result } = renderHook(() => useOrderFormValidation());

    act(() => {
      result.current.handleChange({
        target: { name: 'phone', value: '119999988881' },
      } as React.ChangeEvent<HTMLInputElement>);
    });

    let errors!: ReturnType<typeof result.current.validate>;
    act(() => { errors = result.current.validate(); });

    expect(errors.phone).toBe('Telefone deve ter 10 ou 11 dígitos');
  });

  it('rejects invalid CEP format', () => {
    const { result } = renderHook(() => useOrderFormValidation());

    act(() => {
      result.current.handleChange({
        target: { name: 'postalCode', value: '1234' },
      } as React.ChangeEvent<HTMLInputElement>);
    });

    let errors!: ReturnType<typeof result.current.validate>;
    act(() => { errors = result.current.validate(); });

    expect(errors.postalCode).toBe('CEP inválido (formato: 00000-000)');
  });

  it('accepts CEP with hyphen (00000-000)', () => {
    const { result } = renderHook(() => useOrderFormValidation());

    act(() => {
      result.current.handleChange({
        target: { name: 'postalCode', value: '01234-567' },
      } as React.ChangeEvent<HTMLInputElement>);
    });

    let errors!: ReturnType<typeof result.current.validate>;
    act(() => { errors = result.current.validate(); });

    expect(errors.postalCode).toBeUndefined();
  });

  it('accepts CEP without hyphen (00000000)', () => {
    const { result } = renderHook(() => useOrderFormValidation());

    act(() => {
      result.current.handleChange({
        target: { name: 'postalCode', value: '01234567' },
      } as React.ChangeEvent<HTMLInputElement>);
    });

    let errors!: ReturnType<typeof result.current.validate>;
    act(() => { errors = result.current.validate(); });

    expect(errors.postalCode).toBeUndefined();
  });

  it('rejects invalid date format', () => {
    const { result } = renderHook(() => useOrderFormValidation());

    act(() => {
      result.current.handleChange({
        target: { name: 'receiveDate', value: '2026-12-25' },
      } as React.ChangeEvent<HTMLInputElement>);
    });

    let errors!: ReturnType<typeof result.current.validate>;
    act(() => { errors = result.current.validate(); });

    expect(errors.receiveDate).toBe('Data inválida (formato: DD/MM/AAAA)');
  });

  it('rejects impossible date values like 99/99/9999', () => {
    const { result } = renderHook(() => useOrderFormValidation());

    act(() => {
      result.current.handleChange({
        target: { name: 'receiveDate', value: '99/99/9999' },
      } as React.ChangeEvent<HTMLInputElement>);
    });

    let errors!: ReturnType<typeof result.current.validate>;
    act(() => { errors = result.current.validate(); });

    expect(errors.receiveDate).toBe('Data inválida');
  });

  it('rejects 31/02/2026 (Feb 31 does not exist)', () => {
    const { result } = renderHook(() => useOrderFormValidation());

    act(() => {
      result.current.handleChange({
        target: { name: 'receiveDate', value: '31/02/2026' },
      } as React.ChangeEvent<HTMLInputElement>);
    });

    let errors!: ReturnType<typeof result.current.validate>;
    act(() => { errors = result.current.validate(); });

    expect(errors.receiveDate).toBe('Data inválida');
  });

  it('accepts valid DD/MM/AAAA date', () => {
    const { result } = renderHook(() => useOrderFormValidation());

    act(() => {
      result.current.handleChange({
        target: { name: 'receiveDate', value: '25/12/2026' },
      } as React.ChangeEvent<HTMLInputElement>);
    });

    let errors!: ReturnType<typeof result.current.validate>;
    act(() => { errors = result.current.validate(); });

    expect(errors.receiveDate).toBeUndefined();
  });

  // ── reset ──────────────────────────────────────────────────

  it('resets form data and errors', () => {
    const { result } = renderHook(() => useOrderFormValidation());

    // Dirty the form
    act(() => {
      result.current.handleChange({
        target: { name: 'name', value: 'Maria' },
      } as React.ChangeEvent<HTMLInputElement>);
    });
    act(() => { result.current.validate(); });

    // Reset
    act(() => { result.current.reset(); });

    expect(result.current.formData.name).toBe('');
    expect(result.current.errors).toEqual({});
  });
});
