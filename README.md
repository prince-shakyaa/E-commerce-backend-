# E-Commerce Backend API

Complete e-commerce backend system built with Spring Boot 3, MongoDB, and Mock Payment Service.

## 🎯 Features

- ✅ Product Management (Create, List, Search)
- ✅ Shopping Cart (Add, View, Clear)
- ✅ Order Management (Create from cart, View details)
- ✅ Payment Processing (Mock service with webhook)
- ✅ Stock Management (Automatic updates)
- ✅ Complete Order Flow (Products → Cart → Order → Payment → Status Update)

## 🏗️ Architecture

```
E-Commerce API (Port 8080) ←→ Mock Payment Service (Port 8081)
           ↓
      MongoDB (Port 27017)
```

**Two Applications:**
1. **Main E-Commerce API** - Handles products, cart, orders, and payments
2. **Mock Payment Service** - Simulates payment processing and webhook callbacks

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- MongoDB (running on localhost:27017)

## 🚀 Setup Instructions

### 1. Install MongoDB

**macOS (using Homebrew):**
```bash
brew tap mongodb/brew
brew install mongodb-community
brew services start mongodb-community
```

**Verify MongoDB is running:**
```bash
mongosh
```

### 2. Clone and Build

```bash
cd /Users/princeshakya/Desktop/E-Commerce
```

**Build Main Application:**
```bash
mvn clean install
```

**Build Payment Service:**
```bash
cd payment-service
mvn clean install
cd ..
```

### 3. Run Applications

**Terminal 1 - Start Mock Payment Service:**
```bash
cd payment-service
mvn spring-boot:run
```

**Terminal 2 - Start Main E-Commerce API:**
```bash
mvn spring-boot:run
```

**Verify both services are running:**
- Main API: http://localhost:8080
- Payment Service: http://localhost:8081

## 📚 API Endpoints

### Product APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/products` | Create a new product |
| GET | `/api/products` | Get all products |
| GET | `/api/products/{id}` | Get product by ID |
| GET | `/api/products/search?q=laptop` | Search products (Bonus) |

### Cart APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/cart/add` | Add item to cart |
| GET | `/api/cart/{userId}` | Get user's cart |
| DELETE | `/api/cart/{userId}/clear` | Clear cart |

### Order APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders` | Create order from cart |
| GET | `/api/orders/{orderId}` | Get order details |
| GET | `/api/orders/user/{userId}` | Get user order history (Bonus) |
| POST | `/api/orders/{orderId}/cancel` | Cancel order if not paid (Bonus) |

### Payment APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/payments/create` | Create payment |
| POST | `/api/webhooks/payment` | Payment webhook (called by payment service) |

## 🧪 Testing with Postman

### Import Collection

1. Import `E-Commerce-API.postman_collection.json`
2. Collection includes all endpoints with sample requests
3. Variables: `userId`, `productId`, `orderId` are auto-populated

### Complete Order Flow Test

**Step 1: Create Products**
```
POST http://localhost:8080/api/products
```
```json
{
  "name": "Laptop",
  "description": "Gaming Laptop",
  "price": 50000.0,
  "stock": 10
}
```

**Step 2: Add to Cart**
```
POST http://localhost:8080/api/cart/add
```
```json
{
  "userId": "user123",
  "productId": "<productId from step 1>",
  "quantity": 2
}
```

**Step 3: View Cart**
```
GET http://localhost:8080/api/cart/user123
```

**Step 4: Create Order**
```
POST http://localhost:8080/api/orders
```
```json
{
  "userId": "user123"
}
```
*Note: Cart will be cleared, stock will be reduced*

**Step 5: Create Payment**
```
POST http://localhost:8080/api/payments/create
```
```json
{
  "orderId": "<orderId from step 4>",
  "amount": 100000.0
}
```

**Step 6: Wait 3 seconds for webhook**

Payment service will automatically:
- Wait 3 seconds (simulate processing)
- Call webhook: `POST http://localhost:8080/api/webhooks/payment`
- Update payment status (SUCCESS/FAILED)
- Update order status (PAID/FAILED)

**Step 7: Check Order Status**
```
GET http://localhost:8080/api/orders/<orderId>
```

