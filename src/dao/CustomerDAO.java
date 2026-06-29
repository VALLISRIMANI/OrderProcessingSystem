package src.dao;

import src.model.Customer;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CustomerDAO {

    private final DataSource dataSource;

    public CustomerDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void addCustomer(Customer customer) throws SQLException {
        String sql = "INSERT INTO customers (name, address, phone, email) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.getName());
            ps.setString(2, customer.getAddress());
            ps.setString(3, customer.getPhone());
            ps.setString(4, customer.getEmail());
            ps.executeUpdate();
        }
    }

    public List<Customer> getAllCustomers() throws SQLException {
        List<Customer> customers = new ArrayList<>();
        String sql = "SELECT * FROM customers ORDER BY customer_id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) customers.add(mapRow(rs));
        }
        return customers;
    }

    public Customer getCustomerById(int customerId) throws SQLException {
        String sql = "SELECT * FROM customers WHERE customer_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        return new Customer(
            rs.getInt("customer_id"), rs.getString("name"),
            rs.getString("address"), rs.getString("phone"),
            rs.getString("email")
        );
    }
}