# WebClient Documentation

`WebClient` is a generic, thread-safe utility class for interacting with the Swoopr API. It handles OkHttp setup, JSON serialization/deserialization with Gson, automatic JWT injection, and transparent token refreshing.

## Class Structure

### Core Components
- **`OkHttpClient`**: Configured with 30-second timeouts for connect, read, and write operations.
- **`Gson`**: Used for all JSON transformations.
- **`SecureTokenStore`**: Manages the storage and retrieval of JWT tokens.
- **`Handler` (MainLooper)**: Ensures all callbacks are executed on the Android main (UI) thread.

### Configuration
- **Base URL**: `https://swooprserver-373496068484.europe-west1.run.app`
- **Media Type**: `application/json; charset=utf-8`

### Inner Interface: `WebCallback<T>`
Defines the structure for handling API responses.
```java
public interface WebCallback<T> {
    void onResult(boolean success, String message, T data);
}
```

---

## Functions

### Constructor
- `WebClient(Context context)`: Initializes the client, token store, and main thread handler.

### Public API Methods

#### `post(String endpoint, Object requestData, Class<T> responseDataType, WebCallback<T> callback)`
Sends a `POST` request to the specified endpoint.
- **`endpoint`**: The API path (e.g., `"/auth/login"`).
- **`requestData`**: The object to be serialized into the JSON body.
- **`responseDataType`**: The expected class of the data returned in the `ApiResponse`.
- **`callback`**: Receives the result.

#### `post(String endpoint, Class<T> responseDataType, WebCallback<T> callback)`
Sends a `POST` request without a body.

#### `get(String endpoint, Class<T> responseDataType, WebCallback<T> callback)`
Sends a `GET` request to the specified endpoint.

### Internal Mechanisms
- **Authentication**: Automatically adds `Authorization: Bearer <token>` if a token is present in `SecureTokenStore`.
- **Token Refresh**: Intercepts `403 Forbidden` responses. If the error is token-related, it attempts a transparent refresh via `/auth/refreshToken` and retries the original request.
- **JWT Storage**: Automatically extracts and saves new tokens if found in the `Authorization` header of any response.
- **Logging**: Detailed logging of outbound requests and inbound responses (including headers and pretty-printed JSON) under the tags `outbound_json` and `inbound_json`.

---

## How to use it to make calls

### 1. Initialization
Create an instance of `WebClient` within your Activity, Fragment, or ViewModel.
```java
WebClient webClient = new WebClient(context);
```

### 2. Making a POST request
```java
LoginRequest loginData = new LoginRequest("user@example.com", "password123");

webClient.post("/auth/login", loginData, LoginResponse.class, new WebClient.WebCallback<LoginResponse>() {
    @Override
    public void onResult(boolean success, String message, LoginResponse data) {
        if (success) {
            // Handle successful login
        } else {
            // Show error message
        }
    }
});
```

### 3. Making a GET request
```java
webClient.get("/profile", UserProfile.class, (success, message, data) -> {
    if (success) {
        // Update UI with user data
    } else {
        // Handle error
    }
});
```
