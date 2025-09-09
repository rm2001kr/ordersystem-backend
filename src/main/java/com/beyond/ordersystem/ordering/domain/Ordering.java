package com.beyond.ordersystem.ordering.domain;

import com.beyond.ordersystem.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ordering {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter
    private OrderStatus orderStatus = OrderStatus.ORDERED;
    @JoinColumn(name = "member_id")
    public void cancelStatus(){
        this.orderStatus = OrderStatus.CANCELED;
    }
    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;
    @OneToMany(mappedBy = "ordering", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderDetail> orderDetailList = new ArrayList<>();

}
