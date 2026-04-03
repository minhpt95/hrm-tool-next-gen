package com.minhpt.hrmtoolnextgen.entity.jpa.user;

import java.time.LocalDate;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.minhpt.hrmtoolnextgen.entity.common.IdentityEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EUserLevel;
import com.minhpt.hrmtoolnextgen.enumeration.EUserPosition;
import com.minhpt.hrmtoolnextgen.enumeration.EUserProgramLanguage;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "user_infos")
@SQLDelete(sql = "UPDATE user_infos SET is_delete = TRUE, deleted_date = NOW() WHERE id = ?")
@SQLRestriction("is_delete = FALSE")
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
