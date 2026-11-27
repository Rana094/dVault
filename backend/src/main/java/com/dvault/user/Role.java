package com.dvault.user;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {
    public enum Name { USER, ADMIN }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, unique = true, length = 16)
    private Name name;

    protected Role() {}
    public Role(Name name) { this.name = name; }
    public Long getId() { return id; }
    public Name getName() { return name; }
}
