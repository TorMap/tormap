import L from "leaflet";

(globalThis as typeof globalThis & {L?: typeof L}).L = L;

await import("leaflet.heat");
