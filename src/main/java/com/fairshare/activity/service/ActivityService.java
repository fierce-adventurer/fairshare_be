package com.fairshare.activity.service;

import com.fairshare.activity.dto.ActivityEventResponse;
import com.fairshare.activity.model.ActivityEvent;
import com.fairshare.activity.repository.ActivityEventRepository;
import com.fairshare.group.service.GroupService;
import com.fairshare.realtime.service.EventBroadcaster;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ActivityService {

    private final ActivityEventRepository activityRepository;
    private final GroupService groupService;
    private final EventBroadcaster eventBroadcaster;

    public ActivityService(
            ActivityEventRepository activityRepository,
            GroupService groupService,
            EventBroadcaster eventBroadcaster
    ) {
        this.activityRepository = activityRepository;
        this.groupService = groupService;
        this.eventBroadcaster = eventBroadcaster;
    }

    @Transactional
    public ActivityEventResponse logEvent(
            UUID groupId,
            UUID actorId,
            String eventType,
            String targetType,
            UUID targetId,
            String payload
    ) {
        ActivityEvent event = new ActivityEvent(
                UUID.randomUUID(),
                groupId,
                actorId,
                eventType,
                targetType,
                targetId,
                payload
        );
        ActivityEvent saved = activityRepository.save(event);
        ActivityEventResponse response = ActivityEventResponse.from(saved);

        if (groupId != null) {
            eventBroadcaster.broadcastToGroup(groupId, eventType, response);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<ActivityEventResponse> listGroupActivity(UUID currentUserId, UUID groupId, int page, int size) {
        groupService.assertMembership(currentUserId, groupId);
        return activityRepository.findByGroupIdOrderByCreatedAtDesc(groupId, PageRequest.of(page, size))
                .stream()
                .map(ActivityEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ActivityEventResponse> listAllUserActivity(UUID currentUserId, int page, int size) {
        return activityRepository.findAllForUser(currentUserId, PageRequest.of(page, size))
                .stream()
                .map(ActivityEventResponse::from)
                .toList();
    }
}
