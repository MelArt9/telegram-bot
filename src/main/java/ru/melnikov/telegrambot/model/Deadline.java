package ru.melnikov.telegrambot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "deadlines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deadline extends BaseEntity {

    private String title;

    @Column(name = "deadline_at")
    private LocalDateTime deadlineAt;

    private String description;

    @Column(name = "link_url")
    private String linkUrl;

    @Column(name = "link_text")
    private String linkText;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
}