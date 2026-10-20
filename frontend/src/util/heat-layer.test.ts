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

    const createMap = (zoomAnimation = false) => {
        container = document.createElement("div")
        container.style.width = "400px"
        container.style.height = "300px"
        document.body.appendChild(container)
        map = L.map(container, {zoomAnimation}).setView([0, 0], 2)
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

    it("uses the default radius and gradient without options", () => {
        const leafletMap = createMap()
        heatLayer([[0, 0, 1]]).addTo(leafletMap)
        const center = leafletMap.latLngToContainerPoint([0, 0])
        expect(pixelAt(canvasOf(), Math.round(center.x), Math.round(center.y))[3]).toBeGreaterThan(0)
        expect(canvasOf().classList).toContain("leaflet-zoom-hide")
    })

    it("skips points outside of the visible map", () => {
        const leafletMap = createMap()
        leafletMap.setView([0, 0], 8)
        heatLayer([[60, 120, 1]], {radius: 10, blur: 5}).addTo(leafletMap)
        const canvas = canvasOf()
        const data = canvas.getContext("2d")!.getImageData(0, 0, canvas.width, canvas.height).data
        expect(data.some((value, index) => index % 4 === 3 && value > 0)).toBe(false)
    })

    it("follows the size of the map", () => {
        const leafletMap = createMap()
        heatLayer([[0, 0, 1]], {maxZoom: 10}).addTo(leafletMap)
        container!.style.width = "200px"
        container!.style.height = "100px"
        leafletMap.invalidateSize()
        leafletMap.fire("moveend")
        expect(canvasOf().width).toBe(200)
        expect(canvasOf().height).toBe(100)
    })

    it("can be added again after it was removed", () => {
        const leafletMap = createMap()
        const layer = heatLayer([[0, 0, 1]]).addTo(leafletMap)
        const canvas = canvasOf()
        leafletMap.removeLayer(layer)
        layer.addTo(leafletMap)
        expect(canvasOf()).toBe(canvas)
    })

    it("scales the canvas while the map animates a zoom", () => {
        const leafletMap = createMap(true)
        const layer = heatLayer([[0, 0, 1]]).addTo(leafletMap)
        expect(canvasOf().classList).toContain("leaflet-zoom-animated")

        leafletMap.fire("zoomanim", {center: leafletMap.getCenter(), zoom: 3, noUpdate: false})
        expect(canvasOf().style.transform).toContain("scale(2)")

        leafletMap.removeLayer(layer)
        expect(leafletMap.listens("zoomanim", (layer as unknown as {animateZoom: L.LeafletEventHandlerFn}).animateZoom, layer))
            .toBe(false)
    })
})
