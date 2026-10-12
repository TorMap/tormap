import L from "leaflet";
import {afterEach, describe, expect, it} from "vitest";

import {HeatLayer, heatLayer} from "./heat-layer";

describe("HeatLayer", () => {
    let container: HTMLDivElement | undefined
    let map: L.Map | undefined

    afterEach(() => {
        map?.remove()
        container?.remove()
        map = undefined
        container = undefined
    })

    const createMap = () => {
        container = document.createElement("div")
        container.style.width = "400px"
        container.style.height = "300px"
        document.body.appendChild(container)
        map = L.map(container, {zoomAnimation: false}).setView([0, 0], 2)
        return map
    }

    const canvasOf = () => container!.querySelector<HTMLCanvasElement>("canvas.leaflet-heatmap-layer")!

    const pixelAt = (canvas: HTMLCanvasElement, x: number, y: number) =>
        Array.from(canvas.getContext("2d")!.getImageData(x, y, 1, 1).data)

    it("draws points in the gradient color and leaves the rest transparent", () => {
        const leafletMap = createMap()
        heatLayer([[0, 0, 1], [0, 0, 1]], {radius: 10, blur: 5, max: 1, gradient: {0: "#ff0000", 1: "#ff0000"}})
            .addTo(leafletMap)

        const canvas = canvasOf()
        expect(canvas.width).toBe(400)
        expect(canvas.height).toBe(300)

        const center = leafletMap.latLngToContainerPoint([0, 0])
        const [red, green, blue, alpha] = pixelAt(canvas, Math.round(center.x), Math.round(center.y))
        expect(alpha).toBeGreaterThan(0)
        expect([red, green, blue]).toEqual([255, 0, 0])

        expect(pixelAt(canvas, 5, 5)[3]).toBe(0)
    })

    it("removes its canvas and listeners from the map", () => {
        const leafletMap = createMap()
        const layer = heatLayer([[0, 0, 1]]).addTo(leafletMap)
        expect(canvasOf()).not.toBeNull()

        leafletMap.removeLayer(layer)

        expect(container!.querySelector("canvas.leaflet-heatmap-layer")).toBeNull()
        expect(leafletMap.listens("moveend", (layer as unknown as {reset: L.LeafletEventHandlerFn}).reset, layer))
            .toBe(false)
    })

    it("is a Leaflet layer", () => {
        expect(heatLayer([])).toBeInstanceOf(L.Layer)
        expect(heatLayer([])).toBeInstanceOf(HeatLayer)
    })
})
