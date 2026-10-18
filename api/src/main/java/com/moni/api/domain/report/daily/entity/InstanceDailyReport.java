package com.moni.api.domain.report.daily.entity;

import com.moni.api.domain.instance.entity.Instance;
import com.moni.api.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Table(
        name = "instance_daily_reports",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_instance_daily_reports_instance_date",
                columnNames = {"instance_id", "report_date"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InstanceDailyReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Instance instance;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "summary", nullable = false, length = 255)
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", nullable = false, columnDefinition = "jsonb")
    private String content;

    @Builder
    public InstanceDailyReport(Instance instance, LocalDate reportDate, String summary, String content) {
        this.instance = instance;
        this.reportDate = reportDate;
        this.summary = summary;
        this.content = content;
    }
}
