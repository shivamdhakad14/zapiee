
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Schema(
        name = "OrderMapper",
        description = "Maps order requests and entities to response DTOs"
)
public class OrderMapper {

    private static final String DEFAULT_CURRENCY = "INR";

    public List<OrderItem> toEntity(AddItemRequest request, Order order) {

        if (request == null) {
            throw new IllegalArgumentException("Add item request cannot be null");
        }

        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one order item is required");
        }

        List<OrderItem> result = new ArrayList<>();

        for (OrderItemDTO dto : request.getItems()) {
            if (dto == null) {
                throw new IllegalArgumentException("Order item cannot be null");
            }

            OrderItem item = toEntity(dto, order);
            result.add(item);
        }

        return result;
    }


    private OrderItem toEntity(OrderItemDTO dto, Order order) {

        if (dto == null) {
            throw new IllegalArgumentException("Order item cannot be null");
        }

        return OrderItem.builder()
                .order(order)
                .name(dto.getName())
                .price(dto.getPrice())
                .quantity(dto.getQuantity())
                .imageUrl(dto.getImageUrl())
                .currency(resolveCurrency(dto.getCurrency()))
                .build();
    }

    public OrderItemDTO toDto(OrderItem item) {

        if (item == null) {
            throw new IllegalArgumentException("Order item cannot be null");
        }

        return OrderItemDTO.builder()
                .itemId(item.getId())
                .name(item.getName())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .imageUrl(item.getImageUrl())
                .currency(resolveCurrency(item.getCurrency()))
                .build();
    }


    public OrderItemResponse toItemResponse(OrderItem item) {

        if (item == null) {
            return null;
        }

        BigDecimal price = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;

        int quantity = item.getQuantity() != null ? item.getQuantity() : 0;

        BigDecimal lineTotal = price.multiply(BigDecimal.valueOf(quantity));

        return OrderItemResponse.builder()
                .itemId(item.getId())
                .name(item.getName())
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .imageUrl(item.getImageUrl())
                .currency(resolveCurrency(item.getCurrency()))
                .total(lineTotal)
                .build();
    }


    public OrderResponse toOrderResponse(Order order) {

        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

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
                .currency(resolveOrderCurrency(order.getItems()))
                .payment(order.isPayment())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .completedAt(order.getCompletedAt())
                .items(toItemResponseList(order.getItems()))
                .build();
    }


    public OrderListResponse toListResponse(Order order) {

        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

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
                .currency(resolveOrderCurrency(order.getItems()))
                .payment(order.isPayment())
                .createdAt(order.getCreatedAt())
                .completedAt(order.getCompletedAt())
                .items(toItemResponseList(order.getItems()))
                .build();
    }

    public OrderSnapshotResponse toSnapshot(Order order) {

        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

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


    private List<OrderItemResponse> toItemResponseList(
            List<OrderItem> items) {

        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        List<OrderItemResponse> result = new ArrayList<>(items.size());

        for (OrderItem item : items) {
            if (item != null) {
                result.add(toItemResponse(item));
            }
        }

        return result;
    }


    private String resolveOrderCurrency(List<OrderItem> items) {

        if (items == null || items.isEmpty()) {
            return DEFAULT_CURRENCY;
        }

        for (OrderItem item : items) {
            if (item != null && item.getCurrency() != null &&
                    !item.getCurrency().isBlank()) {
                return item.getCurrency().trim()
                        .toUpperCase(java.util.Locale.ROOT);
            }
        }

        return DEFAULT_CURRENCY;
    }


    private String resolveCurrency(String currency) {

        if (currency == null || currency.isBlank()) {
            return DEFAULT_CURRENCY;
        }

        return currency.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
