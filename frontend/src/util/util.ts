import axios from "axios";
import {useEffect, useState} from "react";

import {backendApiUrl} from "../config";

/**
 * new React-Hook for handling
 * @param value Value to debounce
 * @param delay Time without changes to update value
 */
export function useDebounce<T>(value: T, delay: number): T {
    // State and setters for debounced value
    const [debouncedValue, setDebouncedValue] = useState<T>(value);
    useEffect(
        () => {
            if (value === undefined) return undefined
            // Update debounced value after delay
            const handler = setTimeout(() => {
                setDebouncedValue(value);
            }, delay);
            // Cancel the timeout if value changes (also on delay change or unmount)
            // This is how we prevent debounced value from updating if value is changed ...
            // .. within the delay period. Timeout gets cleared and restarted.
            return () => {
                clearTimeout(handler);
            };
        },
        [value, delay] // Only re-call effect if value or delay changes
    );
    return debouncedValue;
}

/**
 * Helper to check if object contains key
 */
export const nameOfFactory = <T>() => (name: keyof T) => name;

export const backend = axios.create({
    baseURL: backendApiUrl,
    headers: {
        "Content-type": "application/json"
    }
});