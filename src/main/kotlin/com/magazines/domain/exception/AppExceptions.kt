package com.magazines.domain.exception

sealed class AppException(message: String) : RuntimeException(message)

class NotFoundException(message: String) : AppException(message)
class ForbiddenException(message: String) : AppException(message)
class BadRequestException(message: String) : AppException(message)
class ConflictException(message: String) : AppException(message)
class UnauthorizedException(message: String) : AppException(message)
