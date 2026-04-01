package com.sanjay.auth.auth_app.dtos;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expireIn,
        String tokenType,
        UserDto userDto
) {
    public static TokenResponse of(String accessToken, String refreshToken, long expireIn, UserDto user){
        return new TokenResponse(accessToken,refreshToken,expireIn,"Bearer",user);
    }
}
