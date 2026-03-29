package com.sanjay.auth.auth_app.utills;

import java.util.UUID;

public class UserHelper {
    public static UUID parseUUID(String uuid){
        return UUID.fromString(uuid);
    }
}
