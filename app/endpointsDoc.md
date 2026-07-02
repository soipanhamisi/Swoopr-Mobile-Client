# 1.0.0 Authentication API

This document outlines the current authentication, registration, token management, and trip management endpoints exposed by the Swoopd server.

> **Important:** The current token verification code expects the raw JWT value in the `Authorization` header. It does not strip a `Bearer ` prefix.

---

## 1.0.1 `POST /auth/getOtp`

**Description:** Sends a One-Time Password (OTP) to the provided email address for user verification. This endpoint should be called before attempting to authenticate or register a user.

**Request:**
- **Method:** `POST`
- **Path:** `/auth/getOtp`
- **Content-Type:** `application/json`
- **Body:**
  ```json
  {
    "email": "user@example.com"
  }
  ```
  * `email` (String): The email address to which the OTP will be sent.

**Response:**
- **Status:** `200 OK`
- **Content-Type:** `text/plain`
- **Body:** `"OTP sent"`
- **Error Responses:**
  - `400 Bad Request`: If the email format is invalid or other issues prevent OTP generation.

---

## 1.0.2 `POST /auth/authenticateUser`

**Description:** Authenticates a user using their email and a received OTP. This endpoint verifies the OTP and confirms the user's identity.

**Request:**
- **Method:** `POST`
- **Path:** `/auth/authenticateUser`
- **Content-Type:** `application/json`
- **Body:**
  ```json
  {
    "email": "user@example.com",
    "otp": "123456"
  }
  ```
  * `email` (String): The user's email address.
  * `otp` (String): The OTP received by the user.

**Response:**
- **Status:** `200 OK`
- **Content-Type:** `text/plain`
- **Body:** `"user authenticated"`
- **Error Responses:**
  - `401 Unauthorized`: If the OTP is incorrect or has expired.
  - `401 Unauthorized`: `"user not authenticated"` when OTP verification fails.

---

## 1.0.3 `POST /auth/saveUser`

**Description:** Registers a new user in the system. Upon successful registration, a JWT token is returned in the `Authorization` header.

**Request:**
- **Method:** `POST`
- **Path:** `/auth/saveUser`
- **Content-Type:** `application/json`
- **Body:**
  ```json
  {
    "fullName": "John Doe",
    "email": "john.doe@example.com",
    "role": "STUDENT",
    "messagingToken": "optional-device-token"
  }
  ```
  * `fullName` (String): The user's full name.
  * `email` (String): The user's email address.
  * `role` (Role): The user's role in the system.
  * `messagingToken` (String): Optional device messaging token stored during registration.

**Response:**
- **Status:** `201 Created`
- **Content-Type:** `text/plain`
- **Headers:**
  - `Authorization`: `<jwt_token>` (The JWT token for the newly registered user)
- **Body:** `"success"`
- **Error Responses:**
  - `400 Bad Request`: If registration fails due to invalid data or other registration issues.

---

## 1.0.4 `POST /auth/getNewToken`

**Description:** Issues a new JWT token for an authenticated user using their email and a valid OTP. This is typically used after `getOtp` and `authenticateUser` to obtain a fresh token.

**Request:**
- **Method:** `POST`
- **Path:** `/auth/getNewToken`
- **Content-Type:** `application/json`
- **Body:**
  ```json
  {
    "email": "user@example.com",
    "otp": "123456"
  }
  ```
  * `email` (String): The user's email address.
  * `otp` (String): The OTP received by the user.

**Response:**
- **Status:** `200 OK`
- **Content-Type:** `text/plain`
- **Headers:**
  - `Authorization`: `<new_jwt_token>` (The new JWT token)
- **Body:** `"success"`
- **Error Responses:**
  - `401 Unauthorized`: If the OTP is incorrect or has expired, or the user is not authenticated.

---

## 1.0.5 `POST /auth/testEndpoint`

**Description:** A test endpoint to verify token validity and retrieve user information from the token. Requires a valid JWT in the Authorization header.

**Request:**
- **Method:** `POST`
- **Path:** `/auth/testEndpoint`
- **Content-Type:** `application/json`
- **Headers:**
  - `Authorization`: `<jwt_token>`
- **Body:** Not required.

**Response:**
- **Status:** `200 OK`
- **Content-Type:** `text/plain`
- **Body:** `"Hello! <user_email>"`
- **Error Responses:**
  - `401 Unauthorized`: If the provided JWT token is invalid or expired.

---

## 1.0.6 `POST /auth/submitMessagingToken`

**Description:** Submits or updates a user's Firebase Cloud Messaging (FCM) token. This token is used for sending push notifications to the user's device.

