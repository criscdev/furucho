import type { OrderFormData } from '../../component/OrderForm/OrderForm';

/**
 * Factory for creating test order data.
 * 
 * @example
 * // Default data
 * const order = orderFactory();
 * 
 * // With overrides
 * const order = orderFactory({ name: 'Maria Silva' });
 */
export function orderFactory(overrides: Partial<OrderFormData> = {}): OrderFormData {
  // Get a date 30 days in the future to ensure it passes validation
  const futureDate = new Date();
  futureDate.setDate(futureDate.getDate() + 30);
  const isoDate = futureDate.toISOString().split('T')[0]; // Format as YYYY-MM-DD
  
  return {
    name: 'Maria da Silva',
    email: 'maria@exemplo.com',
    phone: '11999998888',
    address: 'Rua das Flores, 123, São Paulo - SP',
    postalCode: '01234-567',
    orderScope: 'Boneca de pano personalizada',
    orderScopeDetail: 'Boneca de aproximadamente 40cm com cabelos castanhos, olhos verdes e vestido azul.',
    receiveDate: '2027-03-15',
    ...overrides,
  };
}
