import { http, HttpResponse } from 'msw';

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'SHIPPED'
  | 'CANCELLED';

/** Mirrors backend OrderResponse record. */
export interface MockOrderResponse {
  id: number;
  name: string;
  email: string;
  phone: string;
  address: string;
  postalCode: string;
  orderScope: string;
  orderScopeDetail: string;
  receiveDate: string;
  createdAt: string;
  status: OrderStatus;
}

/** Fields accepted by POST /api/orders (matches CreateOrderRequest). */
type CreateBody = Omit<MockOrderResponse, 'id' | 'createdAt' | 'status'>;

// ---------------------------------------------------------------------------
// In-memory store
// ---------------------------------------------------------------------------

function createOrderStore() {
  let nextId = 1;
  let orders: MockOrderResponse[] = [];

  return {
    create(body: CreateBody): MockOrderResponse {
      const order: MockOrderResponse = {
        ...body,
        id: nextId++,
        createdAt: new Date().toISOString(),
        status: 'PENDING',
      };
      orders.push(order);
      return order;
    },
    all: () => [...orders],
    findById: (id: number) => orders.find((o) => o.id === id),
    updateStatus(id: number, status: OrderStatus) {
      const order = orders.find((o) => o.id === id);
      if (order) order.status = status;
      return order;
    },
    reset() {
      nextId = 1;
      orders = [];
    },
  };
}

export const db = createOrderStore();

// ---------------------------------------------------------------------------
// Handlers — mirror Spring Boot OrderController
// ---------------------------------------------------------------------------

export const handlers = [
  http.post('/api/orders', async ({ request }) => {
    const body = (await request.json()) as CreateBody;
    return HttpResponse.json(db.create(body), { status: 201 });
  }),

  http.get('/api/orders', () => HttpResponse.json(db.all())),

  http.get('/api/orders/:id', ({ params }) => {
    const order = db.findById(Number(params['id']));
    return order
      ? HttpResponse.json(order)
      : HttpResponse.json({ error: 'Order not found' }, { status: 404 });
  }),

  http.patch('/api/orders/:id/status', ({ params, request }) => {
    const status = new URL(request.url).searchParams.get('status') as OrderStatus | null;
    const order = status
      ? db.updateStatus(Number(params['id']), status)
      : db.findById(Number(params['id']));
    return order
      ? HttpResponse.json(order)
      : HttpResponse.json({ error: 'Order not found' }, { status: 404 });
  }),
];
