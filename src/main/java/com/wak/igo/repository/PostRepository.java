package com.wak.igo.repository;

import com.wak.igo.domain.Member;
import com.wak.igo.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository  extends JpaRepository<Post,Long> {
    List<Post> findAllByOrderByCreatedAtDesc();
    Page<Post> findAllByOrderByCreatedAtDesc(PageRequest pageRequest); //최신순 정렬
    Page<Post> findAllByOrderByViewCountDesc(PageRequest pageRequest); //조회수순 정렬
    Page<Post> findAllByOrderByHeartNumDesc(PageRequest pageRequest); //좋아요순 정렬
    List<Post> findByMember(Member member);
    @Query(value = "select p from Post p where p.title LIKE %:content% OR p.content LIKE %:content%")
    List<Post> findByContent(String content);//입력받은 String으로 db에서 title,contnet,tags 기준으로 검색
    void deleteAllByMember(Member member);

    
}