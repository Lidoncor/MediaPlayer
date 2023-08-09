package com.player.player.dao;

import com.player.player.models.Video;
import com.player.player.models.Word;
import jakarta.persistence.EntityManager;

public class WordDao {
    private EntityManager entityManager;

    public WordDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void delete(Word word) {
        entityManager.getTransaction().begin();
        entityManager.createQuery("delete from Word w where w.id = :id")
                .setParameter("id", word.getId())
                .executeUpdate();
        entityManager.getTransaction().commit();
    }

    public Word findByName(String name) {
        return entityManager.createQuery("select w from Word w where w.name LIKE :name", Word.class)
                .setParameter("name", name).getSingleResult();
    }

    public void merge(Word word) {
        entityManager.getTransaction().begin();
        entityManager.merge(word);
        entityManager.getTransaction().commit();
    }
}
