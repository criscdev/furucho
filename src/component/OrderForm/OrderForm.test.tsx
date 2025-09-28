// @ts-nocheck
import { describe, it, expect } from 'vitest';
import { renderWithProviders, screen } from "../../test-utils";
import { orderFactory } from "../../test/factories/orderFactory";
import OrderForm from "./OrderForm";

describe("OrderForm", () => {
    it("renders the form with all input fields and the submit button", () => {
        renderWithProviders(<OrderForm />);

        expect(screen.getByTestId("name")).toBeTruthy();
        expect(screen.getByTestId("email")).toBeTruthy();
        expect(screen.getByTestId("phone")).toBeTruthy();
        expect(screen.getByTestId("address")).toBeTruthy();
        expect(screen.getByTestId("postalCode")).toBeTruthy();
        expect(screen.getByTestId("orderScope")).toBeTruthy();
        expect(screen.getByTestId("orderScopeDetail")).toBeTruthy();
        expect(screen.getByTestId("receiveDate")).toBeTruthy();
        expect(screen.getByRole("button", { name: /enviar/i })).toBeTruthy();
    });

    it("has required attributes on inputs", () => {
        renderWithProviders(<OrderForm />);

        expect(screen.getByTestId("name").getAttribute("required")).not.toBeNull();
        expect(screen.getByTestId("email").getAttribute("required")).not.toBeNull();
        expect(screen.getByTestId("phone").getAttribute("required")).not.toBeNull();
        expect(screen.getByTestId("address").getAttribute("required")).not.toBeNull();
        expect(screen.getByTestId("postalCode").getAttribute("required")).not.toBeNull();
        expect(screen.getByTestId("orderScope").getAttribute("required")).not.toBeNull();
        expect(screen.getByTestId("orderScopeDetail").getAttribute("required")).not.toBeNull();
        expect(screen.getByTestId("receiveDate").getAttribute("required")).not.toBeNull();
    });

    it("applies maxlength and pattern attributes correctly", () => {
        renderWithProviders(<OrderForm />);

        const name = screen.getByTestId("name");
        expect(name.getAttribute("maxLength")).toBe("200");

        const email = screen.getByTestId("email");
        expect(email.getAttribute("maxLength")).toBe("100");
        expect(email.getAttribute("type")).toBe("email");
        expect(email.getAttribute("pattern")).toBe("[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$");

        const phone = screen.getByTestId("phone");
        expect(phone.getAttribute("maxLength")).toBe("11");
        expect(phone.getAttribute("type")).toBe("tel");

        const address = screen.getByTestId("address");
        expect(address.getAttribute("maxLength")).toBe("200");

        const postalCode = screen.getByTestId("postalCode");
        expect(postalCode.getAttribute("maxLength")).toBe("10");
        expect(postalCode.getAttribute("pattern")).toBe("[0-9]{10}");

        const orderScope = screen.getByTestId("orderScope");
        expect(orderScope.getAttribute("maxLength")).toBe("100");

        const orderScopeDetail = screen.getByTestId("orderScopeDetail");
        expect(orderScopeDetail.getAttribute("maxLength")).toBe("800");

        const receiveDate = screen.getByTestId("receiveDate");
        expect(receiveDate.getAttribute("maxLength")).toBe("10");
        expect(receiveDate.getAttribute("pattern")).toBe("[0-9]{2}/[0-9]{2}/[0-9]{4}");
        expect(receiveDate.getAttribute("type")).toBe("text");
    });
});
