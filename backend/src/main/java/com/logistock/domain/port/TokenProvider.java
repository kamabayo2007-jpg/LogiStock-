package com.logistock.domain.port;

import com.logistock.domain.model.User;

/** Port : génération d'un token d'authentification pour un utilisateur. */
public interface TokenProvider {

    String generate(User user);
}
