package edu.wharton.alumni.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alumni_profiles")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class AlumniProfile {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CohortCampus cohortCampus;

    @Column(nullable = false)
    private int classYear;

    private String currentTitle;
    private String currentCompany;
    private String industry;
    private String city;
    private String stateCountry;
    private String linkedinUrl;

    @Column(length = 5000)
    private String bio;

    private boolean willingToMentor;
    private boolean hiring;
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private boolean approved;

    @Column(nullable = false)
    private Instant createdAt;

    protected AlumniProfile() {
    }

    public AlumniProfile(UUID id, String email, String passwordHash, String firstName, String lastName,
                         String phoneNumber, CohortCampus cohortCampus, int classYear, String currentTitle,
                         String currentCompany, String industry, String city, String stateCountry,
                         String linkedinUrl, String bio, boolean willingToMentor, boolean hiring,
                         String avatarUrl, Role role, boolean approved, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
        this.cohortCampus = cohortCampus;
        this.classYear = classYear;
        this.currentTitle = currentTitle;
        this.currentCompany = currentCompany;
        this.industry = industry;
        this.city = city;
        this.stateCountry = stateCountry;
        this.linkedinUrl = linkedinUrl;
        this.bio = bio;
        this.willingToMentor = willingToMentor;
        this.hiring = hiring;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.approved = approved;
        this.createdAt = createdAt;
    }

    public AlumniProfile withoutPassword() {
        return new AlumniProfile(id, email, null, firstName, lastName, phoneNumber, cohortCampus, classYear,
                currentTitle, currentCompany, industry, city, stateCountry, linkedinUrl, bio, willingToMentor,
                hiring, avatarUrl, role, approved, createdAt);
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public String phoneNumber() {
        return phoneNumber;
    }

    public CohortCampus cohortCampus() {
        return cohortCampus;
    }

    public int classYear() {
        return classYear;
    }

    public String currentTitle() {
        return currentTitle;
    }

    public String currentCompany() {
        return currentCompany;
    }

    public String industry() {
        return industry;
    }

    public String city() {
        return city;
    }

    public String stateCountry() {
        return stateCountry;
    }

    public String linkedinUrl() {
        return linkedinUrl;
    }

    public String bio() {
        return bio;
    }

    public boolean willingToMentor() {
        return willingToMentor;
    }

    public boolean hiring() {
        return hiring;
    }

    public String avatarUrl() {
        return avatarUrl;
    }

    public Role role() {
        return role;
    }

    public boolean approved() {
        return approved;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
