/**
 * OrderForm — Doll order request form with WhatsApp integration.
 *
 * Collects customer information and order details, then opens
 * WhatsApp with a pre-formatted message for easy ordering.
 *
 * A11y:
 * - All inputs have visible labels with htmlFor/id linking
 * - Error messages linked via aria-describedby
 * - Required fields marked with aria-required
 * - Success/error feedback via aria-live region
 * - Keyboard operable (Tab navigation, Enter submit)
 *
 * @example
 * <OrderForm whatsappNumber="5511999999999" />
 */

import { useState, type FormEvent, type ChangeEvent } from "react";

export interface OrderFormData {
  name: string;
  email: string;
  phone: string;
  address: string;
  postalCode: string;
  orderScope: string;
  orderScopeDetail: string;
  receiveDate: string;
}

export interface OrderFormProps {
  /** WhatsApp number with country code (no + or spaces) */
  whatsappNumber?: string;
  /** Callback after successful form submission */
  onSubmitSuccess?: (data: OrderFormData) => void;
}

interface FormErrors {
  name?: string;
  email?: string;
  phone?: string;
  address?: string;
  postalCode?: string;
  orderScope?: string;
  orderScopeDetail?: string;
  receiveDate?: string;
}

const initialFormData: OrderFormData = {
  name: "",
  email: "",
  phone: "",
  address: "",
  postalCode: "",
  orderScope: "",
  orderScopeDetail: "",
  receiveDate: "",
};

