package com.assignment.Core_Api_Guardrails.controller;

import com.assignment.Core_Api_Guardrails.dto.request.CommentRequest;
import com.assignment.Core_Api_Guardrails.dto.request.LikeRequest;
import com.assignment.Core_Api_Guardrails.dto.request.PostRequest;
import com.assignment.Core_Api_Guardrails.dto.response.CommentResponse;
import com.assignment.Core_Api_Guardrails.dto.response.PostResponse;
import com.assignment.Core_Api_Guardrails.service.CommentService;
import com.assignment.Core_Api_Guardrails.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostService postService;
    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody PostRequest request){
        PostResponse response = postService.createPost(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request){
        CommentResponse response = commentService.addComment(postId,request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<String> likePosts(@PathVariable Long postId){
        postService.likePost(postId);
        return ResponseEntity.ok("post liked successfully");
    }
}
