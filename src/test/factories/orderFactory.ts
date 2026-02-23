import type { OrderFormData } from '../../component/OrderForm/OrderForm';

/** Returns a date string 90 days in the future (DD/MM/YYYY). */
function futureDate(): string {
  const d = new Date();
  d.setDate(d.getDate() + 90);
  const dd = String(d.getDate()).padStart(2, '0');
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  return `${dd}/${mm}/${d.getFullYear()}`;
}

/**
 * Factory for creating test order data.
 *
 * @example
 * orderFactory()                          // defaults
 * orderFactory({ name: 'Maria Silva' })   // with overrides
 */
export function orderFactory(overrides: Partial<OrderFormData> = {}): OrderFormData {
  return {
    name: 'Maria da Silva',
    email: 'maria@exemplo.com',
    phone: '11999998888',
    address: 'Rua das Flores, 123, São Paulo - SP',
    postalCode: '01234-567',
    orderScope: 'Boneca de pano personalizada',
    orderScopeDetail: 'Boneca de aproximadamente 40cm com cabelos castanhos, olhos verdes e vestido azul.',
    receiveDate: futureDate(),
    ...overrides,
  };
}
