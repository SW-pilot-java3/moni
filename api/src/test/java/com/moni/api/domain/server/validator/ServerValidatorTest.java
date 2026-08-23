package com.moni.api.domain.server.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.domain.instance.service.InstanceService;
import com.moni.api.domain.server.entity.Server;
import com.moni.api.domain.server.exception.ServerErrorCode;
import com.moni.api.domain.server.repository.ServerRepository;
import com.moni.api.domain.user.entity.User;
import com.moni.api.global.error.exception.CustomException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ServerValidatorTest {

    @InjectMocks
    private ServerValidator serverValidator;

    @Mock
    private ServerRepository serverRepository;

    @Mock
    private InstanceService instanceService;

    @Test
    @DisplayName("소유자 일치 시 서버 검증 및 조회 성공")
    void validateAndGetServer_success() {
        // given
        Long userId = 1L;
        Long serverId = 100L;

        User user = User.builder().email("test@example.com").passwordHash("pass1234").build();
        ReflectionTestUtils.setField(user, "id", userId);

        Instance instance = Instance.builder().name("instance1").ip("127.0.0.1").user(user).build();
        Server server = Server.builder().name("server1").port(8080).instance(instance).build();
        ReflectionTestUtils.setField(server, "id", serverId);

        given(serverRepository.findById(serverId)).willReturn(Optional.of(server));

        // when
        Server result = serverValidator.validateAndGetServer(serverId, userId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(serverId);
    }

    @Test
    @DisplayName("타인 소유 서버 접근 시 SERVER_ACCESS_DENIED 예외 발생")
    void validateAndGetServer_otherUser_throwsException() {
        // given
        Long ownerId = 1L;
        Long attackerId = 999L;
        Long serverId = 100L;

        User owner = User.builder().email("owner@example.com").passwordHash("pass1234").build();
        ReflectionTestUtils.setField(owner, "id", ownerId);

        Instance instance = Instance.builder().name("instance1").ip("127.0.0.1").user(owner).build();
        Server server = Server.builder().name("server1").port(8080).instance(instance).build();
        ReflectionTestUtils.setField(server, "id", serverId);

        given(serverRepository.findById(serverId)).willReturn(Optional.of(server));

        // when & then
        assertThatThrownBy(() -> serverValidator.validateAndGetServer(serverId, attackerId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ServerErrorCode.SERVER_ACCESS_DENIED);
    }

    @Test
    @DisplayName("존재하지 않는 서버 조회 시 SERVER_NOT_FOUND 예외 발생")
    void validateAndGetServer_notFound_throwsException() {
        // given
        Long serverId = 100L;
        Long userId = 1L;
        given(serverRepository.findById(serverId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> serverValidator.validateAndGetServer(serverId, userId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ServerErrorCode.SERVER_NOT_FOUND);
    }

    @Test
    @DisplayName("인스턴스 소유권 검증 위임 성공")
    void validateAndGetInstance_delegatesToInstanceService() {
        // given
        Long instanceId = 10L;
        Long userId = 1L;
        Instance instance = Instance.builder().name("inst").ip("127.0.0.1").build();
        given(instanceService.getInstanceOwnedBy(instanceId, userId)).willReturn(instance);

        // when
        Instance result = serverValidator.validateAndGetInstance(instanceId, userId);

        // then
        assertThat(result).isSameAs(instance);
        verify(instanceService).getInstanceOwnedBy(instanceId, userId);
    }
}
