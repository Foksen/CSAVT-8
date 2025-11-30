package ru.mirea.docsa2.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import ru.mirea.docsa2.dto.CreateOrderRequest;
import ru.mirea.docsa2.dto.OrderResponse;
import ru.mirea.docsa2.event.CustomerValidationResponse;
import ru.mirea.docsa2.event.ProductValidationResponse;
import ru.mirea.docsa2.model.Order;
import ru.mirea.docsa2.producer.OrderEventProducer;
import ru.mirea.docsa2.repository.OrderRepository;
import ru.mirea.docsa2.service.ValidationService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(OrderController.class)
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private ValidationService validationService;

    @MockBean
    private OrderEventProducer orderEventProducer;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    @Test
    @WithMockUser
    void shouldGetAllOrders() throws Exception {
        Order order = new Order();
        order.setId(1L);
        order.setCustomerId(1L);
        order.setProductId(1L);
        order.setQuantity(5);
        order.setTotalPrice(BigDecimal.valueOf(100));
        order.setStatus(Order.OrderStatus.CONFIRMED);

        when(orderRepository.findAll()).thenReturn(List.of(order));

        mockMvc.perform(get("/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].id", is(1)))
            .andExpect(jsonPath("$[0].customerId", is(1)))
            .andExpect(jsonPath("$[0].productId", is(1)))
            .andExpect(jsonPath("$[0].quantity", is(5)));
    }

    @Test
    @WithMockUser
    void shouldGetOrderById() throws Exception {
        Order order = new Order();
        order.setId(1L);
        order.setCustomerId(1L);
        order.setProductId(1L);
        order.setQuantity(5);
        order.setTotalPrice(BigDecimal.valueOf(100));
        order.setStatus(Order.OrderStatus.CONFIRMED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/orders/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.customerId", is(1)))
            .andExpect(jsonPath("$.productId", is(1)))
            .andExpect(jsonPath("$.quantity", is(5)));
    }

    @Test
    @WithMockUser
    void shouldReturn404WhenOrderNotFound() throws Exception {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/orders/999"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldCreateOrderSuccessfully() throws Exception {
        ProductValidationResponse productResponse = new ProductValidationResponse(
            "corr-123", 1L, true, "Test Product", BigDecimal.valueOf(20), null
        );
        CustomerValidationResponse customerResponse = new CustomerValidationResponse(
            "corr-456", 1L, true, "John Doe", null
        );

        when(validationService.validateProduct(anyLong(), anyInt()))
            .thenReturn(CompletableFuture.completedFuture(productResponse));
        when(validationService.validateCustomer(anyLong()))
            .thenReturn(CompletableFuture.completedFuture(customerResponse));

        Order savedOrder = new Order();
        savedOrder.setId(1L);
        savedOrder.setCustomerId(1L);
        savedOrder.setProductId(1L);
        savedOrder.setQuantity(5);
        savedOrder.setTotalPrice(BigDecimal.valueOf(100));
        savedOrder.setStatus(Order.OrderStatus.CONFIRMED);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        String requestBody = """
            {
                "customerId": 1,
                "productId": 1,
                "quantity": 5
            }
            """;

        mockMvc.perform(post("/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(1)))
            .andExpect(jsonPath("$.customerId", is(1)))
            .andExpect(jsonPath("$.productId", is(1)))
            .andExpect(jsonPath("$.quantity", is(5)));

        verify(orderRepository).save(any(Order.class));
        verify(orderEventProducer).sendOrderCreatedEvent(any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldRejectOrderWhenProductNotFound() throws Exception {
        ProductValidationResponse productResponse = new ProductValidationResponse(
            "corr-123", 999L, false, null, null, "Product not found"
        );
        CustomerValidationResponse customerResponse = new CustomerValidationResponse(
            "corr-456", 1L, true, "John Doe", null
        );

        when(validationService.validateProduct(anyLong(), anyInt()))
            .thenReturn(CompletableFuture.completedFuture(productResponse));
        when(validationService.validateCustomer(anyLong()))
            .thenReturn(CompletableFuture.completedFuture(customerResponse));

        String requestBody = """
            {
                "customerId": 1,
                "productId": 999,
                "quantity": 5
            }
            """;

        mockMvc.perform(post("/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void shouldRejectOrderWhenCustomerNotFound() throws Exception {
        ProductValidationResponse productResponse = new ProductValidationResponse(
            "corr-123", 1L, true, "Test Product", BigDecimal.valueOf(20), null
        );
        CustomerValidationResponse customerResponse = new CustomerValidationResponse(
            "corr-456", 999L, false, null, "Customer not found"
        );

        when(validationService.validateProduct(anyLong(), anyInt()))
            .thenReturn(CompletableFuture.completedFuture(productResponse));
        when(validationService.validateCustomer(anyLong()))
            .thenReturn(CompletableFuture.completedFuture(customerResponse));

        String requestBody = """
            {
                "customerId": 999,
                "productId": 1,
                "quantity": 5
            }
            """;

        mockMvc.perform(post("/orders")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest());
    }
}

