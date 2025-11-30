package ru.mirea.docsa2.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import ru.mirea.docsa2.event.CustomerValidationResponse;
import ru.mirea.docsa2.event.ValidateCustomerRequest;
import ru.mirea.docsa2.model.Customer;
import ru.mirea.docsa2.repository.CustomerRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerValidationConsumerTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private CustomerValidationConsumer consumer;

    @Captor
    private ArgumentCaptor<CustomerValidationResponse> responseCaptor;

    @Test
    void shouldValidateExistingCustomer() {
        ValidateCustomerRequest request = new ValidateCustomerRequest("corr-123", 1L);
        
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName("John Doe");

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        consumer.handleValidateCustomerRequest(request);

        verify(kafkaTemplate).send(eq("customer-validation-response"), eq("corr-123"), responseCaptor.capture());
        
        CustomerValidationResponse response = responseCaptor.getValue();
        assertThat(response.correlationId()).isEqualTo("corr-123");
        assertThat(response.customerId()).isEqualTo(1L);
        assertThat(response.valid()).isTrue();
        assertThat(response.customerName()).isEqualTo("John Doe");
        assertThat(response.errorMessage()).isNull();
    }

    @Test
    void shouldRejectNonExistingCustomer() {
        ValidateCustomerRequest request = new ValidateCustomerRequest("corr-456", 999L);

        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        consumer.handleValidateCustomerRequest(request);

        verify(kafkaTemplate).send(eq("customer-validation-response"), eq("corr-456"), responseCaptor.capture());
        
        CustomerValidationResponse response = responseCaptor.getValue();
        assertThat(response.correlationId()).isEqualTo("corr-456");
        assertThat(response.customerId()).isEqualTo(999L);
        assertThat(response.valid()).isFalse();
        assertThat(response.customerName()).isNull();
        assertThat(response.errorMessage()).isEqualTo("Customer not found");
    }
}

