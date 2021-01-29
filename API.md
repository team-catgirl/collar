# API

## Current server version

**Request**

`/api/version`

**Response**
```
{
    major: 1,
    minor: 1,
    patch: 1
}
```

### Discover supported API versions

Returns API versions supported by this collar server

**Request**

`GET /api/discover`

**Response**

`[1]`

### Test if server is operational

Used by the collar client and other API consumers to check server availability

**Request**

`GET /api/1/`

**Response**

```
{
    "status": "OK"
}
```

## Authentication and authorization

### Create a user

**Request**

```
POST /api/1/auth/create

{
    "name": "users display name",
    "email": "users email address",
    "password": "the users password",
    "confirmPassword: "used to check the password"
}
```

**Response**

```
{
    "profile" : {
        ...
    },
    "token" : "bearer token"
}
```

### Login

**Request**

```
POST /api/1/auth/login

{
    "username": "username",
    "password: "password",
}
```

***Response***

```
{
    "profile" : {
        ...
    },
    "token" : "bearer token"
}
```

### Authorize your requests

All requests must be sent with an `Authorization: Bearer <token>` with a token returned by `/api/1/auth/create`
 or `/api/1/auth/login`

## Profile

### Get a user profile

**Request**

`GET /api/1/profiles/<profile uuid>`

**Response**

```
{
    "id": "profile uuid",
    "name": "profiles display name"
}
```

### Get your profile

**Request**

`GET /api/1/profiles/me`

**Response**

```
{
    "id": "profile uuid",
    "name": "profiles display name",
    "email": "your email"
}
```

### List registered devices 

**Request**

`GET /api/1/profiles/me/devices`

**Response**

```
{
    "profileId": "uuid of the owner",
    "deviceId": "the device id",
    "publicKey: {
        ...
    }
}
```

### Delete registered device

**Request**

`GET /api/1/profiles/me/devices/<deviceId>`

**Response**

`{}`