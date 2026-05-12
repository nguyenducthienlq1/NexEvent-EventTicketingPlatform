package com.nexevent.nexevent.domains.entities;

import com.nexevent.nexevent.domains.enums.StatusTicket;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket implements Persistable<String> {
    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusTicket status;

    @Column(name = "qrcode", nullable = false, columnDefinition = "TEXT")
    private String qrCode;

    @CreationTimestamp
    @Column(name = "issued_at", updatable = false)
    private LocalDateTime issuedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PrePersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }
}