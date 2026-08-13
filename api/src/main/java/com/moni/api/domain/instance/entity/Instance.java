package com.moni.api.domain.instance.entity;

import com.moni.api.domain.instance.enums.InstanceStatus;
import com.moni.api.domain.user.entity.User;
import com.moni.api.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "instances")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Instance extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;
    @Column(name = "ip", length = 45, nullable = false)
    private String ip;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private InstanceStatus status;

    @Column(name = "last_received_at", nullable = true)
    private LocalDateTime lastReceivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public Instance(String name, String ip, User user) {
        this.name = name;
        this.ip = ip;
        this.user = user;
        this.status = InstanceStatus.DISCONNECTED;
    }

    public void update(String name, String ip) {
        this.name = name;
        this.ip = ip;
    }
}