**Request:**
- **Method:** `POST`
- **Path:** `/auth/submitMessagingToken`
- **Content-Type:** `application/json`
- **Headers:**
  - `Authorization`: `<jwt_token>`
- **Body:** `"your_fcm_messaging_token_here"`
  * (String): The FCM token from the user's device.

**Response:**
- **Status:** `200 OK`
- **Content-Type:** `text/plain`
- **Body:** `"Messaging token submitted"`
- **Error Responses:**
  - `401 Unauthorized`: If the provided JWT token is invalid or expired.
  - `400 Bad Request`: If the messaging token is invalid or other submission issues occur.

---

# 1.1.0 Trip Management API

This section documents the trip management endpoints exposed by `TripManagementController` and the status codes returned by `TripManagementControllerExceptionHandlers`.

> **Important:** The controller currently expects a header named `jwt` for trip endpoints. The route for vehicle registration is spelled `regidterVehicle` in code, so the documented path below matches the current implementation exactly.

## 1.1.1 `POST /trips/regidterVehicle`

**Description:** Registers a vehicle for the authenticated user.

**Request:**
- **Method:** `POST`
- **Path:** `/trips/regidterVehicle`
- **Headers:**
  - `jwt`: `<jwt_token>`
- **Content-Type:** `application/json`
- **Body:**
  ```json
  {
    "regNo": "KDA 123A",
    "desc": "Blue Toyota Prius"
  }
  ```
  * `regNo` (String): Vehicle registration number.
  * `desc` (String): Vehicle description.

**Response:**
- **Status:** `200 OK`
- **Content-Type:** `text/plain`
- **Body:** `"Registered vehicle successfully"`

## 1.1.2 `POST /trips/createTrip`

**Description:** Creates a new trip for the authenticated host.

**Request:**
- **Method:** `POST`
- **Path:** `/trips/createTrip`
- **Headers:**
  - `jwt`: `<jwt_token>`
- **Content-Type:** `application/json`
- **Body:**
  ```json
  {
    "capacity": 3,
    "departureTime": "2026-07-01T09:30:00",
    "originDestinationCoordinates": {
      "origin": { "latitude": -1.2921, "longitude": 36.8219 },
      "destination": { "latitude": -1.2675, "longitude": 36.8113 }
    }
  }
  ```
  * `capacity` (int): Number of passengers that can join the trip.
  * `departureTime` (LocalDateTime): Scheduled departure time.
  * `originDestinationCoordinates` (OriginDestination): Trip route coordinates.

**Response:**
- **Status:** `201 Created`
- **Content-Type:** `text/plain`
- **Body:** `"Trip Created Successfully"`

**Error Responses:**
- `403 Forbidden`: Returned when `CannotCreateTripException` is thrown.

## 1.1.3 `POST /trips/cancelTrip`

**Description:** Cancels the current user's active trip.

**Request:**
- **Method:** `POST`
- **Path:** `/trips/cancelTrip`
- **Headers:**
  - `jwt`: `<jwt_token>`

**Response:**
- **Status:** `200 OK`
- **Content-Type:** `text/plain`
- **Body:** `"Trip Cancelled Successfully"`

**Error Responses:**
- `403 Forbidden`: Returned when `CannotCancelTripException` is thrown.

## 1.1.4 `POST /trips/joinCarPool`

**Description:** Matches the authenticated ride seeker to a trip and returns the updated trip record.

**Request:**
- **Method:** `POST`
- **Path:** `/trips/joinCarPool`
- **Headers:**
  - `jwt`: `<jwt_token>`
- **Content-Type:** `application/json`
- **Body:**
  ```json
  {
    "departureTime": "2026-07-01T09:30:00",
    "rsOriginDestination": {
      "origin": { "latitude": -1.2921, "longitude": 36.8219 },
      "destination": { "latitude": -1.2675, "longitude": 36.8113 }
    }
  }
  ```
  * `departureTime` (LocalDateTime): Desired departure time.
  * `rsOriginDestination` (OriginDestination): Ride-seeker route coordinates.

**Response:**
- **Status:** `200 OK`
- **Content-Type:** `application/json`
- **Body:** A serialized `Trip` object.

The returned `Trip` entity currently includes these fields:
- `tripId`
- `users`
- `vehicle`
- `tripCapacity`
- `tripStatus`
- `originDestination`
- `routePolyline`
- `departureTime`
- `createdBy`
- `destinationZone`

**Error Responses:**
- `100 Continue`: Returned when `NoAvailableTripException` is thrown.
