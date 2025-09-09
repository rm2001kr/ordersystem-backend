package com.beyond.ordersystem.ordering.dto;

import com.beyond.ordersystem.product.domain.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data

public class OrderCreateDto {
    private Long productId;
    private Integer productCount;
}
