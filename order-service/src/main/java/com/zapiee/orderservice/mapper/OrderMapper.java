package com.zapiee.orderservice.mapper;

import com.zapiee.orderservice.dto.request.AddItemRequest;
import com.zapiee.orderservice.dto.request.OrderItemDTO;
import com.zapiee.orderservice.dto.response.OrderItemResponse;
import com.zapiee.orderservice.dto.response.OrderListResponse;
import com.zapiee.orderservice.dto.response.OrderResponse;
import com.zapiee.orderservice.dto.response.OrderSnapshotResponse;
import com.zapiee.orderservice.model.Order;
import com.zapiee.orderservice.model.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
@Schema(
        name = "Order Mapper",
        description = "Maps Order Service entities to request and response DTOs."
)
public class OrderMapper {


    public List<OrderItem> toEntity(AddItemRequest request, Order order) {

        if (request == null || request.getItems() == null ||
                request.getItems().isEmpty()) {
            return Collections.emptyList();
        }

        if (order == null) {
            throw new IllegalArgumentException(
                    "Order cannot be null.");
        }

        return request.getItems()
                .stream()
                .filter(Objects::nonNull)
                .map(item -> toEntity(item, order))
                .toList();
    }

    private OrderItem toEntity(OrderItemDTO dto, Order order) {
        if (dto == null) {
            throw new IllegalArgumentException(
                    "Order item cannot be null.");
        }

        return OrderItem.builder()
                .order(order)
                .name(dto.getName())
                .price(dto.getPrice())
                .quantity(dto.getQuantity())
                .imageUrl(dto.getImageUrl())
                .currency(
                        dto.getCurrency() != null
                                && !dto.getCurrency().isBlank()
                                ? dto.getCurrency()
                                : "INR"
                )
                .build();
    }

    public OrderItemDTO toDto(OrderItem item) {

        return OrderItemDTO.builder()
                .itemId(item.getId())
                .name(item.getName())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .imageUrl(item.getImageUrl())
                .currency(resolveItemCurrency(item))
                .build();
    }


    private OrderItemResponse toItemResponse( OrderItem item ) {
        if (item == null) {
            return null;
        }

        BigDecimal lineTotal = BigDecimal.ZERO;
        if (item.getPrice() != null && item.getQuantity() != null) {
            lineTotal = item.getPrice() .multiply( BigDecimal.valueOf(item.getQuantity()) );
        }

        return OrderItemResponse.builder()
                .itemId(item.getId())
                .name(item.getName())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .imageUrl(item.getImageUrl())
                .currency(resolveItemCurrency(item))
                .lineTotal(lineTotal)
                .build();
    }

    public OrderResponse toOrderResponse(Order order) {

        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .restaurantId(order.getRestaurantId())
                .tableId(order.getTableId())
                .tableNumber(order.getTableNumber())
                .sessionToken(order.getSessionToken())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .taxPercentage(order.getTaxPercentage())
                .taxAmount(order.getTaxAmount())
                .totalAmount(order.getTotalAmount())
                .currency(resolveCurrency(order.getItems()))
                .payment(order.isPayment())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .completedAt(order.getCompletedAt())
                .items(toItemResponseList(order.getItems()))
                .build();
    }

    public OrderListResponse toListResponse(Order order) {

        return OrderListResponse.builder()
                .id(order.getId())
                .orderId(order.getOrderId())
                .restaurantId(order.getRestaurantId())
                .tableId(order.getTableId())
                .tableNumber(order.getTableNumber())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .taxPercentage(order.getTaxPercentage())
                .taxAmount(order.getTaxAmount())
                .totalAmount(order.getTotalAmount())
                .currency(resolveCurrency(order.getItems()))
                .payment(order.isPayment())
                .createdAt(order.getCreatedAt())
                .completedAt(order.getCompletedAt())
                .items(toItemResponseList(order.getItems()))
                .build();
    }

    public OrderSnapshotResponse toSnapshot(Order order) {

        return OrderSnapshotResponse.builder()
                .restaurantId(order.getRestaurantId())
                .orderId(order.getOrderId())
                .tableId(order.getTableId())
                .tableNumber(order.getTableNumber())
                .sessionToken(order.getSessionToken())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .taxPercentage(order.getTaxPercentage())
                .taxAmount(order.getTaxAmount())
                .totalAmount(order.getTotalAmount())
                .payment(order.isPayment())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .completedAt(order.getCompletedAt())
                .items(toItemResponseList(order.getItems()))
                .build();
    }

    private List<OrderItemResponse> toItemResponseList(List<OrderItem> items) {

        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        return items.stream()
                .filter(Objects::nonNull)
                .map(this::toItemResponse)
                .toList();
    }

    private List<OrderItemDTO> toItemDtoList(List<OrderItem> items) {

        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        return items.stream()
                .filter(Objects::nonNull)
                .map(this::toDto)
                .toList();
    }

    private String resolveCurrency(List<OrderItem> items) {

        if (items == null || items.isEmpty()) {
            return "INR";
        }

        return items.stream()
                .filter(Objects::nonNull)
                .map(OrderItem::getCurrency)
                .filter(currency ->
                        currency != null &&
                                !currency.isBlank()
                )
                .findFirst()
                .orElse("INR");
    }

    private String resolveItemCurrency(OrderItem item) {

        if (item.getCurrency() == null || item.getCurrency().isBlank()) {
            return "INR";
        }

        return item.getCurrency();
    }

    private java.math.BigDecimal calculateLineTotal(OrderItem item) {

        if (item.getPrice() == null || item.getQuantity() == null) {
            return java.math.BigDecimal.ZERO;
        }

        return item.getPrice().multiply(
                        java.math.BigDecimal.valueOf(
                                item.getQuantity()
                        )
                );
    }
}
