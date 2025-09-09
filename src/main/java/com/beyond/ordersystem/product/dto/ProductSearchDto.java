package com.beyond.ordersystem.product.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data


public class ProductSearchDto {
    private long id;
    private String name;
    private String category;
    private Integer price;
    private Integer stockQuantity;
}
