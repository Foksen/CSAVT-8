package ru.mirea.docsa2.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import ru.mirea.docsa2.event.ProductValidationResponse;
import ru.mirea.docsa2.event.ValidateProductRequest;
import ru.mirea.docsa2.model.Product;
import ru.mirea.docsa2.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductValidationConsumerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ProductValidationConsumer consumer;

    @Captor
    private ArgumentCaptor<ProductValidationResponse> responseCaptor;

    @Test
    void shouldValidateExistingProduct() {
        ValidateProductRequest request = new ValidateProductRequest("corr-123", 1L, 5);
        
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setPrice(BigDecimal.valueOf(50));
        product.setQuantity(10);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        consumer.handleValidateProductRequest(request);

        verify(kafkaTemplate).send(eq("product-validation-response"), eq("corr-123"), responseCaptor.capture());
        
        ProductValidationResponse response = responseCaptor.getValue();
        assertThat(response.correlationId()).isEqualTo("corr-123");
        assertThat(response.productId()).isEqualTo(1L);
        assertThat(response.valid()).isTrue();
        assertThat(response.productName()).isEqualTo("Test Product");
        assertThat(response.price()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(response.errorMessage()).isNull();
    }

    @Test
    void shouldRejectNonExistingProduct() {
        ValidateProductRequest request = new ValidateProductRequest("corr-456", 999L, 5);

        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        consumer.handleValidateProductRequest(request);

        verify(kafkaTemplate).send(eq("product-validation-response"), eq("corr-456"), responseCaptor.capture());
        
        ProductValidationResponse response = responseCaptor.getValue();
        assertThat(response.correlationId()).isEqualTo("corr-456");
        assertThat(response.productId()).isEqualTo(999L);
        assertThat(response.valid()).isFalse();
        assertThat(response.productName()).isNull();
        assertThat(response.price()).isNull();
        assertThat(response.errorMessage()).isEqualTo("Product not found");
    }
}

