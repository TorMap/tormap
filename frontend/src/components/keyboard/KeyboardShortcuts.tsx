import {addMonths, format, parseISO} from "date-fns";
import {useAtomValue, useSetAtom} from "jotai";
import {FunctionComponent, useEffect} from "react";

import {useDate} from "../../context/date-context";
import {isFullscreenSupported, toggleFullscreen} from "../../util/fullscreen";
import {hasModifier, isEditable, isNothingFocused, isSearchShortcut} from "../../util/keyboard";
import {showAboutDialogAtom} from "../dialogs/AboutInformation";
import {openRelaySearchAtom} from "../dialogs/relay/relay-details-atoms";
import {filteredRelaysAtom} from "../leaflet/LeafletLayers";

const dayNavigationKeys = ["ArrowLeft", "ArrowRight", "Home", "End", "PageUp", "PageDown"]

/**
 * The index of the day to select when the key is pressed on the day at the given index. Like the date slider, Page Up
 * moves forward in time. A month back or forward selects the closest available day in that direction.
 * @param availableDays all available days in ascending order
 * @param index the index of the currently selected day
 * @param key a key of dayNavigationKeys
 */
export const dayIndexForKey = (availableDays: string[], index: number, key: string): number => {
    const last = availableDays.length - 1
    switch (key) {
        case "ArrowLeft":
            return Math.max(index - 1, 0)
        case "ArrowRight":
            return Math.min(index + 1, last)
        case "Home":
            return 0
        case "End":
            return last
        case "PageUp": {
            const target = format(addMonths(parseISO(availableDays[index]), 1), "yyyy-MM-dd")
            const found = availableDays.findIndex(day => day >= target)
            return found === -1 ? last : found
        }
        case "PageDown": {
            const target = format(addMonths(parseISO(availableDays[index]), -1), "yyyy-MM-dd")
            return Math.max(availableDays.findLastIndex(day => day <= target), 0)
        }
        default:
            return index
    }
}

/**
 * Global keyboard shortcuts:
 * - Ctrl+K (Cmd+K on Apple devices) or / opens the relay search
 * - ? opens the about dialog, which lists the shortcuts
 * - F toggles full screen, where the browser supports it
 * - The arrow keys, Home, End, Page Up and Page Down select another day, while nothing has the focus. Once the map has
 *   the focus, the arrow keys pan the map instead, until Escape leaves the map.
 */
export const KeyboardShortcuts: FunctionComponent = () => {
    // App context
    const {availableDays, selectedDate, setSelectedDate} = useDate()

    // Atom state
    const filteredRelays = useAtomValue(filteredRelaysAtom)
    const openRelaySearch = useSetAtom(openRelaySearchAtom)
    const setShowAboutDialog = useSetAtom(showAboutDialogAtom)

    useEffect(() => {
        const onKeyDown = (event: KeyboardEvent) => {
            if (event.defaultPrevented || event.isComposing) return
            // "/" and "?" need Shift on many keyboard layouts
            const isPlainKey = !event.altKey && !event.ctrlKey && !event.metaKey
            if (isSearchShortcut(event) || (event.key === "/" && isPlainKey && !isEditable(event.target))) {
                event.preventDefault()
                if (filteredRelays.length > 0) openRelaySearch({relays: filteredRelays, focusSearch: true})
            } else if (event.key === "?" && isPlainKey && !isEditable(event.target)) {
                event.preventDefault()
                setShowAboutDialog(true)
            } else if (event.key.toLowerCase() === "f" && !hasModifier(event) && !isEditable(event.target) &&
                isFullscreenSupported()) {
                event.preventDefault()
                void toggleFullscreen()
            } else if (event.key === "Escape" && document.activeElement?.classList.contains("leaflet-container")) {
                (document.activeElement as HTMLElement).blur()
            } else if (dayNavigationKeys.includes(event.key) && !hasModifier(event) && isNothingFocused()) {
                const index = selectedDate ? availableDays.indexOf(selectedDate) : -1
                if (index === -1) return
                event.preventDefault()
                const newIndex = dayIndexForKey(availableDays, index, event.key)
                if (newIndex !== index) setSelectedDate(availableDays[newIndex])
            }
        }
        window.addEventListener("keydown", onKeyDown)
        return () => window.removeEventListener("keydown", onKeyDown)
    }, [availableDays, filteredRelays, openRelaySearch, selectedDate, setSelectedDate, setShowAboutDialog])

    return null
}
