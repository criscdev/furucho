/**
 * formatWhatsAppMessage — Formats order data into a WhatsApp message string.
 *
 * Takes an {@link OrderFormData} object and returns a URI-encoded string
 * ready to be appended to a `https://wa.me/` URL as the `text` parameter.
 *
 * @param data - The order form data to format.
 * @returns URI-encoded WhatsApp message string.
 *
 * @example
 * const msg = formatWhatsAppMessage(formData);
 * window.open(`https://wa.me/5511999999999?text=${msg}`);
 */
import type { OrderFormData } from './OrderForm';

export function formatWhatsAppMessage(data: OrderFormData): string {
  return encodeURIComponent(
    `🧸 *Nova Encomenda de Boneca*\n\n` +
    `*Nome:* ${data.name}\n` +
    `*Email:* ${data.email}\n` +
    `*Telefone:* ${data.phone}\n` +
    `*Endereço:* ${data.address}\n` +
    `*CEP:* ${data.postalCode}\n\n` +
    `*Resumo:* ${data.orderScope}\n` +
    `*Detalhes:* ${data.orderScopeDetail}\n\n` +
    `*Data desejada:* ${data.receiveDate}`
  );
}
