package uk.danielgooding.kokaplayground.common.websocket;

public interface ISessionState<StateTag> {
    /// The websocket handler will report the count of sessions in each state.
    /// This will be a set of tagged gauges, with the tag produced by this function.
    StateTag getStateTag();
}
