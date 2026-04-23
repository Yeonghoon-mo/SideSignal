-- 콕 찌르기 이벤트 타입 허용
ALTER TABLE signal_events
    DROP CONSTRAINT ck_signal_events_event_type;

ALTER TABLE signal_events
    ADD CONSTRAINT ck_signal_events_event_type CHECK (
        event_type IN (
            'SIGNAL_UPDATED',
            'DEPARTURE_TIME_CLEARED',
            'POKE_SENT'
        )
    );
