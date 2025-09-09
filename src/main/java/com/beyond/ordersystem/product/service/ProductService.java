package com.beyond.ordersystem.product.service;

import com.beyond.ordersystem.member.domain.Member;
import com.beyond.ordersystem.member.repository.MemberRepository;
import com.beyond.ordersystem.ordering.domain.OrderDetail;
import com.beyond.ordersystem.ordering.domain.Ordering;
import com.beyond.ordersystem.ordering.dto.OrderDetailResDto;
import com.beyond.ordersystem.ordering.dto.OrderListResDto;
import com.beyond.ordersystem.ordering.service.StockInventoryService;
import com.beyond.ordersystem.product.domain.Product;
import com.beyond.ordersystem.product.dto.ProductCreateDto;
import com.beyond.ordersystem.product.dto.ProductResDto;
import com.beyond.ordersystem.product.dto.ProductSearchDto;
import com.beyond.ordersystem.product.dto.ProductUpdateDto;
import com.beyond.ordersystem.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final S3Client s3Client;
    private final StockInventoryService stockInventoryService;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public Long save(ProductCreateDto productCreateDto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("member is not found"));
        Product product = productRepository.save(productCreateDto.toEntity(member));

        if (productCreateDto.getProductImage() != null) {
//        image명 설정
            String fileName = "product-" + product.getId() + "-productImage-" + productCreateDto.getProductImage().getOriginalFilename();

//        저장 객체 구성
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(productCreateDto.getProductImage().getContentType()) //image/jpeg, video/mp4 ...
                    .build();

//        이미지를 업로드(byte 형태로)
            try {
                s3Client.putObject(putObjectRequest, RequestBody.fromBytes(productCreateDto.getProductImage().getBytes()));
            } catch (Exception e) {
//            checked -> unchecked로 바꿔 전체 rollback 되도록 예외처리
                throw new IllegalArgumentException("이미지 업로드 실패");
            }

//        이미지 url 추출
            String imgUrl = s3Client.utilities().getUrl(a -> a.bucket(bucket).key(fileName)).toExternalForm();
            product.updateImageUrl(imgUrl);
        }

//        redis에 재고 세팅
        stockInventoryService.makeStockQuantity(product.getId(), product.getStockQuantity());
        return product.getId();
    }

    public Page<ProductResDto> findAll(Pageable pageable, ProductSearchDto productSearchDto) {
        System.out.println(productSearchDto);
        Specification<Product> specification = new Specification<Product>() {
            @Override
            public Predicate toPredicate(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
//                Root : 엔티티의 속성을 접근하기 위한 객체, CriteriaBuilder : 쿼리를 생성하기 위한 객체
                List<Predicate> predicateList = new ArrayList<>();
                if (productSearchDto.getCategory() != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("category"), productSearchDto.getCategory()));
                }
                if (productSearchDto.getName() != null) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + productSearchDto.getName() + "%"));
                }
                Predicate[] predicateArr = new Predicate[predicateList.size()];
                for (int i = 0; i < predicateList.size(); i++) {
                    predicateArr[i] = predicateList.get(i);
                }
//                위의 검색 조건들을 하나(한줄)의 Predicate 객체로 만들어서 return
                return criteriaBuilder.and(predicateArr);
            }
        };
        Page<Product> productList = productRepository.findAll(specification, pageable);
        return productList.map(ProductResDto::fromEntity);
    }

        public ProductResDto findById(Long id){
            Product product = productRepository.findById(id).orElseThrow(()->new EntityNotFoundException("상품정보없음"));
            return ProductResDto.fromEntity(product);
    }


        public Long update(ProductUpdateDto productUpdateDto, Long productId) {
            Product product = productRepository.findById(productId).orElseThrow(()->new EntityNotFoundException("상품정보없음"));


            if (productUpdateDto.getProductImage() != null && !productUpdateDto.getProductImage().isEmpty()) {

//                기존이미지를 삭제 : 파일명으로 삭제
                String imgUrl = product.getImagePath();
                String fileName = imgUrl.substring(imgUrl.lastIndexOf("/")+1);
                s3Client.deleteObject(a->a.bucket(bucket).key(fileName));

//                신규이미지 등록
                String newFileName = "product-" + product.getId() + "-productImage-" + productUpdateDto.getProductImage().getOriginalFilename();
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(newFileName)
                        .contentType(productUpdateDto.getProductImage().getContentType())
                        .build();
                try{
                    s3Client.putObject(putObjectRequest, RequestBody.fromBytes(productUpdateDto.getProductImage().getBytes()));
                } catch (Exception e) {
                    throw new IllegalArgumentException("이미지 업로드 실패");
                }
                // image Url 추출
                String newImgUrl = s3Client.utilities()
                        .getUrl(a -> a.bucket(bucket).key(fileName)) // ← key 추가
                        .toExternalForm(); // ToDo - 예외처리 필요

                product.updateImageUrl(newImgUrl);
            }
            else {
                product.updateImageUrl(null);
            }
            return product.getId();
    }
}
