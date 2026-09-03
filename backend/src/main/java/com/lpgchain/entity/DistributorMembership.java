package com.lpgchain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class DistributorMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "distributor_id")
    private Distributor distributor;

    private String membershipRole;
    private String status;

    public DistributorMembership() {
    }

    public DistributorMembership(User user, Distributor distributor,
                                  String membershipRole, String status) {
        this.user = user;
        this.distributor = distributor;
        this.membershipRole = membershipRole;
        this.status = status;
    }

    public DistributorMembership(Long id, User user, Distributor distributor,
                                  String membershipRole, String status) {
        this.id = id;
        this.user = user;
        this.distributor = distributor;
        this.membershipRole = membershipRole;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Distributor getDistributor() { return distributor; }
    public void setDistributor(Distributor distributor) { this.distributor = distributor; }

    public String getMembershipRole() { return membershipRole; }
    public void setMembershipRole(String membershipRole) { this.membershipRole = membershipRole; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
