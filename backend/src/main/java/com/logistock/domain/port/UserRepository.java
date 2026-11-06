package com.logistock.domain.port;

import com.logistock.domain.model.User;
import java.util.Optional;

/** Port : ce dont le métier a besoin pour retrouver un utilisateur. */
public interface UserRepository {

    Optional<User> findByEmail(String email);
}
