import type {Plugin} from "vite";

const dayMs = 24 * 60 * 60 * 1000
// RFC 9116 recommends an expiry date less than a year in the future. Every deployment moves it along.
const validityDays = 180

/** The content of security.txt (RFC 9116), which tells security researchers how to report a vulnerability. */
export function securityTxt(now: Date): string {
    const expires = new Date(Math.floor(now.getTime() / dayMs) * dayMs + validityDays * dayMs)
    return [
        "Contact: https://github.com/TorMap/tormap/security/advisories/new",
        `Expires: ${expires.toISOString().replace(".000Z", "Z")}`,
        "Policy: https://github.com/TorMap/tormap/security/policy",
        "Preferred-Languages: en",
        "",
    ].join("\n")
}

/** Adds /.well-known/security.txt to the build. The contact is the GitHub advisory form described in SECURITY.md. */
export function securityTxtFile(now: () => Date = () => new Date()): Plugin {
    return {
        name: "tormap:security-txt",
        apply: "build",
        generateBundle() {
            this.emitFile({type: "asset", fileName: ".well-known/security.txt", source: securityTxt(now())})
        },
    }
}
