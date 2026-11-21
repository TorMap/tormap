import {FamilyV1Dto, RelayFamilyIdentifier, RelayLocationDto, RelayV1Dto} from "../dto/relay";
import {RelayFlag} from "../types/relay";

/*
 The map reads the public API v1 (api/openapi-v1.json), the same API the TorMap CLI uses. These functions turn its
 responses into the shapes the map works with.
 */

const flagsByName = new Map<string, RelayFlag>(
    Object.values(RelayFlag)
        .filter((value): value is RelayFlag => typeof value === "number")
        .map(flag => [RelayFlag[flag], flag])
)

/**
 * The month (YYYY-MM) of a day (YYYY-MM-DD). Relay details, families and autonomous systems are stored per month.
 */
export const monthOfDay = (day: string) => day.slice(0, 7)

/**
 * Converts a relay of `/v1/days/{day}/relays`. Flags this frontend does not know yet are left out.
 */
export function toRelayLocation(relay: RelayV1Dto): RelayLocationDto {
    return {
        lat: relay.lat,
        long: relay.long,
        country: relay.country,
        flags: (relay.flags ?? []).flatMap(name => {
            const flag = flagsByName.get(name)
            return flag === undefined ? [] : [flag]
        }),
        fingerprint: relay.fingerprint,
        hasDetails: !!relay.nickname,
        familyId: relay.familyId,
        nickname: relay.nickname ?? "",
        consensusWeight: relay.consensusWeight,
        asNumber: relay.autonomousSystemNumber,
    }
}

/**
 * Converts a family of `/v1/families?month=` into a row of the family dialog
 */
export function toFamilyIdentifier(family: FamilyV1Dto): RelayFamilyIdentifier {
    const names = family.autonomousSystems.map(as => as.name).filter(name => !!name)
    return {
        id: family.id,
        memberCount: family.members.length,
        nicknames: family.members.map(member => member.nickname).join(", "),
        autonomousSystems: names.length > 0 ? names.join(", ") : undefined,
    }
}

/**
 * The path of the details of a relay in a month
 */
export const relayDetailsPath = (fingerprint: string, month: string) =>
    `/v1/relays/${encodeURIComponent(fingerprint)}?month=${encodeURIComponent(month)}`
