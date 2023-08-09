package com.player.player.dao;

import com.player.player.common.EntityManagerFactory;
import com.player.player.models.Translation;
import com.player.player.models.Word;
import jakarta.persistence.EntityManager;

public class TranslationDao {
    private EntityManager entityManager;

    public TranslationDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void deleteByTranslationIdAndWordId(Long translationId, Long wordId) {
        entityManager.getTransaction().begin();
        entityManager.createQuery("delete from Translation t where t.id = :tId and t.word.id = :wId")
                .setParameter("tId", translationId)
                .setParameter("wId", wordId)
                .executeUpdate();
        entityManager.getTransaction().commit();
    }

    public void deleteByTranslationNameAndWordId(String translationName, Long wordId) {
        entityManager.getTransaction().begin();
        entityManager.createQuery("delete from Translation t where t.name = :name and t.word.id = :id")
                .setParameter("name", translationName)
                .setParameter("id", wordId)
                .executeUpdate();
        entityManager.getTransaction().commit();
    }

    public Long isExistByTranslationNameAndWordName(String translationName, String wordName) {
        return entityManager.createQuery("select count(t) from Translation t where t.name LIKE :tName and t.word.name LIKE :wName", Long.class)
                .setParameter("tName", translationName)
                .setParameter("wName", wordName)
                .getSingleResult();
    }
}