## 📊 Sample Data

### Create Sample Products

```json
[
  {
    "name": "Laptop",
    "description": "Gaming Laptop",
    "price": 50000.0,
    "stock": 10
  },
  {
    "name": "Mouse",
    "description": "Wireless Mouse",
    "price": 1000.0,
    "stock": 50
  },
  {
    "name": "Keyboard",
    "description": "Mechanical Keyboard",
    "price": 3000.0,
    "stock": 30
  }
]
```

## 🔍 How It Works

### Order Creation Flow

1. **User adds items to cart** → Validates product exists & stock available
2. **User creates order** → 
   - Validates cart not empty
   - Checks all items in stock
   - Creates order with status `CREATED`
   - Creates order items (snapshot prices)
   - Reduces product stock
   - Clears cart
3. **User initiates payment** →
   - Validates order status is `CREATED`
   - Creates payment with status `PENDING`
   - Calls mock payment service
4. **Payment service processes** →
   - Waits 3 seconds
   - Sends webhook with status (SUCCESS/FAILED)
5. **Webhook updates** →
   - Updates payment status
   - Updates order status (PAID/FAILED)

### Payment Webhook Pattern

```
E-Commerce API                Mock Payment Service
      |                              |
      |---(1) Create Payment-------->|
      |<--(2) Payment Initiated------|
      |                              |
      |                         (Wait 3s)
      |                              |
      |<--(3) Webhook Callback------|
      |    (Payment Status)          |
      |                              |
   (Update Order Status)
```

## 🐛 Error Handling

- **Validation Errors** → 400 Bad Request with field errors
- **Resource Not Found** → 404 Not Found
- **Business Logic Errors** → 400 Bad Request with message
  - Empty cart
  - Insufficient stock
  - Invalid order status
- **Server Errors** → 500 Internal Server Error

## 📁 Project Structure

```
E-Commerce/
├── src/main/java/com/example/ecommerce/
│   ├── model/              # Entities (User, Product, Order, etc.)
│   ├── repository/         # MongoDB Repositories
│   ├── service/            # Business Logic
│   ├── controller/         # REST Controllers
│   ├── dto/                # Request/Response DTOs
│   ├── exception/          # Exception Handling
│   ├── config/             # Configuration
│   └── webhook/            # Webhook Controllers
├── payment-service/
│   └── src/main/java/com/example/paymentservice/
│       ├── controller/     # Mock Payment Controller
│       ├── dto/            # DTOs
│       └── config/         # Configuration
└── pom.xml
```

## ✅ Grading Checklist

- [x] Product APIs (15 points)
- [x] Cart APIs (20 points)
- [x] Order APIs (25 points)
- [x] Payment Integration (30 points)
- [x] Order Status Update (10 points)
- [x] Code Quality (10 points) - Clean code, proper structure
- [x] Postman Collection (10 points)


## 🎁 Bonus Features Implemented

1. **Order History** - `GET /api/orders/user/{userId}` - View all orders for a user (+5 points)
2. **Product Search** - `GET /api/products/search?q=<query>` - Search products by name (+5 points)
3. **Order Cancellation** - `POST /api/orders/{orderId}/cancel` - Cancel order if not paid, restore stock (+5 points)

## 🔧 Troubleshooting

### MongoDB Connection Error
```
Error: MongoSocketOpenException
```
**Solution:** Ensure MongoDB is running
```bash
brew services start mongodb-community
```

### Port Already in Use
```
Error: Port 8080 is already in use
```
**Solution:** Kill process or change port in `application.yaml`

### Payment Service Not Responding
```
Error calling payment service
```
**Solution:** Ensure payment service is running on port 8081

## 📝 Notes

- **Mock Payment Service**: 90% success rate (randomly fails 10% of time for testing)
- **Stock Management**: Automatic stock reduction on order creation
- **Cart Behavior**: Cart cleared automatically after order creation
- **Order Status**: `CREATED` → `PAID` (on success) or `FAILED` (on failure)
- **Payment Status**: `PENDING` → `SUCCESS` or `FAILED`

## 👨‍💻 Developer

Built for in-class assignment following Spring Boot best practices with clean architecture and proper error handling.
