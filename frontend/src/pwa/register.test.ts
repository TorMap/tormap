import {afterEach, describe, expect, it, vi} from "vitest";

import {getPwaState, setPwaState} from "./pwa-state";
import {applyUpdate, registerServiceWorker} from "./register";

class FakeWorker extends EventTarget {
    state = "installing"
    postMessage = vi.fn()

    install() {
        this.state = "installed"
        this.dispatchEvent(new Event("statechange"))
    }
}

class FakeRegistration extends EventTarget {
    installing: FakeWorker | null = null
    waiting: FakeWorker | null = null

    startUpdate(worker: FakeWorker) {
        this.installing = worker
        this.dispatchEvent(new Event("updatefound"))
    }
}

class FakeContainer extends EventTarget {
    registration = new FakeRegistration()
    register = vi.fn(async () => this.registration)

    constructor(public controller: object | null) {
        super()
    }

    changeController() {
        this.controller = {}
        this.dispatchEvent(new Event("controllerchange"))
    }
}

async function register(container: FakeContainer, reload = vi.fn()) {
    await registerServiceWorker(container as unknown as ServiceWorkerContainer, reload)
    return reload
}

describe("registerServiceWorker", () => {
    afterEach(() => setPwaState({waitingWorker: undefined}))

    it("registers the worker for the whole site", async () => {
        const container = new FakeContainer(null)
        await register(container)
        expect(container.register).toHaveBeenCalledWith("/sw.js", {scope: "/"})
    })

    it("offers a new version once it is installed", async () => {
        const container = new FakeContainer({})
        await register(container)
        const worker = new FakeWorker()
        container.registration.startUpdate(worker)
        expect(getPwaState().waitingWorker).toBeUndefined()

        worker.install()
        expect(getPwaState().waitingWorker).toBe(worker)
    })

    it("offers a version that was already waiting", async () => {
        const container = new FakeContainer({})
        const worker = new FakeWorker()
        container.registration.waiting = worker
        await register(container)
        expect(getPwaState().waitingWorker).toBe(worker)
    })

    it("offers nothing on the first install", async () => {
        const container = new FakeContainer(null)
        await register(container)
        const worker = new FakeWorker()
        container.registration.startUpdate(worker)
        worker.install()
        expect(getPwaState().waitingWorker).toBeUndefined()
    })

    it("does not reload when the first worker takes over, but when a later one does", async () => {
        const container = new FakeContainer(null)
        const reload = await register(container)

        container.changeController()
        expect(reload).not.toHaveBeenCalled()

        container.changeController()
        container.changeController()
        expect(reload).toHaveBeenCalledTimes(1)
    })

    it("activates a waiting version when asked to", () => {
        const worker = new FakeWorker()
        applyUpdate(worker as unknown as ServiceWorker)
        expect(worker.postMessage).toHaveBeenCalledWith("SKIP_WAITING")
    })
})
