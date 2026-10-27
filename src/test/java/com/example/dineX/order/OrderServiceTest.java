package com.example.dineX.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.example.dineX.common.ApiException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private FoodOrderRepository orders;

    @InjectMocks
    private OrderService service;

    @Test
    void totalsIgnoreCancelledOrders() {
        FoodOrder served = order(FoodOrder.Status.SERVED, "100.00", "2.50", "2.50");
        FoodOrder cancelled = order(FoodOrder.Status.CANCELLED, "50.00", "1.25", "1.25");

        OrderService.Totals totals = service.totals(List.of(served, cancelled));

        assertEquals(new BigDecimal("100.00"), totals.subtotal());
        assertEquals(new BigDecimal("2.50"), totals.sgst());
        assertEquals(new BigDecimal("2.50"), totals.cgst());
        assertEquals(BigDecimal.ZERO.setScale(2), totals.tipAmount());
        assertEquals(new BigDecimal("105.00"), totals.grandTotal());
    }

    @Test
    void percentRoundsToTwoDecimals() {
        assertEquals(new BigDecimal("2.50"),
                OrderService.percent(new BigDecimal("100.00"), new BigDecimal("2.5")));
        assertEquals(new BigDecimal("0.33"),
                OrderService.percent(new BigDecimal("13.33"), new BigDecimal("2.5")));
    }

    @Test
    void newCodeHasPrefixAndHexSuffix() {
        assertTrue(OrderService.newCode("BILL").matches("BILL-[0-9A-F]{10}"));
    }

    @Test
    void kitchenCannotCancelOrders() {
        FoodOrder order = order(FoodOrder.Status.ACCEPTED, "10.00", "0.25", "0.25");
        when(orders.findByIdAndHotelId(1L, 9L)).thenReturn(Optional.of(order));

        assertThrows(ApiException.class,
                () -> service.changeStatus(9L, 1L, FoodOrder.Status.CANCELLED, true));
    }

    @Test
    void invalidTransitionIsRejected() {
        FoodOrder order = order(FoodOrder.Status.ORDERED, "10.00", "0.25", "0.25");
        when(orders.findByIdAndHotelId(1L, 9L)).thenReturn(Optional.of(order));

        assertThrows(ApiException.class,
                () -> service.changeStatus(9L, 1L, FoodOrder.Status.SERVED, false));
    }

    @Test
    void servedOrderCannotChangeAgain() {
        FoodOrder order = order(FoodOrder.Status.SERVED, "10.00", "0.25", "0.25");
        when(orders.findByIdAndHotelId(1L, 9L)).thenReturn(Optional.of(order));

        assertThrows(ApiException.class,
                () -> service.changeStatus(9L, 1L, FoodOrder.Status.READY, false));
    }

    private static FoodOrder order(FoodOrder.Status status, String subtotal, String sgst, String cgst) {
        FoodOrder order = new FoodOrder();
        order.setStatus(status);
        order.setSubtotal(new BigDecimal(subtotal));
        order.setSgst(new BigDecimal(sgst));
        order.setCgst(new BigDecimal(cgst));
        order.setGrandTotal(new BigDecimal(subtotal).add(new BigDecimal(sgst)).add(new BigDecimal(cgst)));
        return order;
    }
}
