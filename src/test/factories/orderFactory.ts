// @ts-nocheck
export function orderFactory(overrides = {}) {
  return {
    name: 'Test Name',
    email: 'test@example.com',
    phone: '12345678901',
    address: '123 Test St',
    postalCode: '12345',
    orderScope: 'Produto A',
    orderScopeDetail: 'Detalhes do pedido',
    receiveDate: '2025-10-01',
    ...overrides,
  };
}
