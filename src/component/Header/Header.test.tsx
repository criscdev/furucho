// @ts-nocheck
import { describe, it, expect } from 'vitest';
import { renderWithProviders } from "../../test-utils";
import Header from "./Header";
import { screen } from "@testing-library/react";

describe("Header", () => {
    it("renders without crashing", () => {
        renderWithProviders(<Header />, undefined);

        expect(screen.getByTestId("header")).toBeTruthy();
    });
});
