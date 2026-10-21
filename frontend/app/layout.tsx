import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "PokéBattle",
  description: "Cliente académico para la API PokéBattle",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="es">
      <body>{children}</body>
    </html>
  );
}
