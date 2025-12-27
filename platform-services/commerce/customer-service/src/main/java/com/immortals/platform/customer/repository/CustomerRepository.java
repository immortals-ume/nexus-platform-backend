package com.immortals.platform.customer.repository;

import com.immortals.platform.customer.entity.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerRepository extends MongoRepository<Customer, String > {

}
