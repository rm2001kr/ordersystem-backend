package com.beyond.ordersystem.product.domain;

import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.product.dto.ProductUpdateDto;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
@Getter
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private long id;
    private String name;
    private String category;
    private Integer price;
    private Integer stockQuantity; // 재고
    private String imagePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Builder.Default // 빌더패턴에서 변수 초기화(디폴트 값)시 Builder.Default 어노테이션 필수
    private Role role = Role.USER;

    public void updateImageUrl(String url) {
        this.imagePath = url;
    }

    public void updateProduct(ProductUpdateDto updateDto){
        this.name = updateDto.getName();
        this.category = updateDto.getCategory();
        this.price = updateDto.getPrice();
        this.stockQuantity = updateDto.getStockQuantity();
    }

    public void updateStockQuantity(int orderQuantity){
        this.stockQuantity = this.stockQuantity - orderQuantity;
    }

    public void cancelOrder(int orderQuantity){
        this.stockQuantity = this.stockQuantity + orderQuantity;
    }
}
