package com.immortals.platform.customer.entity;

import com.immortals.platform.customer.dto.response.Address;
import lombok.AllArgsConstructor;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Document
public class Customer {

  @Id
  private String id;
  private String firstname;
  private String lastname;
  private String email;
  private Address address;
}
