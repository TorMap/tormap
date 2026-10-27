import {LeafletEventHandlerFnMap, Map as LeafletMap, TileLayer as LeafletTileLayer} from "leaflet";
import {FunctionComponent, ReactNode, useEffect, useState} from "react";
import {describe, expect, it, vi} from "vitest";
import {page} from "vitest/browser";

import {MapContainer, TileLayer, useMap, useMapEvents} from "./map";

let probedMap: LeafletMap | undefined

const MapProbe: FunctionComponent = () => {
    const map = useMap()
    useEffect(() => {
        probedMap = map
    }, [map])
    return <span>map ready</span>
}

const render = (children: ReactNode) => page.render(
    <MapContainer center={[30, 10]} zoom={4} minZoom={2} style={{width: 400, height: 300}}>
        {children}
    </MapContainer>
)

describe("MapContainer", () => {
    it("creates the map with the initial view and options, then renders the children", async () => {
        probedMap = undefined
        const screen = await render(<MapProbe/>)
        await expect.element(screen.getByText("map ready")).toBeInTheDocument()

        expect(probedMap).toBeInstanceOf(LeafletMap)
        expect(probedMap!.getZoom()).toBe(4)
        expect(probedMap!.getCenter().lat).toBeCloseTo(30)
        expect(probedMap!.getCenter().lng).toBeCloseTo(10)
        expect(probedMap!.getMinZoom()).toBe(2)
        const container = screen.container.querySelector(".leaflet-container") as HTMLElement
        expect(container.style.width).toBe("400px")
        expect(container.getBoundingClientRect().height).toBe(300)
    })

    it("keeps the same map when re-rendered and removes it on unmount", async () => {
        const maps = new Set<LeafletMap>()
        const Collect = ({value}: { value: number }) => {
            maps.add(useMap())
            return <span>{`render ${value}`}</span>
        }
        const Rerender = () => {
            const [value, setValue] = useState(0)
            return (
                <>
                    <button onClick={() => setValue(v => v + 1)}>again</button>
                    <MapContainer center={[0, 0]} zoom={3} style={{width: 200, height: 200}}>
                        <Collect value={value}/>
                    </MapContainer>
                </>
            )
        }
        const screen = await page.render(<Rerender/>)
        await screen.getByRole("button", {name: "again"}).click()
        await expect.element(screen.getByText("render 1")).toBeInTheDocument()
        expect(maps.size).toBe(1)

        const map = [...maps][0]
        const remove = vi.spyOn(map, "remove")
        await screen.unmount()
        expect(remove).toHaveBeenCalledOnce()
    })
})

describe("useMap", () => {
    it("fails outside of a MapContainer", async () => {
        const Outside = () => {
            useMap()
            return null
        }
        vi.spyOn(console, "error").mockImplementation(() => undefined)
        await expect(page.render(<Outside/>)).rejects.toThrow("useMap() can only be used in a descendant of <MapContainer>")
    })
})

describe("useMapEvents", () => {
    it("registers the handlers while mounted", async () => {
        const moveend = vi.fn()
        const handlers: LeafletEventHandlerFnMap = {moveend}
        const Events = () => {
            useMapEvents(handlers)
            return null
        }
        const Toggle = () => {
            const [shown, setShown] = useState(true)
            return (
                <MapContainer center={[0, 0]} zoom={3} style={{width: 200, height: 200}}>
                    <MapProbe/>
                    <button onClick={() => setShown(false)}>hide</button>
                    {shown && <Events/>}
                </MapContainer>
            )
        }
        probedMap = undefined
        const screen = await page.render(<Toggle/>)
        await vi.waitFor(() => expect(probedMap).toBeDefined())
        probedMap!.fire("moveend")
        expect(moveend).toHaveBeenCalledOnce()

        // Leaflet itself also listens to moveend, so check that the handler is no longer called
        await screen.getByRole("button", {name: "hide"}).click()
        await vi.waitFor(() => {
            probedMap!.fire("moveend")
            expect(moveend).toHaveBeenCalledTimes(1)
        })
    })
})

describe("TileLayer", () => {
    it("adds the layer with its options while mounted", async () => {
        // A data URL, so no tiles are requested from a server
        const url = "data:image/png;base64,iVBORw0KGgo=#{z}/{x}/{y}"
        const Toggle = () => {
            const [shown, setShown] = useState(true)
            return (
                <MapContainer center={[0, 0]} zoom={3} style={{width: 200, height: 200}}>
                    <MapProbe/>
                    <button onClick={() => setShown(false)}>hide</button>
                    {shown && <TileLayer url={url} maxZoom={7} noWrap={true}/>}
                </MapContainer>
            )
        }
        const screen = await page.render(<Toggle/>)
        const tileLayers = () => {
            const layers: LeafletTileLayer[] = []
            probedMap?.eachLayer(layer => {
                if (layer instanceof LeafletTileLayer) layers.push(layer)
            })
            return layers
        }
        await vi.waitFor(() => expect(tileLayers()).toHaveLength(1))
        expect(tileLayers()[0].options.maxZoom).toBe(7)
        expect(tileLayers()[0].options.noWrap).toBe(true)

        await screen.getByRole("button", {name: "hide"}).click()
        await vi.waitFor(() => expect(tileLayers()).toHaveLength(0))
    })
})
