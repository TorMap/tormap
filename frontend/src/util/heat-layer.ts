/*
 Heatmap layer for Leaflet, ported to TypeScript from Leaflet.heat 0.2.0 (https://github.com/Leaflet/Leaflet.heat)
 and simpleheat 0.2.0 (https://github.com/mourner/simpleheat), which it bundles. Only the parts TorMap uses are kept;
 the drawing algorithm is unchanged.

 Copyright (c) 2014, Vladimir Agafonkin
 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification, are
 permitted provided that the following conditions are met:

    1. Redistributions of source code must retain the above copyright notice, this list of
       conditions and the following disclaimer.

    2. Redistributions in binary form must reproduce the above copyright notice, this list
       of conditions and the following disclaimer in the documentation and/or other materials
       provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
import L from "leaflet";

/** Latitude, longitude and intensity */
export type HeatLatLngTuple = [number, number, number];

export interface HeatLayerOptions extends L.LayerOptions {
    /** Minimum opacity of a point */
    minOpacity?: number
    /** Zoom level where points reach maximum intensity (defaults to the map's maximum zoom) */
    maxZoom?: number
    /** Maximum point intensity */
    max?: number
    /** Radius of each point in pixels */
    radius?: number
    /** Amount of blur in pixels */
    blur?: number
    /** Color gradient, e.g. {0.4: 'blue', 0.65: 'lime', 1: 'red'} */
    gradient?: Record<number, string>
}

/** A data point in container pixels: x, y and intensity */
type HeatPoint = [number, number, number];

const DEFAULT_RADIUS = 25;
const DEFAULT_BLUR = 15;
const DEFAULT_GRADIENT: Record<number, string> = {0.4: "blue", 0.6: "cyan", 0.7: "lime", 0.8: "yellow", 1: "red"};

/**
 * Draws intensity points onto a canvas and colorizes them with a gradient (simpleheat).
 */
class CanvasHeatmap {
    width: number
    height: number
    /** Radius of a point including blur */
    r = 0
    private readonly ctx: CanvasRenderingContext2D
    private max = 1
    private data: HeatPoint[] = []
    private circle?: HTMLCanvasElement
    private grad?: Uint8ClampedArray

    constructor(canvas: HTMLCanvasElement) {
        this.ctx = canvas.getContext("2d")!
        this.width = canvas.width
        this.height = canvas.height
    }

    setData(data: HeatPoint[]) {
        this.data = data
        return this
    }

    setMax(max: number) {
        this.max = max
        return this
    }

    setRadius(r: number, blur = DEFAULT_BLUR) {
        const circle = this.circle = document.createElement("canvas")
        const ctx = circle.getContext("2d")!
        const r2 = this.r = r + blur

        circle.width = circle.height = r2 * 2

        // Draw the circle outside the canvas and only keep its blurred shadow
        ctx.shadowOffsetX = ctx.shadowOffsetY = 200
        ctx.shadowBlur = blur
        ctx.shadowColor = "black"

        ctx.beginPath()
        ctx.arc(r2 - 200, r2 - 200, r, 0, Math.PI * 2, true)
        ctx.closePath()
        ctx.fill()
        return this
    }

    setGradient(grad: Record<number, string>) {
        const canvas = document.createElement("canvas")
        const ctx = canvas.getContext("2d")!
        const gradient = ctx.createLinearGradient(0, 0, 0, 256)

        canvas.width = 1
        canvas.height = 256

        for (const [stop, color] of Object.entries(grad)) {
            gradient.addColorStop(Number(stop), color)
        }

        ctx.fillStyle = gradient
        ctx.fillRect(0, 0, 1, 256)

        this.grad = ctx.getImageData(0, 0, 1, 256).data
        return this
    }

    draw(minOpacity?: number) {
        if (!this.circle) this.setRadius(DEFAULT_RADIUS)
        if (!this.grad) this.setGradient(DEFAULT_GRADIENT)

        const ctx = this.ctx
        ctx.clearRect(0, 0, this.width, this.height)

        for (const [x, y, value] of this.data) {
            ctx.globalAlpha = Math.max(value / this.max, minOpacity || 0.05)
            ctx.drawImage(this.circle!, x - this.r, y - this.r)
        }

        // Map the alpha channel of each pixel to the gradient color
        const colored = ctx.getImageData(0, 0, this.width, this.height)
        const pixels = colored.data
        const gradient = this.grad!
        for (let i = 3; i < pixels.length; i += 4) {
            const j = pixels[i] * 4
            if (j) {
                pixels[i - 3] = gradient[j]
                pixels[i - 2] = gradient[j + 1]
                pixels[i - 1] = gradient[j + 2]
            }
        }
        ctx.putImageData(colored, 0, 0)
        return this
    }
}

