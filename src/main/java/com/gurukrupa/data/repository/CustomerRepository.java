package com.gurukrupa.data.repository;

import com.gurukrupa.data.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByCity(String city);
    List<Customer> findByLastName(String lastName);
    List<Customer> findByFirstName(String lastName);
    // Custom query to find by mobile or alternative mobile
    List<Customer> findByMobileOrAlternativeMobile(String mobile, String alternativeMobile);
    Optional<Customer> findByMobile(String mobile);
    @Query("SELECT CONCAT(c.firstName, ' ', c.middleName, ' ', c.lastName) FROM Customer c")
    List<String> findAllCustomerFullNames();

    // Search by full name (unique combination)
    @Query("SELECT c FROM Customer c WHERE c.firstName = :firstName AND c.middleName = :middleName AND c.lastName = :lastName")
    Optional<Customer> searchByFullName(String firstName, String middleName, String lastName);

    // Search by full name and mobile number (unique combination)
    @Query("SELECT c FROM Customer c WHERE c.firstName = :firstName AND c.middleName = :middleName AND c.lastName = :lastName AND c.mobile = :mobile")
    Optional<Customer> searchByFullNameAndMobile(String firstName, String middleName, String lastName, String mobile);
}