package com.player.player.common;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Persistence;

public class EntityManagerFactory {
    static jakarta.persistence.EntityManagerFactory entityManagerFactory
            = Persistence.createEntityManagerFactory("MediaPlayer");

    public static EntityManager getEntityManager(){
        return entityManagerFactory.createEntityManager();
    }
}
