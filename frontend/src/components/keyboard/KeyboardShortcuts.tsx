import {useAtomValue, useSetAtom} from "jotai";
import {FunctionComponent, useEffect} from "react";

import {useDate} from "../../context/date-context";
import {hasModifier, isNothingFocused, isSearchShortcut} from "../../util/keyboard";
import {openRelaySearchAtom} from "../dialogs/relay/relay-details-atoms";
import {filteredRelaysAtom} from "../leaflet/LeafletLayers";

/**
 * Global keyboard shortcuts:
 * - Ctrl+K (Cmd+K on Apple devices) opens the relay search
 * - The left and right arrow keys select the previous or next day, while nothing has the focus. Once the map has the
 *   focus, they pan the map instead.
 */
export const KeyboardShortcuts: FunctionComponent = () => {
    // App context
    const {availableDays, selectedDate, setSelectedDate} = useDate()

    // Atom state
    const filteredRelays = useAtomValue(filteredRelaysAtom)
    const openRelaySearch = useSetAtom(openRelaySearchAtom)

    useEffect(() => {
        const onKeyDown = (event: KeyboardEvent) => {
            if (event.defaultPrevented || event.isComposing) return
            if (isSearchShortcut(event)) {
                event.preventDefault()
                if (filteredRelays.length > 0) openRelaySearch({relays: filteredRelays, focusSearch: true})
            } else if ((event.key === "ArrowLeft" || event.key === "ArrowRight") && !hasModifier(event) && isNothingFocused()) {
                const index = selectedDate ? availableDays.indexOf(selectedDate) : -1
                if (index === -1) return
                event.preventDefault()
                const newIndex = index + (event.key === "ArrowLeft" ? -1 : 1)
                if (newIndex >= 0 && newIndex < availableDays.length) setSelectedDate(availableDays[newIndex])
            }
        }
        window.addEventListener("keydown", onKeyDown)
        return () => window.removeEventListener("keydown", onKeyDown)
    }, [availableDays, filteredRelays, openRelaySearch, selectedDate, setSelectedDate])

    return null
}
