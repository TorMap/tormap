export interface RelayView {
    id: string;
    firstSeen: string;
    lastSeen: string;
    lat: number;
    long: number;
    flags?: RelayFlag[];
}

export enum RelayFlag {
    Valid,
    Named,
    Unamed,
    Running,
    Stable,
    Exit,
    Fast,
    Guard,
    Authority,
    V2Dir,
    HSDir,
    NoEdConsensus,
    StaleDesc,
    Sybil,
    BadExit,
}
