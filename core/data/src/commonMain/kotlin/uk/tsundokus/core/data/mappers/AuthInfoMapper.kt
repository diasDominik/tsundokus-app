package uk.tsundokus.core.data.mappers

import uk.tsundokus.core.data.dto.AuthInfoSerializable
import uk.tsundokus.core.data.dto.UserSerializable
import uk.tsundokus.core.data.dto.UserTypeSerializable
import uk.tsundokus.core.domain.auth.AuthInfo
import uk.tsundokus.core.domain.auth.User
import uk.tsundokus.core.domain.auth.UserType

fun AuthInfoSerializable.toDomain(): AuthInfo {
    return AuthInfo(
        accessToken = accessToken,
        refreshToken = refreshToken,
        user = user.toDomain(),
    )
}

fun UserSerializable.toDomain(): User {
    return User(
        id = id,
        email = email,
        username = username,
        hasVerifiedEmail = hasVerifiedEmail,
        userType = userType.toDomain(),
    )
}

fun UserTypeSerializable.toDomain(): UserType {
    return when (this) {
        UserTypeSerializable.REGISTERED -> UserType.REGISTERED
        UserTypeSerializable.ANONYMOUS -> UserType.ANONYMOUS
    }
}

fun User.toSerializable(): UserSerializable {
    return UserSerializable(
        id = id,
        email = email,
        username = username,
        hasVerifiedEmail = hasVerifiedEmail,
        userType = userType.toSerializable(),
    )
}

fun AuthInfo.toSerializable(): AuthInfoSerializable {
    return AuthInfoSerializable(
        accessToken = accessToken,
        refreshToken = refreshToken,
        user = user.toSerializable(),
    )
}

fun UserType.toSerializable(): UserTypeSerializable {
    return when (this) {
        UserType.REGISTERED -> UserTypeSerializable.REGISTERED
        UserType.ANONYMOUS -> UserTypeSerializable.ANONYMOUS
    }
}
