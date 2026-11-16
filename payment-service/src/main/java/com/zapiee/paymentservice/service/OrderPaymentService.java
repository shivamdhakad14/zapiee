package com.zapiee.paymentservice.service;

import com.zapiee.paymentservice.client.OrderClient;
import com.zapiee.paymentservice.client.RestaurantClient;
import com.zapiee.paymentservice.dto.request.InitiateOrderPaymentRequest;
import com.zapiee.paymentservice.dto.request.OrderPaymentRequest;
import com.zapiee.paymentservice.dto.response.*;
import com.zapiee.paymentservice.enums.OrderPaymentStatus;
import com.zapiee.paymentservice.enums.PaymentStatus;
import com.zapiee.paymentservice.enums.Status;
import com.zapiee.paymentservice.exception.PaymentException;
import com.zapiee.paymentservice.exception.ResourceNotFoundException;
import com.zapiee.paymentservice.model.OrderPayment;
import com.zapiee.paymentservice.repository.OrderPaymentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPaymentService {

    private final RestaurantClient restaurantClient;
    private final OrderPaymentRepository orderPaymentRepository;
    private static final int    INTENT_TTL_SECONDS = 300;
    private static final String CURRENCY = "INR";
    private final OrderClient orderClient;

    public OrderPaymentInitResponse initiateOrder(Long restaurantId,
                                                  InitiateOrderPaymentRequest req) {

        RestaurantInformation restaurant = restaurantClient.getRestaurantById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found: " + restaurantId));

        OrderResponse order = orderClient.getOrderByIdAndRestaurantId(restaurant.getRestaurantId(), req.getOrderId())
                .orElseThrow(() -> new RuntimeException("order not found: " + restaurantId + "and" + req.getOrderId()));

        if(!order.getTableNumber().equals(req.getTableNumber()) && !order.getAmount().equals(req.getAmount())){
            throw new RuntimeException("order not process due to wrong order data");
        }

        if (!restaurant.getStatus().equals(Status.ACTIVE)) {
            throw new PaymentException(
                    "This restaurant is not currently accepting payments.",
                    "RESTAURANT_INACTIVE", HttpStatus.PAYMENT_REQUIRED);
        }

        if (orderPaymentRepository.findByOrderIdAndRestaurantId(req.getOrderId(), restaurantId).isPresent()) {
            throw new PaymentException(
                    "Payment already initiated for order: " + req.getOrderId(),
                    "DUPLICATE_ORDER", HttpStatus.CONFLICT);
        }

        log.info("[restaurant={}] Initiating UPI payment | order={} amount={}",
                restaurantId, req.getOrderId(), req.getAmount());

            String desc = req.getDescription() != null
                    ? req.getDescription()
                    : "Order " + req.getOrderId() + " | Table " + req.getTableNumber();

            OrderPayment payment = OrderPayment.builder()
                    .restaurantId(restaurantId)
                    .orderId(req.getOrderId())
                    .tableNumber(req.getTableNumber())
                    .amount(order.getAmount())
                    .currency(CURRENCY)
                    .customerName(req.getCustomerName())
                    .customerEmail(req.getCustomerEmail())
                    .customerPhone(req.getCustomerPhone())
                    .description(desc)
                    .orderPaymentStatus(OrderPaymentStatus.INITIATED)
                    .paymentStatus(PaymentStatus.PENDING)
                    .build();

            orderPaymentRepository.save(payment);
            log.info("Payment record {} saved for order={}", payment.getId(), req.getOrderId());

            return OrderPaymentInitResponse.builder()
                    .paymentRecordId(payment.getId())
                    .orderId(req.getOrderId())
                    .amount(req.getAmount())
                    .currency(CURRENCY)
                    .restaurantName(restaurant.getName())
                    .customerName(req.getCustomerName())
                    .customerEmail(req.getCustomerEmail())
                    .customerPhone(req.getCustomerPhone())
                    .description(desc)
                    .build();
    }

    @Transactional
    public OrderResponse confirmOrderPayment(Long restaurantId, OrderPaymentRequest request){
        RestaurantInformation restaurant = restaurantClient.getRestaurantById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found: " + restaurantId));

        OrderPayment orderPayment = orderPaymentRepository.findByOrderIdAndRestaurantId(request.getOrderId(), restaurant.getRestaurantId())
                .orElseThrow(() -> new RuntimeException("order not found: " + request.getOrderId()));

        if(!orderPayment.getTableNumber().equals(request.getTableNumber())){
            throw new RuntimeException("order not process due to wrong order data");
        }

        if(!orderPayment.getId().equals(request.getPaymentRecordId())){
            throw new RuntimeException("order not process wrong payment id");
        }

        if (!restaurant.getStatus().equals(Status.ACTIVE)) {
            throw new PaymentException(
                    "This restaurant is not currently accepting payments.",
                    "RESTAURANT_INACTIVE", HttpStatus.PAYMENT_REQUIRED);
        }

        if (orderPayment.getOrderPaymentStatus().equals(OrderPaymentStatus.PAID)) {
            throw new PaymentException(
                    "Payment already initiated for order: " + request.getOrderId(),
                    "DUPLICATE_ORDER", HttpStatus.CONFLICT);
        }

        orderPayment.setOrderPaymentStatus(OrderPaymentStatus.PAID);
        orderPayment.setPaymentStatus(PaymentStatus.CAPTURED);
        orderPayment.setPaidAt(LocalDateTime.now());
        orderPaymentRepository.save(orderPayment);

        orderClient.paymentSuccess(orderPayment.getOrderId());

        return OrderResponse.builder()
                .amount(orderPayment.getAmount())
                .currency(orderPayment.getCurrency())
                .tableNumber(orderPayment.getTableNumber())
                .orderId(orderPayment.getOrderId()).build();
    }


    public PagedResponse<OrderPaymentResponse> getOrderPaymentByRestaurantId(
            Long restaurantId, Pageable pageable) {

        RestaurantInformation restaurant = restaurantClient.getRestaurantById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        Page<OrderPayment> orderPayments =
                orderPaymentRepository.findByRestaurantIdOrderByCreatedAtDesc(
                        restaurant.getRestaurantId(), pageable);

        return mapToDto(orderPayments);
    }

    public PagedResponse<OrderPaymentResponse> getOrderPaymentByRestaurantIdAndStatus(
            Long restaurantId,
            OrderPaymentStatus status,
            Pageable pageable) {

        RestaurantInformation restaurant = restaurantClient.getRestaurantById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));

        Page<OrderPayment> orderPayments =
                orderPaymentRepository.findByRestaurantIdAndOrderPaymentStatusOrderByCreatedAtDesc(
                        restaurant.getRestaurantId(), status, pageable);

        return mapToDto(orderPayments);
    }

    private PagedResponse<OrderPaymentResponse> mapToDto(
            Page<OrderPayment> orderPayments) {

        Page<OrderPaymentResponse> responsePage = orderPayments.map(orderPayment ->
                OrderPaymentResponse.builder()
                        .id(orderPayment.getId())
                        .orderId(orderPayment.getOrderId())
                        .orderPaymentStatus(orderPayment.getOrderPaymentStatus())
                        .amount(orderPayment.getAmount())
                        .customerEmail(orderPayment.getCustomerEmail())
                        .paymentStatus(orderPayment.getPaymentStatus())
                        .customerName(orderPayment.getCustomerName())
                        .customerPhone(orderPayment.getCustomerPhone())
                        .description(orderPayment.getDescription())
                        .paidAt(orderPayment.getPaidAt())
                        .currency(orderPayment.getCurrency())
                        .restaurantId(orderPayment.getRestaurantId())
                        .tableNumber(orderPayment.getTableNumber())
                        .build()
        );

        return PagedResponse.from(responsePage);
    }


}
