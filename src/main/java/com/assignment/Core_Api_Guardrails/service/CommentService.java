package com.assignment.Core_Api_Guardrails.service;

import com.assignment.Core_Api_Guardrails.dto.request.CommentRequest;
import com.assignment.Core_Api_Guardrails.dto.response.CommentResponse;
import com.assignment.Core_Api_Guardrails.entity.AuthorType;
import com.assignment.Core_Api_Guardrails.entity.Bot;
import com.assignment.Core_Api_Guardrails.entity.Comment;
import com.assignment.Core_Api_Guardrails.entity.Post;
import com.assignment.Core_Api_Guardrails.repository.BotRepo;
import com.assignment.Core_Api_Guardrails.repository.CommentRepo;
import com.assignment.Core_Api_Guardrails.repository.PostRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepo commentRepo;
    private final BotRepo botRepo;
    private final PostRepo postRepo;
    private final GuardrailService guardrailService;
    private final NotificationService notificationService;
    private final ViralityService viralityService;

    @Transactional
    public CommentResponse addComment(Long postId, CommentRequest request){
        Post post = postRepo.findById(postId).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND,"post not found"));

        if(request.getAuthorType()== AuthorType.BOT){
            return handleBotComment(post, request);
        }else{
            return handleHumanComment(post, request);
        }
    }

    private CommentResponse handleBotComment(Post post, CommentRequest request){
        Bot bot = botRepo.findById(request.getAuthorId()).orElseThrow(()->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Bot not found with id {}" + request.getAuthorId()));

        Long humanId = null;
        if(post.getAuthorType() == AuthorType.USER){
            humanId = post.getAuthorId();
        }

        if(humanId != null){
            guardrailService.enforceBotGuardrails
                    (post.getId(),bot.getId(),humanId, request.getDepthLevel());
        }else{
            guardrailService.enforceHorizontalAndVerticalCap(post.getId(), request.getDepthLevel());
        }

        Comment comment = Comment.builder().
                postId(post.getId()).
                authorId(bot.getId()).
                authorType(AuthorType.BOT).
                content(request.getContent()).
                depthLevel(request.getDepthLevel())
                .build();

        Comment saved = commentRepo.save(comment);
        log.info("bot {} commented on post {}",bot.getName(),post.getId());

        viralityService.addBotReply(post.getId());

        if(humanId != null){
            notificationService.handleBotNotification(humanId, bot.getName());
        }

        return mapToResponse(saved);
    }

    private CommentResponse handleHumanComment(Post post, CommentRequest request){
        Comment comment = Comment.builder().
                postId(post.getId()).
                authorId(request.getAuthorId()).
                authorType(AuthorType.USER).
                content(request.getContent()).
                depthLevel(request.getDepthLevel())
                .build();

        Comment saved = commentRepo.save(comment);
        log.info("Human {} commented on post {}", request.getAuthorId(), post.getId());

        viralityService.addHumanComment(post.getId());
        return mapToResponse(saved);
    }

    private CommentResponse mapToResponse(Comment comment){
        return CommentResponse.builder().
                id(comment.getId()).
                authorId(comment.getAuthorId()).
                postId(comment.getPostId()).
                authorType(comment.getAuthorType()).
                content(comment.getContent()).
                depthLevel(comment.getDepthLevel()).
                createdAt(comment.getCreatedAt()).
        build();
    }
}
