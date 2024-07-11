package com.vastidev.planner.activity;

import com.vastidev.planner.trip.Trip;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ActivityService {

    @Autowired
    private ActivityRepository repository;

    public ActivityResponse registerActivity(ActivityRequestPayload payload, Trip trip){
        Activity newActivity = new Activity(payload.title(), payload.occurs_at(), trip);
       this.repository.save(newActivity);
       return new ActivityResponse(newActivity.getId());
    }

    public List<ActivityData> getAllActivities(UUID id) {
        return this.repository.findByTripId(id).stream().map(activity -> new ActivityData(activity.getId(),activity.getTitle(),activity.getOccursAt())).toList();
    }
}
