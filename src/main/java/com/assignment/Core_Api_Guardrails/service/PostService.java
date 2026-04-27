package com.assignment.Core_Api_Guardrails.service;

import com.assignment.Core_Api_Guardrails.dto.request.PostRequest;
import com.assignment.Core_Api_Guardrails.dto.response.PostResponse;
import com.assignment.Core_Api_Guardrails.entity.Post;
import com.assignment.Core_Api_Guardrails.repository.PostRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepo postRepo;
    private final ViralityService viralityService;

    @Transactional
    public PostResponse createPost(PostRequest request){
        Post post = Post.builder().
                authorId(request.getAuthorId()).
                authorType(request.getAuthorType()).
                content(request.getContent()).
                build();

        Post saved = postRepo.save(post);
        log.info("post created and saved");
        return mapToResponse(saved);
    }

    @Transactional
    public void likePost(Long postId){
        postRepo.findById(postId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,"post dosent exist with id {}"+ postId));

        viralityService.addHumanLike(postId);
        log.info("Post {} liked — virality +20", postId);
    }

    private PostResponse mapToResponse(Post post){
        return PostResponse.builder().
                id(post.getId()).
                authorId(post.getAuthorId()).
                authorType(post.getAuthorType()).
                content(post.getContent()).
                createdAt(post.getCreatedAt()).
        build();
    }
}
