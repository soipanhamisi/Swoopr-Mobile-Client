# 1.1.1 Auth Module Endpoints

This document outlines the API endpoints for the authentication module. The base URL for all endpoints is `https://swooprserver-373496068484.europe-west1.run.app/auth`.

---

## 1.1.1.1 `POST /auth/getOtp`

**Description:** Sends a One-Time Password (OTP) to the provided email address for user verification.

**Input:**
*   **Method:** `POST`
*   **Content-Type:** `application/json`
*   **Body:**
    ```json
    {
      "email": "user@example.com"
    }
    ```
    *   `email` (string, required): The email address to which the OTP will be sent.

**Output:**
*   **Success (HTTP 200 OK):**
    ```
    OTP sent
    ```
*   **Error (HTTP 500 Internal Server Error):** (Implicit, if email sending fails or other server-side issues)
    ```
    // Error message indicating failure to send OTP
    ```

---

## 1.1.1.2 `POST /auth/authenticateUser`

**Description:** Authenticates a user by verifying the provided OTP against the email address.

**Input:**
*   **Method:** `POST`
*   **Content-Type:** `application/json`
*   **Body:**
    ```json
    {
      "otp": "123456",
      "email": "user@example.com"
    }
    ```
    *   `otp` (string, required): The OTP received by the user.
    *   `email` (string, required): The email address associated with the OTP.

**Output:**
*   **Success (HTTP 200 OK):**
    ```
    user authenticated
    ```
*   **Error (HTTP 401 Unauthorized):**
    ```
    user not authenticated
    ```

---

## 1.1.1.3 `POST /auth/registerUser`

**Description:** Registers a new user in the system. Upon successful registration, a JWT token is returned.

**Input:**
*   **Method:** `POST`
*   **Content-Type:** `application/json`
*   **Body:**
    ```json
    {
      "firstName": "John",
      "lastName": "Doe",
      "email": "john.doe@example.com",
      "password": "securepassword123",
      "phoneNumber": "+254712345678",
      "studentId": "USIU12345"
    }
    ```
    *   `firstName` (string, required): User's first name.
    *   `lastName` (string, required): User's last name.
    *   `email` (string, required): User's email address (must be a valid USIU email).
    *   `password` (string, required): User's chosen password.
    *   `phoneNumber` (string, required): User's phone number.
    *   `studentId` (string, required): User's USIU student ID.

**Output:**
*   **Success (HTTP 201 Created):**
    ```
    <JWT_TOKEN_STRING>
    ```
    *   Returns a JWT token string if registration is successful.
*   **Error (HTTP 400 Bad Request):**
    ```
    // Error message indicating why registration failed (e.g., "Email already registered", "Invalid USIU email format")
    ```

---

## 1.1.1.4 `POST /auth/login`

**Description:** Authenticates an existing user with their email and password, returning a JWT token upon successful login.

**Input:**
*   **Method:** `POST`
*   **Content-Type:** `application/json`
*   **Body:**
    ```json
    {
      "email": "user@example.com",
      "password": "securepassword123"
    }
    ```
    *   `email` (string, required): User's registered email address.
    *   `password` (string, required): User's password.

**Output:**
*   **Success (HTTP 200 OK):**
    ```
    <JWT_TOKEN_STRING>
    ```
    *   Returns a JWT token string if login is successful.
*   **Error (HTTP 401 Unauthorized):**
    ```
    // Error message indicating why login failed (e.g., "Invalid credentials", "User not found")
    ```

---

## 1.1.1.5 `POST /auth/testEndpoint`

**Description:** A test endpoint to validate a JWT token.

**Input:**
*   **Method:** `POST`
*   **Content-Type:** `application/json`
*   **Body:**
    ```json
    {
      "jwt": "YOUR_JWT_TOKEN",
      "message": "Hello, authenticated user!"
    }
    ```
    *   `jwt` (string, required): A valid JWT token.
    *   `message` (string, required): A sample message to be returned if the token is valid.

**Output:**
*   **Success (HTTP 200 OK):**
    ```
    Hello, authenticated user!
    ```
    *   Returns the `message` from the request body if the token is valid.
*   **Error (HTTP 401 Unauthorized):**
    ```
    Invalid token
    ```
