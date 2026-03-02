/**
 * useOrderFormValidation — Form state and validation logic for OrderForm.
 *
 * Manages form data, field-level errors, and validation rules.
 * Extracted from OrderForm for single-responsibility and testability.
 *
 * @example
 * const { formData, errors, handleChange, validate, reset } = useOrderFormValidation();
 */

import { useState, type ChangeEvent } from 'react';
import type { OrderFormData } from './OrderForm';

export interface FormErrors {
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
  name: '',
  email: '',
  phone: '',
  address: '',
  postalCode: '',
  orderScope: '',
  orderScopeDetail: '',
  receiveDate: '',
};

export function useOrderFormValidation() {
  const [formData, setFormData] = useState<OrderFormData>(initialFormData);
  const [errors, setErrors] = useState<FormErrors>({});

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
    if (errors[name as keyof FormErrors]) {
      setErrors(prev => ({ ...prev, [name]: undefined }));
    }
  };

  const validate = (): FormErrors => {
    const newErrors: FormErrors = {};

    if (!formData.name.trim()) {
      newErrors.name = 'Nome é obrigatório';
    } else if (formData.name.length > 200) {
      newErrors.name = 'Nome deve ter no máximo 200 caracteres';
    }

    if (!formData.email.trim()) {
      newErrors.email = 'Email é obrigatório';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = 'Email inválido';
    }

    if (!formData.phone.trim()) {
      newErrors.phone = 'Telefone é obrigatório';
    } else if (!/^\d{10,11}$/.test(formData.phone.replace(/\D/g, ''))) {
      newErrors.phone = 'Telefone deve ter 10 ou 11 dígitos';
    }

    if (!formData.address.trim()) {
      newErrors.address = 'Endereço é obrigatório';
    }

    if (!formData.postalCode.trim()) {
      newErrors.postalCode = 'CEP é obrigatório';
    } else if (!/^\d{5}-?\d{3}$/.test(formData.postalCode)) {
      newErrors.postalCode = 'CEP inválido (formato: 00000-000)';
    }

    if (!formData.orderScope.trim()) {
      newErrors.orderScope = 'Tipo de boneca é obrigatório';
    }

    if (!formData.orderScopeDetail.trim()) {
      newErrors.orderScopeDetail = 'Detalhes do pedido são obrigatórios';
    }

    if (!formData.receiveDate.trim()) {
      newErrors.receiveDate = 'Data de entrega é obrigatória';
    } else if (!/^\d{2}\/\d{2}\/\d{4}$/.test(formData.receiveDate)) {
      newErrors.receiveDate = 'Data inválida (formato: DD/MM/AAAA)';
    } else {
      const [dd, mm, yyyy] = formData.receiveDate.split('/').map(Number);
      const date = new Date(yyyy, mm - 1, dd);
      if (
        date.getFullYear() !== yyyy ||
        date.getMonth() !== mm - 1 ||
        date.getDate() !== dd
      ) {
        newErrors.receiveDate = 'Data inválida';
      }
    }

    setErrors(newErrors);
    return newErrors;
  };

  const reset = () => {
    setFormData(initialFormData);
    setErrors({});
  };

  return { formData, errors, handleChange, validate, reset };
}
