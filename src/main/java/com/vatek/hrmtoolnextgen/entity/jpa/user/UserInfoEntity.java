package com.vatek.hrmtoolnextgen.entity.jpa.user;

import com.vatek.hrmtoolnextgen.entity.common.IdentityEntity;
import com.vatek.hrmtoolnextgen.enumeration.EUserLevel;
import com.vatek.hrmtoolnextgen.enumeration.EUserPosition;
import com.vatek.hrmtoolnextgen.enumeration.EUserProgramLanguage;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "user_infos")
public class UserInfoEntity extends IdentityEntity {

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "identity_card", unique = true, nullable = false)
    private String identityCard;

    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber1;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "current_address")
    private String currentAddress;

    @Column(name = "permanent_address")
    private String permanentAddress;

    @Column(name = "onboard_date")
    private LocalDate onboardDate;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    private EUserLevel userLevel;

    @ElementCollection(targetClass = EUserProgramLanguage.class, fetch = FetchType.EAGER)
    private List<EUserProgramLanguage> programLanguage;

    @Enumerated(EnumType.STRING)
    private EUserPosition userPosition;

    @OneToOne(mappedBy = "userInfo")
    private UserEntity user;
}
