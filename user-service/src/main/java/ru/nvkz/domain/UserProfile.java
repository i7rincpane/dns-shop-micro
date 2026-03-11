package ru.nvkz.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDate;

@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
public class UserProfile {
    @Id
    @EqualsAndHashCode.Include
    private Long userId;
    private String name;
    private String surname;
    private String phone;
    @Column("birth_date")
    private LocalDate birthdate;
    @Version
    private Long version;
}
