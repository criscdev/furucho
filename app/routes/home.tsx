import type { Route } from "./+types/home";
import { Welcome } from "../welcome/welcome";
import { Header } from "../../src/component/Header/Header";
import { OrderForm } from "../../src/component/OrderForm/OrderForm";
import { Gallery } from "../../src/component/Gallery/Gallery";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "Roberta Furucho | Bonecas Artesanais" },
    { name: "description", content: "Bonecas artesanais feitas à mão com amor e dedicação. Encomende sua boneca personalizada única." },
    { property: "og:title", content: "Roberta Furucho | Bonecas Artesanais" },
    { property: "og:description", content: "Bonecas artesanais feitas à mão com amor e dedicação." },
    { property: "og:type", content: "website" },
    { name: "theme-color", content: "#F4B8C5" },
  ];
}

export default function Home() {
  return (
    <>
      <Header />
      <Welcome />
      <Gallery />
      <OrderForm />
    </>
  );
}
