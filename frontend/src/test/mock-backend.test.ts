import {AxiosError} from "axios";
import {describe, expect, it} from "vitest";

import {backend} from "../util/util";
import {deferred, mockBackend, networkError, reply} from "./mock-backend";

describe("mockBackend", () => {
    it("answers GET requests with data", async () => {
        mockBackend.onGet("/days", ["2024-01-01"])
        const response = await backend.get<string[]>("/days")
        expect(response.status).toBe(200)
        expect(response.data).toEqual(["2024-01-01"])
        expect(mockBackend.requested).toEqual(["GET /days"])
    })

    it("passes the parsed body and regexp captures to handlers", async () => {
        mockBackend.onPost(/^\/relay\/(\d+)$/, ({data, match}) => ({echo: data, id: match?.[1]}))
        const response = await backend.post("/relay/42", {search: "tor"})
        expect(response.data).toEqual({echo: {search: "tor"}, id: "42"})
    })

    it("lets later registrations override earlier ones", async () => {
        mockBackend.onGet("/value", 1)
        mockBackend.onGet("/value", 2)
        expect((await backend.get("/value")).data).toBe(2)
    })

    it("rejects with the response for error statuses", async () => {
        mockBackend.onGet("/broken", () => reply(500, {message: "boom"}))
        const error = await backend.get("/broken").catch(e => e as AxiosError)
        expect(error).toBeInstanceOf(AxiosError)
        expect((error as AxiosError).response?.status).toBe(500)
        expect((error as AxiosError).response?.data).toEqual({message: "boom"})
    })

    it("rejects without a response for network errors", async () => {
        mockBackend.onGet("/offline", () => networkError())
        const error = await backend.get("/offline").catch(e => e as AxiosError)
        expect((error as AxiosError).code).toBe(AxiosError.ERR_NETWORK)
        expect((error as AxiosError).response).toBeUndefined()
    })

    it("waits for deferred answers", async () => {
        const answer = deferred<string>()
        mockBackend.onGet("/slow", () => answer.promise)
        const pending = backend.get("/slow")
        answer.resolve("done")
        expect((await pending).data).toBe("done")
    })

    it("records unmocked requests and rejects them", async () => {
        const error = await backend.get("/unknown").catch(e => e as AxiosError)
        expect((error as AxiosError).code).toBe(AxiosError.ERR_NETWORK)
        expect(mockBackend.unhandled).toEqual(["GET /unknown"])
        // The shared teardown fails tests with unmocked requests, so clear it for this deliberate one
        mockBackend.reset()
    })
})
