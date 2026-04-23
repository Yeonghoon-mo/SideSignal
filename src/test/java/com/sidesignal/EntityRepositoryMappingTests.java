package com.sidesignal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.sidesignal.auth.domain.UserEntity;
import com.sidesignal.auth.infrastructure.UserRepository;
import com.sidesignal.pair.domain.PairEntity;
import com.sidesignal.pair.domain.PairInviteEntity;
import com.sidesignal.pair.infrastructure.PairInviteRepository;
import com.sidesignal.pair.infrastructure.PairRepository;
import com.sidesignal.signal.domain.SignalEntity;
import com.sidesignal.signal.domain.SignalEventEntity;
import com.sidesignal.signal.domain.SignalEventType;
import com.sidesignal.signal.domain.SignalStatus;
import com.sidesignal.signal.infrastructure.SignalEventRepository;
import com.sidesignal.signal.infrastructure.SignalRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EntityRepositoryMappingTests {

    private final UserRepository userRepository;
    private final PairRepository pairRepository;
    private final PairInviteRepository pairInviteRepository;
    private final SignalRepository signalRepository;
    private final SignalEventRepository signalEventRepository;

    @Autowired
    EntityRepositoryMappingTests(
        UserRepository userRepository,
        PairRepository pairRepository,
        PairInviteRepository pairInviteRepository,
        SignalRepository signalRepository,
        SignalEventRepository signalEventRepository
    ) {
        this.userRepository = userRepository;
        this.pairRepository = pairRepository;
        this.pairInviteRepository = pairInviteRepository;
        this.signalRepository = signalRepository;
        this.signalEventRepository = signalEventRepository;
    }

    @Test
    void saveAndFindCoreEntities() {
        UserEntity firstUser = userRepository.save(new UserEntity(
            "first@sidesignal.test",
            "first-password-hash",
            "First"
        ));
        UserEntity secondUser = userRepository.save(new UserEntity(
            "second@sidesignal.test",
            "second-password-hash",
            "Second"
        ));
        PairEntity pair = pairRepository.save(new PairEntity(firstUser, secondUser));

        PairInviteEntity invite = new PairInviteEntity(
            "invite-code-hash",
            firstUser,
            Instant.now().plus(1, ChronoUnit.HOURS)
        );
        invite.accept(secondUser, pair, Instant.now());
        pairInviteRepository.save(invite);

        SignalEntity signal = new SignalEntity(pair, firstUser);
        signal.update(SignalStatus.LEAVING_SOON, Instant.now().plus(30, ChronoUnit.MINUTES), "wrapping up");
        signalRepository.save(signal);

        SignalEventEntity event = new SignalEventEntity(
            pair,
            firstUser,
            SignalEventType.SIGNAL_UPDATED,
            Map.of("status", SignalStatus.LEAVING_SOON.name())
        );
        signalEventRepository.save(event);

        assertThat(userRepository.findByEmail("first@sidesignal.test"))
            .hasValueSatisfying(user -> assertThat(user.getDisplayName()).isEqualTo("First"));
        assertThat(pairRepository.findByFirstUserIdOrSecondUserId(firstUser.getId(), firstUser.getId()))
            .hasValueSatisfying(foundPair -> assertThat(foundPair.getId()).isEqualTo(pair.getId()));
        assertThat(pairInviteRepository.findByCodeHash("invite-code-hash"))
            .hasValueSatisfying(foundInvite -> assertThat(foundInvite.getAcceptedBy().getId()).isEqualTo(secondUser.getId()));
        assertThat(signalRepository.findByPairIdAndUserId(pair.getId(), firstUser.getId()))
            .hasValueSatisfying(foundSignal -> assertThat(foundSignal.getStatus()).isEqualTo(SignalStatus.LEAVING_SOON));
        assertThat(signalEventRepository.findTop100ByPairIdAndCreatedAtAfterOrderByCreatedAtAsc(
            pair.getId(),
            Instant.EPOCH
        ))
            .hasSize(1)
            .first()
            .satisfies(foundEvent -> {
                assertThat(foundEvent.getEventType()).isEqualTo(SignalEventType.SIGNAL_UPDATED);
                assertThat(foundEvent.getPayload()).containsEntry("status", SignalStatus.LEAVING_SOON.name());
            });
    }
}