export function OrderForm({ 
  whatsappNumber = "5511999999999",
  onSubmitSuccess 
}: OrderFormProps) {
  const [formData, setFormData] = useState<OrderFormData>(initialFormData);
  const [errors, setErrors] = useState<FormErrors>({});
  const [submitStatus, setSubmitStatus] = useState<"idle" | "success" | "error">("idle");

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    // Clear error when user starts typing
    if (errors[name as keyof FormErrors]) {
      setErrors(prev => ({ ...prev, [name]: undefined }));
    }
  };

  const validateForm = (): FormErrors => {
    const newErrors: FormErrors = {};

    if (!formData.name.trim()) {
      newErrors.name = "Nome é obrigatório";
    } else if (formData.name.length > 200) {
      newErrors.name = "Nome deve ter no máximo 200 caracteres";
    }

    if (!formData.email.trim()) {
      newErrors.email = "Email é obrigatório";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = "Email inválido";
    }

    if (!formData.phone.trim()) {
      newErrors.phone = "Telefone é obrigatório";
    } else if (!/^\d{10,11}$/.test(formData.phone.replace(/\D/g, ""))) {
      newErrors.phone = "Telefone deve ter 10 ou 11 dígitos";
    }

    if (!formData.address.trim()) {
      newErrors.address = "Endereço é obrigatório";
    }

    if (!formData.postalCode.trim()) {
      newErrors.postalCode = "CEP é obrigatório";
    } else if (!/^\d{5}-?\d{3}$/.test(formData.postalCode)) {
      newErrors.postalCode = "CEP inválido (formato: 00000-000)";
    }

    if (!formData.orderScope.trim()) {
      newErrors.orderScope = "Resumo do pedido é obrigatório";
    }

    if (!formData.orderScopeDetail.trim()) {
      newErrors.orderScopeDetail = "Detalhes do pedido são obrigatórios";
    }

    if (!formData.receiveDate.trim()) {
      newErrors.receiveDate = "Data de entrega é obrigatória";
    } else if (!/^\d{4}-\d{2}-\d{2}$/.test(formData.receiveDate)) {
      newErrors.receiveDate = "Data inválida (formato: AAAA-MM-DD)";
    } else {
      // Validate that the date is in the future
      const selectedDate = new Date(formData.receiveDate);
      selectedDate.setHours(0, 0, 0, 0);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      if (selectedDate < today) {
        newErrors.receiveDate = "Data deve ser no futuro";
      }
    }

    setErrors(newErrors);
    return newErrors;
  };

  const formatWhatsAppMessage = (): string => {
    // Format date from ISO (YYYY-MM-DD) to Brazilian format (DD/MM/YYYY) for display
    const formatDate = (isoDate: string): string => {
      const parts = isoDate.split('-');
      if (parts.length !== 3) {
        return isoDate; // Return as-is if format is unexpected
      }
      const [year, month, day] = parts;
      return `${day}/${month}/${year}`;
    };

    return encodeURIComponent(
      `🧸 *Nova Encomenda de Boneca*\n\n` +
      `*Nome:* ${formData.name}\n` +
      `*Email:* ${formData.email}\n` +
      `*Telefone:* ${formData.phone}\n` +
      `*Endereço:* ${formData.address}\n` +
      `*CEP:* ${formData.postalCode}\n\n` +
      `*Resumo:* ${formData.orderScope}\n` +
      `*Detalhes:* ${formData.orderScopeDetail}\n\n` +
      `*Data desejada:* ${formatDate(formData.receiveDate)}`
    );
  };

  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setSubmitStatus("idle");

    const validationErrors = validateForm();
    if (Object.keys(validationErrors).length > 0) {
      // Focus first error field
      const firstErrorField = Object.keys(validationErrors)[0];
      if (firstErrorField) {
        const element = document.getElementById(firstErrorField);
        element?.focus();
      }
      return;
    }

    try {
      // Normalize data before sending to backend to match backend validation
      const normalizedData = {
        ...formData,
        phone: formData.phone.replace(/\D/g, ""),
        postalCode: formData.postalCode.replace(/-/g, "")
      };

      const whatsappUrl = `https://wa.me/${whatsappNumber}?text=${formatWhatsAppMessage()}`;
      window.open(whatsappUrl, "_blank", "noopener,noreferrer");
      setSubmitStatus("success");
      onSubmitSuccess?.(normalizedData);
    } catch {
      setSubmitStatus("error");
    }
  };

  return (
    <section 
      id="order-form" 
      className="max-w-2xl mx-auto px-4 py-12"
      aria-labelledby="order-form-heading"
      tabIndex={-1}
    >
      <div className="card">
        <h2 
          id="order-form-heading"
          className="text-2xl font-bold mb-6 text-center"
          style={{ color: 'var(--color-text-heading)' }}
        >
          Faça sua Encomenda
        </h2>

        {/* Status message - aria-live for screen readers */}
        <div 
          aria-live="polite" 
          aria-atomic="true"
          className="mb-4"
        >
          {submitStatus === "success" && (
            <div 
              className="p-4 rounded-md text-center"
              style={{ backgroundColor: 'var(--color-mint)', color: 'var(--color-text-heading)' }}
              role="status"
            >
              ✓ Redirecionando para o WhatsApp... Obrigada pelo interesse!
            </div>
          )}
          {submitStatus === "error" && (
            <div 
              className="p-4 rounded-md text-center"
              style={{ backgroundColor: '#FECACA', color: '#991B1B' }}
              role="alert"
            >
              Erro ao processar. Por favor, tente novamente.
            </div>
          )}
        </div>

        <form onSubmit={handleSubmit} noValidate>
          <div className="space-y-4">
            {/* Name */}
            <div>
              <label htmlFor="name" className="form-label">
                Nome completo <span aria-hidden="true">*</span>
              </label>
              <input
                type="text"
                id="name"
                name="name"
                value={formData.name}
                onChange={handleChange}
                className="form-input"
                aria-required="true"
                aria-invalid={!!errors.name}
                aria-describedby={errors.name ? "name-error" : undefined}
                maxLength={200}
              />
              {errors.name && (
                <span id="name-error" className="form-error" role="alert">
                  {errors.name}
                </span>
              )}
            </div>

            {/* Email */}
            <div>
              <label htmlFor="email" className="form-label">
                Email <span aria-hidden="true">*</span>
              </label>
              <input
                type="email"
                id="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                className="form-input"
                aria-required="true"
                aria-invalid={!!errors.email}
                aria-describedby={errors.email ? "email-error" : undefined}
                maxLength={100}
              />
              {errors.email && (
                <span id="email-error" className="form-error" role="alert">
                  {errors.email}
                </span>
              )}
            </div>

            {/* Phone */}
            <div>
              <label htmlFor="phone" className="form-label">
                Telefone <span aria-hidden="true">*</span>
              </label>
              <input
                type="tel"
                id="phone"
                name="phone"
                value={formData.phone}
                onChange={handleChange}
                className="form-input"
                aria-required="true"
                aria-invalid={!!errors.phone}
                aria-describedby={errors.phone ? "phone-error" : undefined}
                placeholder="11999999999"
                maxLength={15}
              />
              {errors.phone && (
                <span id="phone-error" className="form-error" role="alert">
                  {errors.phone}
                </span>
              )}
            </div>

            {/* Address */}
            <div>
              <label htmlFor="address" className="form-label">
                Endereço completo <span aria-hidden="true">*</span>
              </label>
              <input
                type="text"
                id="address"
                name="address"
                value={formData.address}
                onChange={handleChange}
                className="form-input"
                aria-required="true"
                aria-invalid={!!errors.address}
                aria-describedby={errors.address ? "address-error" : undefined}
                maxLength={200}
              />
              {errors.address && (
                <span id="address-error" className="form-error" role="alert">
                  {errors.address}
                </span>
              )}
            </div>

            {/* Postal Code */}
            <div>
              <label htmlFor="postalCode" className="form-label">
                CEP <span aria-hidden="true">*</span>
              </label>
              <input
                type="text"
                id="postalCode"
                name="postalCode"
                value={formData.postalCode}
                onChange={handleChange}
                className="form-input"
                aria-required="true"
                aria-invalid={!!errors.postalCode}
                aria-describedby={errors.postalCode ? "postalCode-error" : undefined}
                placeholder="00000-000"
                maxLength={10}
              />
              {errors.postalCode && (
                <span id="postalCode-error" className="form-error" role="alert">
                  {errors.postalCode}
                </span>
              )}
            </div>

            {/* Order Scope */}
            <div>
              <label htmlFor="orderScope" className="form-label">
                Tipo de boneca desejada <span aria-hidden="true">*</span>
              </label>
              <input
                type="text"
                id="orderScope"
                name="orderScope"
                value={formData.orderScope}
                onChange={handleChange}
                className="form-input"
                aria-required="true"
                aria-invalid={!!errors.orderScope}
                aria-describedby={errors.orderScope ? "orderScope-error" : undefined}
                placeholder="Ex: Boneca de pano tradicional, Amigurumi..."
                maxLength={100}
              />
              {errors.orderScope && (
                <span id="orderScope-error" className="form-error" role="alert">
                  {errors.orderScope}
                </span>
              )}
            </div>

            {/* Order Details */}
            <div>
              <label htmlFor="orderScopeDetail" className="form-label">
                Detalhes da boneca <span aria-hidden="true">*</span>
              </label>
              <textarea
                id="orderScopeDetail"
                name="orderScopeDetail"
                value={formData.orderScopeDetail}
                onChange={handleChange}
                className="form-input min-h-[120px]"
                aria-required="true"
                aria-invalid={!!errors.orderScopeDetail}
                aria-describedby={errors.orderScopeDetail ? "orderScopeDetail-error" : undefined}
                placeholder="Descreva cores, tamanho, características especiais..."
                maxLength={800}
              />
              {errors.orderScopeDetail && (
                <span id="orderScopeDetail-error" className="form-error" role="alert">
                  {errors.orderScopeDetail}
                </span>
              )}
            </div>

            {/* Receive Date */}
            <div>
              <label htmlFor="receiveDate" className="form-label">
                Data desejada para receber <span aria-hidden="true">*</span>
              </label>
              <input
                type="date"
                id="receiveDate"
                name="receiveDate"
                value={formData.receiveDate}
                onChange={handleChange}
                className="form-input"
                aria-required="true"
                aria-invalid={!!errors.receiveDate}
                aria-describedby={errors.receiveDate ? "receiveDate-error" : undefined}
              />
              {errors.receiveDate && (
                <span id="receiveDate-error" className="form-error" role="alert">
                  {errors.receiveDate}
                </span>
              )}
            </div>

            {/* Submit Button */}
            <div className="pt-4">
              <button
                type="submit"
                className="btn btn-primary w-full text-lg py-3"
              >
                <span className="inline-flex items-center gap-2">
                  <svg 
                    aria-hidden="true" 
                    className="w-5 h-5" 
                    fill="currentColor" 
                    viewBox="0 0 24 24"
                  >
                    <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
                  </svg>
                  Enviar pelo WhatsApp
                </span>
              </button>
            </div>
          </div>
        </form>
      </div>
    </section>
  );
}