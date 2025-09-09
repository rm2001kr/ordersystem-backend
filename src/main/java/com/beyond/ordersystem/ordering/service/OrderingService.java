package com.beyond.ordersystem.ordering.service;

import com.beyond.ordersystem.common.service.SseAlarmService;
import com.beyond.ordersystem.common.service.StockRabbitMqService;
import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.repository.MemberRepository;
import com.beyond.ordersystem.ordering.domain.OrderDetail;
import com.beyond.ordersystem.ordering.domain.Ordering;
import com.beyond.ordersystem.ordering.dto.OrderCreateDto;
import com.beyond.ordersystem.ordering.dto.OrderListResDto;
import com.beyond.ordersystem.ordering.repository.OrderDetailRepository;
import com.beyond.ordersystem.ordering.repository.OrderingRepository;
import com.beyond.ordersystem.product.domain.Product;
import com.beyond.ordersystem.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.hibernate.query.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@Transactional
@RequiredArgsConstructor
public class OrderingService {

    private final OrderingRepository orderingRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final StockInventoryService stockInventoryService;
    private final StockRabbitMqService stockRabbitMqService;
    private final SseAlarmService sseAlarmService;

    public Long create(List<OrderCreateDto> orderCreateDtoList) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("오류"));

        Ordering ordering = Ordering.builder()
                .member(member)
                .build();

        for (OrderCreateDto dto : orderCreateDtoList) {
            Product product = productRepository.findById(dto.getProductId()).orElseThrow(() -> new EntityNotFoundException("오류"));
            if (product.getStockQuantity() < dto.getProductCount()) {
//                예외를 강제 발생시킴으로서, 모든 임시 저장사항들을 rollback 처리
                throw new IllegalArgumentException("재고가 부족합니다");
            }
//            1. 동시에 접근하는 상황에서 update 값의 정합성이 깨지고, 갱신이상이 발생
//            2. spring 버전이나 mysql 버전에 따라 jpa 에서 강제에러(deadlock)를 유발시켜 대부분의 요청실패 발생

            product.updateStockQuantity(dto.getProductCount());

            OrderDetail orderDetail = OrderDetail.builder()
                    .product(product)
                    .quantity(dto.getProductCount())
                    .ordering(ordering)
                    .build();
            orderDetailRepository.save(orderDetail);
//            ordering.getOrderDetailList().add(orderDetail);
        }
        orderingRepository.save(ordering);
        return ordering.getId();
    }

    public List<OrderListResDto> findAll(){
        return orderingRepository.findAll().stream().map(OrderListResDto::fromEntity).collect(Collectors.toList());
    }


    public List<OrderListResDto> findAllByMember(){
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            Member member = memberRepository.findByEmail(email).orElseThrow(()-> new EntityNotFoundException("member is not found"));
            return orderingRepository.findAllByMember(member).stream().map(OrderListResDto::fromEntity).collect(Collectors.toList());
    }


    @Transactional(isolation = Isolation.READ_COMMITTED) // 격리레밸을 낮춤으로서, 성능향상과 lock 관련 문제 원전 차단
    public Long createConcurrent(List<OrderCreateDto> orderCreateDtoList) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("오류"));

        Ordering ordering = Ordering.builder()
                .member(member)
                .build();

        for (OrderCreateDto dto : orderCreateDtoList) {
            Product product = productRepository.findById(dto.getProductId()).orElseThrow(() -> new EntityNotFoundException("오류"));

//            redis 에서 재고수량 확인 및 재고수량 감소처리
            int newQuantity = stockInventoryService.decreaseStockQuantity(product.getId(), dto.getProductCount());
            if(newQuantity < 0){
                throw new IllegalArgumentException("재고부족");
            }
            OrderDetail orderDetail = OrderDetail.builder()
                    .product(product)
                    .quantity(dto.getProductCount())
                    .ordering(ordering)
                    .build();
            orderDetailRepository.save(orderDetail);
//            ordering.getOrderDetailList().add(orderDetail);
//            rdb 에 사후 update를 위한 메세지 발행 (비동기처리)
            stockRabbitMqService.publish(dto.getProductId(), dto.getProductCount());
        }
        orderingRepository.save(ordering);

//        주문 성공 시, admin 유저에게 알림 메세지 전송
        sseAlarmService.publishMessage("admin@naver.com", email, ordering.getId());

        return ordering.getId();
    }




    // 주문취소시 값 증가
    public Ordering cancel(Long id) {
//        Ordering DB 상태값 변경 CANCELED
        Ordering ordering = orderingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("오류"));
        ordering.cancelStatus();
        for (OrderDetail orderDetail : ordering.getOrderDetailList()) {

//        rdb 재고 업데이트
            orderDetail.getProduct().cancelOrder(orderDetail.getQuantity());
            int quantity = orderDetail.getQuantity();

//        redis의 재고값 증가
            stockInventoryService.increaseStockQuantity(orderDetail.getProduct().getId(), quantity);
        }
        return ordering;
    }
}
