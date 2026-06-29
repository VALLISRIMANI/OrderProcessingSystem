-- ==========================================================
-- Order Processing System - Database Schema
-- ==========================================================

CREATE DATABASE IF NOT EXISTS order_processing_system;
USE order_processing_system;

DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS items;

CREATE TABLE items (
    item_id        INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    price          DECIMAL(10,2) NOT NULL CHECK (price > 0),
    quantity       INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reorder_level  INT NOT NULL DEFAULT 0 CHECK (reorder_level >= 0)
);

CREATE TABLE customers (
    customer_id    INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    address        VARCHAR(255),
    phone          VARCHAR(20),
    email          VARCHAR(100)
);

CREATE TABLE orders (
    order_id       INT AUTO_INCREMENT PRIMARY KEY,
    customer_id    INT NOT NULL,
    order_date     DATE NOT NULL,
    total_amount   DECIMAL(10,2) NOT NULL DEFAULT 0,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- Junction table: one order can have many items (many-to-many)
CREATE TABLE order_items (
    order_item_id  INT AUTO_INCREMENT PRIMARY KEY,
    order_id       INT NOT NULL,
    item_id        INT NOT NULL,
    quantity       INT NOT NULL DEFAULT 1,
    price_at_order DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES items(item_id)
);

-- Sample seed data (optional, for quick testing)
INSERT INTO items (name, price, quantity, reorder_level) VALUES
('Laptop', 50000.00, 50, 5),
('Phone', 25000.00, 50, 5),
('Mouse', 400.00, 2, 10);

INSERT INTO customers (name, address, phone, email) VALUES
('Valli', 'Hyderabad', '9999999999', 'valli@gmail.com'),
('Bhavya', 'Vijayawada', '8888888888', 'bhavya@gmail.com');