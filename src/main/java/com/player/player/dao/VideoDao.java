package com.player.player.dao;

import com.player.player.common.EntityManagerFactory;
import com.player.player.models.Video;
import jakarta.persistence.EntityManager;

import java.util.List;

public class VideoDao {
    private EntityManager entityManager;

    public VideoDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void delete(Video video) {
        entityManager.getTransaction().begin();
        entityManager.createQuery("delete from Video v where v.id = :id")
                .setParameter("id", video.getId())
                .executeUpdate();
        entityManager.getTransaction().commit();
    }

    public void merge(Video video) {
        entityManager.getTransaction().begin();
        entityManager.merge(video);
        entityManager.getTransaction().commit();
    }

    public void persist(Video video) {
        entityManager.getTransaction().begin();
        entityManager.persist(video);
        entityManager.getTransaction().commit();
    }

    public List<Video> selectAll() {
        entityManager.getTransaction().begin();
        entityManager.clear();
        List<Video> videos = entityManager.createQuery("select v from Video v").getResultList();
        entityManager.getTransaction().commit();
        return videos;
    }

    public Video findByNameAndPath(String videoPath, String videoName) {
        return entityManager.createQuery("select v from Video v where v.name LIKE :name and v.path LIKE :path", Video.class)
                .setParameter("name", videoName)
                .setParameter("path", videoPath)
                .getSingleResult();
    }
}
