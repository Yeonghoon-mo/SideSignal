package com.sidesignal.signal.api;

import java.util.List;

public record PairSignalsResponse(
    List<SignalResponse> signals
) {
    public static PairSignalsResponse from(List<SignalResponse> signals) {
        return new PairSignalsResponse(signals);
    }
}
