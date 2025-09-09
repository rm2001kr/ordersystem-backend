package com.beyond.ordersystem.ordering.controller;

import com.beyond.ordersystem.ordering.domain.OrderDetail;
import com.beyond.ordersystem.ordering.domain.Ordering;
import com.beyond.ordersystem.ordering.dto.OrderCreateDto;
import com.beyond.ordersystem.ordering.dto.OrderDetailResDto;
import com.beyond.ordersystem.ordering.dto.OrderListResDto;
import com.beyond.ordersystem.ordering.service.OrderingService;
import com.beyond.ordersystem.product.dto.CommonDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ordering")
public class OrderingController {

    private final OrderingService orderingService;

//    @PostMapping("/create")
//    public ResponseEntity<?> create(@RequestBody List<OrderCreateDto> orderCreateDto) {
//        Long id = orderingService.create(orderCreateDto);
//        return new ResponseEntity<>(
//                CommonDto.builder()
//                        .result(id)
//                        .status_code(HttpStatus.CREATED.value())
//                        .status_message("주문완료")
//                        .build(),
//                HttpStatus.CREATED
//        );

    @PostMapping("/create")
    public ResponseEntity<?> createConcurrent(@RequestBody List<OrderCreateDto> orderCreateDto) {
        Long id = orderingService.createConcurrent(orderCreateDto);
            return new ResponseEntity<>(
                    CommonDto.builder()
                        .result(id)
                            .status_code(HttpStatus.CREATED.value())
                        .status_message("주문완료")
                        .build(),
                    HttpStatus.CREATED
            );
    }

    @GetMapping("/list")
    public ResponseEntity<?> findAll(){
        List<OrderListResDto> orderListResDtos = orderingService.findAll();
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(orderListResDtos)
                        .status_code(HttpStatus.OK.value())
                        .status_message("상품목록조회성공")
                        .build(),
                HttpStatus.OK
        );


    }
    @GetMapping("/myorders")
    public ResponseEntity<?> findAllByMember() {
        List<OrderListResDto> orderDetailResDtos = orderingService.findAllByMember();
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(orderDetailResDtos)
                        .status_code(HttpStatus.OK.value())
                        .status_message("상품목록조회성공")
                        .build(),
                HttpStatus.OK
        );
    }

    @GetMapping("/cancel/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> orderCancel(@PathVariable Long id){
        Ordering ordering = orderingService.cancel(id);
        return new ResponseEntity<>(
                CommonDto.builder()
                        .result(ordering)
                        .status_code(HttpStatus.OK.value())
                        .status_message("상품취소완료")
                        .build(),
                HttpStatus.OK
        );
    }
}
