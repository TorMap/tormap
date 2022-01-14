import React, {FunctionComponent, useContext, useState} from "react";
import {Statistics} from "../types/app-state";

interface StatisticsInterface {
    statistics: Statistics
    setStatistics: (statistics: Statistics) => void
}

const StatisticsContext = React.createContext<StatisticsInterface | null>(null)

export function useStatistics() {
    return useContext(StatisticsContext)!
}

interface StatisticsProviderProps {
    children?: React.ReactNode;
}

export const StatisticsProvider: FunctionComponent<StatisticsProviderProps> = ({children}) => {
    const [statistics, setStatistics] = useState<Statistics>({
        relayGuardCount: 0,
        relayExitCount: 0,
        relayOtherCount: 0,
    })
    return (
        <StatisticsContext.Provider
            value={{
                statistics,
                setStatistics
            }}>
            {children}
        </StatisticsContext.Provider>
    )
}