import type { JSX } from "react";

interface FormValues {
    name: string;
    email: string;
    phone: string;
    address: string;
    orderScope: string;
    orderScopeDetail: string;
    receiveDate: string;
};

const initialValuesType: FormValues ={
    name: "",
    email: "",
    phone: "",
    address: "",
    orderScope: "",
    orderScopeDetail: "",
    receiveDate: ""
};

const OrderForm = (): JSX.Element => {
    return (
        <form>
            <input data-testid="name" type="text" name="name" placeholder="Nome*" required maxLength={200}/>
            <input data-testid="email" type="email" name="email" placeholder="Email*" required maxLength={100} pattern="[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$"/>
            <input data-testid="phone" type="tel" name="phone" placeholder="Telefone*" required maxLength={11}/>
            <input data-testid="address" type="text" name="address" placeholder="Endereço*" required maxLength={200}/>
            <input data-testid="postalCode" type="text" name="postalCode" placeholder="CEP*" required maxLength={10} pattern="[0-9]{10}"/>
            <input data-testid="orderScope" type="text" name="orderScope" placeholder="Resumo do Pedido*" required maxLength={100}/>
            <input data-testid="orderScopeDetail" type="text" name="orderScopeDetail" placeholder="Detalhes do Pedido*" required maxLength={800}/>
            {/* Use text + pattern to enforce dd/mm/yyyy format (e.g. 25/12/2025) */}
            <input data-testid="receiveDate" type="text" name="receiveDate" placeholder="Data que quer receber o pedido*" required maxLength={10} pattern="[0-9]{2}/[0-9]{2}/[0-9]{4}"/>
            <button type="submit">enviar</button>
        </form>
    );
};
export default OrderForm;