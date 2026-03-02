import { describe, it, expect } from 'vitest';
import { formatWhatsAppMessage } from './formatWhatsAppMessage';
import type { OrderFormData } from './OrderForm';

/**
 * Tests for formatWhatsAppMessage utility.
 *
 * Verifies that the function produces a URI-encoded WhatsApp message
 * string with all order fields in the expected format.
 */
describe('formatWhatsAppMessage', () => {
  const baseData: OrderFormData = {
    name: 'Maria Silva',
    email: 'maria@example.com',
    phone: '11999887766',
    address: 'Rua das Flores, 123',
    postalCode: '01234567',
    orderScope: 'Boneca personalizada',
    orderScopeDetail: 'Cabelo loiro, vestido azul, 40cm',
    receiveDate: '15/04/2026',
  };

  it('returns a URI-encoded string', () => {
    const result = formatWhatsAppMessage(baseData);

    // Should not contain raw newlines or asterisks (they're encoded)
    expect(result).not.toContain('\n');
    expect(result).toBe(encodeURIComponent(decodeURIComponent(result)));
  });

  it('includes all order fields in the message', () => {
    const result = formatWhatsAppMessage(baseData);
    const decoded = decodeURIComponent(result);

    expect(decoded).toContain('Maria Silva');
    expect(decoded).toContain('maria@example.com');
    expect(decoded).toContain('11999887766');
    expect(decoded).toContain('Rua das Flores, 123');
    expect(decoded).toContain('01234567');
    expect(decoded).toContain('Boneca personalizada');
    expect(decoded).toContain('Cabelo loiro, vestido azul, 40cm');
    expect(decoded).toContain('15/04/2026');
  });

  it('includes labeled sections with bold markers', () => {
    const result = formatWhatsAppMessage(baseData);
    const decoded = decodeURIComponent(result);

    expect(decoded).toContain('*Nome:*');
    expect(decoded).toContain('*Email:*');
    expect(decoded).toContain('*Telefone:*');
    expect(decoded).toContain('*Endereço:*');
    expect(decoded).toContain('*CEP:*');
    expect(decoded).toContain('*Resumo:*');
    expect(decoded).toContain('*Detalhes:*');
    expect(decoded).toContain('*Data desejada:*');
  });

  it('includes the order header with emoji', () => {
    const result = formatWhatsAppMessage(baseData);
    const decoded = decodeURIComponent(result);

    expect(decoded).toContain('🧸');
    expect(decoded).toContain('Nova Encomenda de Boneca');
  });

  it('handles special characters in fields', () => {
    const data: OrderFormData = {
      ...baseData,
      name: 'José & Maria',
      address: 'Av. São Paulo, nº 100 — Apt. 5B',
      orderScopeDetail: 'Cor: rosa/lilás — tamanho "grande"',
    };

    const result = formatWhatsAppMessage(data);
    const decoded = decodeURIComponent(result);

    expect(decoded).toContain('José & Maria');
    expect(decoded).toContain('Av. São Paulo, nº 100 — Apt. 5B');
    expect(decoded).toContain('Cor: rosa/lilás — tamanho "grande"');
  });

  it('handles empty string fields without breaking', () => {
    const emptyData: OrderFormData = {
      name: '',
      email: '',
      phone: '',
      address: '',
      postalCode: '',
      orderScope: '',
      orderScopeDetail: '',
      receiveDate: '',
    };

    const result = formatWhatsAppMessage(emptyData);
    const decoded = decodeURIComponent(result);

    // Should still produce structured message with labels
    expect(decoded).toContain('*Nome:*');
    expect(decoded).toContain('*Email:*');
    expect(decoded).toContain('Nova Encomenda de Boneca');
  });
});
