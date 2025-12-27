package ru.melnikov.telegrambot.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Link extends BaseEntity {

    private String title;

    private String url;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;
}