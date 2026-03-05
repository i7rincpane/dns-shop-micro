package ru.nvkz.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class UserProfile {
    private Long userId;
    private String name;
    private String surname;
    private String phone;
    @Column("birth_date")
    private LocalDate birthdate;
}
