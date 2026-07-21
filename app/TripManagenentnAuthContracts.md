# 1.1.1 Controller Endpoint Contracts

This document describes the request and response JSON shapes for the documented auth and trip-management endpoints.

## 1.1.1.1 Shared Response Envelope

Most endpoints return the shared `ApiResponse` record:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": null
}
```

Failure responses typically use the same shape with `success: false`.

```json
{
  "success": false,
  "message": "Reason for failure",
  "data": null
}
```

## 1.1.1.2 Auth Endpoints

### 1.1.1.2.1 `POST /auth/getOtp`

**Inbound JSON** (`EmailDTO`):

```json
{
  "email": "student@usiu.ac.ke"
}
```

**Outbound JSON on success**:

```json
{
  "success": true,
  "message": "Otp Sent",
  "data": null
}
```

### 1.1.1.2.2 `POST /auth/authenticateUser`

**Inbound JSON** (`EmailAuthCredsDTO`):

```json
{
  "email": "student@usiu.ac.ke",
  "otp": 123456
}
```

**Outbound JSON on success**:

```json
{
  "success": true,
  "message": "User Verified",
  "data": null
}
```

**Outbound JSON when OTP is wrong**:

```json
{
  "success": false,
  "message": "Wrong OTP",
  "data": null
}
```

### 1.1.1.2.3 `POST /auth/refreshToken`

**Inbound**:

- `Authorization: Bearer <token>` header

**Outbound JSON**:

```json
{
  "success": true,
  "message": "Refresh Token Generated",
  "data": null
}
```

A refreshed JWT is returned in the response header:

```http
Authorization: Bearer <new-jwt>
```
### 1.1.1.2.7 POST /auth/getNewToken
client must hit the`POST /auth/getOtp` first

Generates a new JWT using the provided email and OTP.

### Request Body

```json
{
  "email": "student@usiu.ac.ke",
  "otp": 123456
}
```

### Success Response

**Status:** `200 OK`

```json
{
  "success": true,
  "message": "Jwt Generated Successfully",
  "data": null
}
```
### Response Headers

The generated JWT is returned in the `Authorization` response header.

```http
Authorization: Bearer <jwt_token>
```


### 1.1.1.2.4 `POST /auth/saveUser`

**Inbound JSON** (`UserDTO`):

```json
{
  "fullName": "Jane Doe",
  "email": "student@usiu.ac.ke",
  "role": "NORMAL_USER",
  "messagingToken": "optional-fcm-token"
}
```

**Outbound JSON**:

```json
{
  "success": true,
  "message": "User saved",
  "data": null
}
```

A JWT is also returned in the `Authorization` response header:

```http
Authorization: Bearer <jwt>
```

### 1.1.1.2.5 `POST /auth/testEndpoint`

**Inbound**:

- `Authorization: Bearer <token>` header

**Outbound JSON**:

```json
{
  "success": true,
  "message": "Hello! student@usiu.ac.ke",
  "data": null
}
```

### 1.1.1.2.6 `POST /auth/submitMessagingToken`

**Inbound**:

- `Authorization: Bearer <token>` header
- Request body is a raw string token value. If sent as JSON, it should be a JSON string literal.

Example body:

```json
"fcm-token-value"
```

**Outbound JSON**:

```json
{
  "success": true,
  "message": "Messaging Token Submitted",
  "data": null
}
```

## 1.1.1.3 Trip Endpoints

### 1.1.1.3.1 `POST /trips/registerVehicle`

**Inbound JSON** (`VehicleDto`):

```json
{
  "regNo": "KAA 123A",
  "desc": "Silver Toyota Noah"
}
```

**Outbound JSON**:

```json
{
  "success": true,
  "message": "Vehicle registered",
  "data": null
}
```

### 1.1.1.3.2 `POST /trips/queryRegisteredVehicle`

**Inbound**:

- `Authorization: Bearer <token>` header

**Outbound JSON** is a bare array of `VehicleDto` objects, not an `ApiResponse` wrapper:

```json
[
  {
    "regNo": "KAA 123A",
    "desc": "Silver Toyota Noah"
  }
]
```

### 1.1.1.3.3 `POST /trips/createTrip`

**Inbound JSON** (`TripData`):

```json
{
  "capacity": 4,
  "departureTime": "2026-07-13T08:00:00",
  "originDestinationCoordinates": {
    "originLongitude": 36.807,
    "originLatitude": -1.283,
    "destinationLongitude": 36.812,
    "destinationLatitude": -1.3
  }
}
```

`departureTime` is serialized as an ISO-8601 `LocalDateTime` value.

**Outbound JSON**:

```json
{
  "success": true,
  "message": "Trip Created",
  "data": null
}
```

### 1.1.1.3.4 `POST /trips/cancelTrip`

**Inbound**:

- `Authorization: Bearer <token>` header

**Outbound JSON**:

```json
{
  "success": true,
  "message": "Trip Cancelled",
  "data": null
}
```

### 1.1.1.3.5 `POST /trips/joinCarpool`

**Inbound JSON** (`JoinCarpoolDto`):

```json
{
  "departureTime": "2026-07-13T08:00:00",
  "rsOriginDestination": {
    "originLongitude": 36.807,
    "originLatitude": -1.283,
    "destinationLongitude": 36.812,
    "destinationLatitude": -1.3
  }
}
```

**Outbound JSON**:

```json
{
  "success": true,
  "message": "Carpool joined successfully",
  "data": {
    "tripId": "uuid",
    "users": [
      {
        "userId": "uuid",
        "fullName": "Jane Doe",
        "email": "student@usiu.ac.ke",
        "role": "NORMAL_USER",
        "messagingToken": "optional-fcm-token"
      }
    ],
    "vehicle": {
      "vehicleId": "uuid",
      "user": {
        "userId": "uuid",
        "fullName": "Host Name",
        "email": "host@usiu.ac.ke",
        "role": "NORMAL_USER",
        "messagingToken": "optional-fcm-token"
      },
      "vehicleRegNumber": "KAA 123A",
      "vehicleDescription": "Silver Toyota Noah"
    },
    "tripCapacity": 3,
    "tripStatus": "OPEN",
    "originDestination": {
      "originLongitude": 36.807,
      "originLatitude": -1.283,
      "destinationLongitude": 36.812,
      "destinationLatitude": -1.3
    },
    "routePolyline": "encoded-polyline",
    "departureTime": "2026-07-13T08:00:00",
    "createdBy": "uuid",
    "destinationZone": "Westlands"
  }
}
```

## 1.1.1.4 Exception Response Contracts

### 1.1.1.4.1 `AuthControllerExceptionHandlers`

- `NoUserWithMatchingEmailException`
  - Current response body is a plain string, not an `ApiResponse` object.
  - Example:

  ```text
  "<error message>, Go to Registration"
  ```

- `InvalidEmailException`
  - `401 Unauthorized`
  - JSON:

  ```json
  {
    "success": false,
    "message": "<validation message>",
    "data": null
  }
  ```

- `UserExistsException`
  - `401 Unauthorized`
  - JSON:

  ```json
  {
    "success": false,
    "message": "<duplicate-user message>",
    "data": null
  }
  ```

### 1.1.1.4.2 `GlobalExceptionHandler`

- `TokenServiceException`
  - `403 Forbidden`
  - JSON:

  ```json
  {
    "success": false,
    "message": "<token-service message>",
    "data": null
  }
  ```

- `InvalidTokenException`
  - `403 Forbidden`
  - JSON:

  ```json
  {
    "success": false,
    "message": "<invalid-token message>",
    "data": null
  }
  ```

### 1.1.1.4.3 `TripManagementControllerExceptionHandlers`

- `CannotCreateTripException`
  - `403 Forbidden`
  - JSON:

  ```json
  {
    "success": false,
    "message": "<cannot-create-trip message>",
    "data": null
  }
  ```

- `CannotCancelTripException`
  - `403 Forbidden`
  - JSON:

  ```json
  {
    "success": false,
    "message": "<cannot-cancel-trip message>",
    "data": null
  }
  ```

- `NoAvailableTripException`
  - `202 Accepted`
  - JSON:

  ```json
  {
    "success": false,
    "message": "<no-available-trip message>",
    "data": null
  }
  ```

- `CannotCreateCarpoolRequestException`
  - `403 Forbidden`
  - JSON:

  ```json
  {
    "success": false,
    "message": "<cannot-create-carpool-request message>",
    "data": null
  }
  ```

- `GoogleMapsServiceUnavailableException`
  - `503 Service Unavailable`
  - JSON:

  ```json
  {
    "success": false,
    "message": "<google-maps-unavailable message>",
    "data": null
  }
  ```

