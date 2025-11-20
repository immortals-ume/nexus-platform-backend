package com.example.customer;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

  private final CustomerService customerService;

  @PostMapping
  public ResponseEntity<String> createNewCustomer(
      @RequestBody @Valid CustomerRequest request
  ) {
    // Added a more descriptive variable name
    String newCustomerId = customerService.createCustomer(request);
    return ResponseEntity.ok(newCustomerId);
  }

  @PutMapping
  public ResponseEntity<Void> updateExistingCustomer(
      @RequestBody @Valid CustomerRequest request
  ) {
    customerService.updateCustomer(request);
    return ResponseEntity.accepted().build();
  }

  @GetMapping
  public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
    List<CustomerResponse> customers = customerService.findAllCustomers();
    return ResponseEntity.ok(customers);
  }

  @GetMapping("/exists/{customerId}")
  public ResponseEntity<Boolean> checkCustomerExists(
      @PathVariable("customerId") String customerId
  ) {
    Boolean exists = customerService.existsById(customerId);
    return ResponseEntity.ok(exists);
  }

  @GetMapping("/{customerId}")
  public ResponseEntity<CustomerResponse> getCustomerById(
      @PathVariable("customerId") String customerId
  ) {
    CustomerResponse customer = customerService.findById(customerId);
    return ResponseEntity.ok(customer);
  }

  @DeleteMapping("/{customerId}")
  public ResponseEntity<Void> deleteCustomer(
      @PathVariable("customerId") String customerId
  ) {
    customerService.deleteCustomer(customerId);
    return ResponseEntity.accepted().build();
  }
}