/** Private Leaflet map members the layer relies on, as in Leaflet.heat */
interface MapInternals {
    _getMapPanePos(): L.Point
    _getCenterOffset(center: L.LatLng): L.Point
}

/**
 * Leaflet layer that renders points as a heatmap on a canvas in the overlay pane (Leaflet.heat).
 */
export class HeatLayer extends L.Layer {
    declare options: HeatLayerOptions
    private readonly latlngs: HeatLatLngTuple[]
    private canvas?: HTMLCanvasElement
    private heat?: CanvasHeatmap

    constructor(latlngs: HeatLatLngTuple[], options: HeatLayerOptions = {}) {
        super()
        this.latlngs = latlngs
        L.setOptions(this, options)
    }

    onAdd(map: L.Map) {
        this._map = map
        if (!this.canvas) this.initCanvas(map)
        map.getPane("overlayPane")!.appendChild(this.canvas!)
        map.on("moveend", this.reset, this)
        if (map.options.zoomAnimation && L.Browser.any3d) {
            map.on("zoomanim", this.animateZoom, this)
        }
        this.reset()
        return this
    }

    onRemove(map: L.Map) {
        this.canvas?.remove()
        map.off("moveend", this.reset, this)
        map.off("zoomanim", this.animateZoom, this)
        return this
    }

    private initCanvas(map: L.Map) {
        const canvas = this.canvas = L.DomUtil.create("canvas", "leaflet-heatmap-layer leaflet-layer")
        canvas.style.transformOrigin = "50% 50%"

        const size = map.getSize()
        canvas.width = size.x
        canvas.height = size.y

        const animated = map.options.zoomAnimation && L.Browser.any3d
        L.DomUtil.addClass(canvas, "leaflet-zoom-" + (animated ? "animated" : "hide"))

        this.heat = new CanvasHeatmap(canvas)
        this.heat.setRadius(this.options.radius || DEFAULT_RADIUS, this.options.blur)
        if (this.options.gradient) this.heat.setGradient(this.options.gradient)
        if (this.options.max) this.heat.setMax(this.options.max)
    }

    private reset() {
        const map = this._map
        const canvas = this.canvas!
        const heat = this.heat!
        L.DomUtil.setPosition(canvas, map.containerPointToLayerPoint([0, 0]))

        const size = map.getSize()
        if (heat.width !== size.x) canvas.width = heat.width = size.x
        if (heat.height !== size.y) canvas.height = heat.height = size.y

        this.redraw()
    }

    private redraw() {
        const map = this._map
        const heat = this.heat!
        const r = heat.r
        const size = map.getSize()
        const bounds = new L.Bounds(L.point([-r, -r]), size.add([r, r]))
        const max = this.options.max === undefined ? 1 : this.options.max
        const maxZoom = this.options.maxZoom === undefined ? map.getMaxZoom() : this.options.maxZoom
        const v = 1 / Math.pow(2, Math.max(0, Math.min(maxZoom - map.getZoom(), 12)))
        const cellSize = r / 2
        const panePos = (map as unknown as MapInternals)._getMapPanePos()
        const offsetX = panePos.x % cellSize
        const offsetY = panePos.y % cellSize

        // Merge points that fall into the same grid cell, weighted by intensity
        const grid: HeatPoint[][] = []
        for (const latlng of this.latlngs) {
            const p = map.latLngToContainerPoint([latlng[0], latlng[1]])
            if (!bounds.contains(p)) continue
            const x = Math.floor((p.x - offsetX) / cellSize) + 2
            const y = Math.floor((p.y - offsetY) / cellSize) + 2
            const k = (latlng[2] !== undefined ? +latlng[2] : 1) * v

            const row = grid[y] = grid[y] || []
            const cell = row[x]
            if (!cell) {
                row[x] = [p.x, p.y, k]
            } else {
                cell[0] = (cell[0] * cell[2] + p.x * k) / (cell[2] + k)
                cell[1] = (cell[1] * cell[2] + p.y * k) / (cell[2] + k)
                cell[2] += k
            }
        }

        const data: HeatPoint[] = []
        for (const row of grid) {
            if (!row) continue
            for (const cell of row) {
                if (cell) data.push([Math.round(cell[0]), Math.round(cell[1]), Math.min(cell[2], max)])
            }
        }

        heat.setData(data).draw(this.options.minOpacity)
    }

    private animateZoom(e: L.ZoomAnimEvent) {
        const map = this._map
        const internals = map as unknown as MapInternals
        const scale = map.getZoomScale(e.zoom)
        const offset = internals._getCenterOffset(e.center).multiplyBy(-scale).subtract(internals._getMapPanePos())
        L.DomUtil.setTransform(this.canvas!, offset, scale)
    }
}

export const heatLayer = (latlngs: HeatLatLngTuple[], options?: HeatLayerOptions) => new HeatLayer(latlngs, options)
