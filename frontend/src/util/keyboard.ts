/**
 * Whether the browser runs on macOS or iOS, where shortcuts use the Command key instead of Control
 */
export const isApplePlatform = (): boolean => /Mac|iPhone|iPad|iPod/.test(navigator.platform)

/**
 * The shortcut opening the relay search, in the format of the aria-keyshortcuts attribute, as text and as single keys
 */
export const searchShortcut = () => isApplePlatform()
    ? {ariaKeyShortcuts: "Meta+K", label: "⌘K", keys: ["⌘", "K"]}
    : {ariaKeyShortcuts: "Control+K", label: "Ctrl+K", keys: ["Ctrl", "K"]}

/**
 * Whether the event is Ctrl+K, or Cmd+K on Apple devices. On macOS Ctrl+K deletes to the end of a line in text fields,
 * so it is left alone there.
 */
export const isSearchShortcut = (event: KeyboardEvent): boolean =>
    event.key.toLowerCase() === "k" && !event.altKey && !event.shiftKey &&
    (isApplePlatform() ? event.metaKey && !event.ctrlKey : event.ctrlKey && !event.metaKey)

/**
 * Whether the event has a modifier key, which browsers and operating systems use for their own shortcuts
 */
export const hasModifier = (event: KeyboardEvent): boolean =>
    event.altKey || event.ctrlKey || event.metaKey || event.shiftKey

/**
 * Whether no element has the focus, so a key press is not meant for a field, button, dialog or the map
 */
export const isNothingFocused = (): boolean => {
    const active = document.activeElement
    return !active || active === document.body || active === document.documentElement
}

/**
 * Whether typing into the element enters text, so a printable key must not trigger a shortcut there
 */
export const isEditable = (target: EventTarget | null): boolean =>
    target instanceof HTMLElement &&
    (target.isContentEditable || target.tagName === "INPUT" || target.tagName === "TEXTAREA" || target.tagName === "SELECT")
