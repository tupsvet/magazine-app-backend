package com.magazines.domain.exception

class EmailAlreadyTakenException(email: String) :
    RuntimeException("Email already registered: $email")

class InvalidCredentialsException :
    RuntimeException("Invalid email or password")

class UserNotFoundException(id: String) :
    RuntimeException("User not found: $id")
