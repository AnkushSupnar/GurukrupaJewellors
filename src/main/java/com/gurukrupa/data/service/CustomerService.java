package com.gurukrupa.data.service;

import com.gurukrupa.data.entities.Customer;

import com.gurukrupa.data.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Autowired
    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer saveCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findById(id);
    }

    public void deleteCustomerById(Long id) {
        customerRepository.deleteById(id);
    }

    public boolean customerExists(Long id) {
        return customerRepository.existsById(id);
    }

    // New methods

    public List<Customer> findByFirstName(String firstName) {
        return customerRepository.findByFirstName(firstName);
    }

    public List<Customer> findByLastName(String lastName) {
        return customerRepository.findByLastName(lastName);
    }

    /**
     * Find customers with given mobile number matching either their main or alternative mobile.
     *
     * @param mobile the mobile number to search by
     * @return list of matching customers
     */
    public List<Customer> findByMobileOrAlternativeMobile(String mobile) {
        return customerRepository.findByMobileOrAlternativeMobile(mobile, mobile);
    }
    public Optional<Customer> findCustomerByMobile(String mobile) {
        return customerRepository.findByMobile(mobile);
    }
    public List<String> getAllCustomerFullNames() {
        return customerRepository.findAllCustomerFullNames();
    }

    public Optional<Customer>searchByFullName(String firstName, String middleName, String lastName){
        return customerRepository.searchByFullName(firstName,middleName,lastName);
    }

    public Optional<Customer>searchByFullNameAndMobile(String firstName, String middleName, String lastName, String mobile){
        return customerRepository.searchByFullNameAndMobile(firstName,middleName,lastName,mobile);
    }
    public Optional<Customer> findByMobile(String mobile){
        return customerRepository.findByMobile(mobile);
    }
    
    public List<Customer> searchByName(String name) {
        return customerRepository.searchByNameContaining(name);
    }
    
    public Optional<Customer> findByNameAndMobile(String name, String mobile) {
        return customerRepository.findByNameAndMobile(name, mobile);
    }
}
