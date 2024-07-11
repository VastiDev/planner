package com.vastidev.planner.trip;

import com.vastidev.planner.activity.ActivityData;
import com.vastidev.planner.activity.ActivityRequestPayload;
import com.vastidev.planner.activity.ActivityResponse;
import com.vastidev.planner.activity.ActivityService;
import com.vastidev.planner.link.*;
import com.vastidev.planner.participant.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/trips")
public class TripController {
    @Autowired
    private ParticipantService participantService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private LinkService linkService;

    @Autowired
    private TripRepository repository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<TripCreateResponse> createTrip(@RequestBody TripRequestPayload payload){
        Trip newTrip = new Trip(payload);
        this.repository.save(newTrip);

        this.participantService.registerParticipantsToEvent(payload.emails_to_invite(), newTrip);

        return ResponseEntity.ok().body(new TripCreateResponse(newTrip.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trip> getTripDetails(@PathVariable UUID id){
        Optional<Trip> trip = repository.findById(id);

        return trip.map(ResponseEntity::ok).orElseGet(()-> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Trip> updateTrip(@PathVariable UUID id, @RequestBody TripRequestPayload payload){
        Optional<Trip> trip = repository.findById(id);
        if (trip.isPresent()){
            Trip rowTrip = trip.get();
            rowTrip.setEndsAt(LocalDateTime.parse(payload.ends_at(), DateTimeFormatter.ISO_DATE_TIME));
            rowTrip.setStartsAt(LocalDateTime.parse(payload.starts_at(), DateTimeFormatter.ISO_DATE_TIME));
            rowTrip.setDestination(payload.destination());
            this.repository.save(rowTrip);

            return ResponseEntity.ok(rowTrip);
        }
        return ResponseEntity.notFound().build();
    }
    @GetMapping("/confirm/{id}")
    public ResponseEntity<Trip> updateTrip(@PathVariable UUID id){
        Optional<Trip> trip = repository.findById(id);
        if (trip.isPresent()){
            Trip rowTrip = trip.get();
            rowTrip.setIsConfirmed(true);

            this.repository.save(rowTrip);
            this.participantService.triggerConfirmationEmailToParticipants(id);

            return ResponseEntity.ok(rowTrip);
        }
        return ResponseEntity.notFound().build();
    }
    @PostMapping("/{id}/invite")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ParticipantCreateResponse> inviteParticipant(@PathVariable UUID id, @RequestBody ParticipantRequestPayload payload) {
        Optional<Trip> trip = repository.findById(id);

        if (trip.isPresent()) {
            Trip rowTrip = trip.get();

            ParticipantCreateResponse participantResponse = this.participantService.registerParticipantToEvent(payload.email(), rowTrip);
            if (rowTrip.getIsConfirmed()) {
                this.participantService.triggerConfirmationEmailToParticipant(payload.email());
            }

            return ResponseEntity.ok(participantResponse);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/listParticipants/{id}")
    public ResponseEntity<List<ParticipantData>> getAllParticipants (@PathVariable UUID id){
        List<ParticipantData> participantList = this.participantService.getAllParticipantsFromEvent(id);
        return ResponseEntity.ok(participantList);
    }

    @PostMapping("/{id}/activities")
    public ResponseEntity<ActivityResponse> registerActivity(@PathVariable UUID id, @RequestBody ActivityRequestPayload payload){
        Optional<Trip> trip = this.repository.findById(id);
        if(trip.isPresent()){
            Trip rawTrip  = trip.get();
            ActivityResponse activityResponse = this.activityService.registerActivity(payload, rawTrip);

            return ResponseEntity.ok(activityResponse);

        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/allActivities")
    public ResponseEntity<List<ActivityData>> getAllActivities (@PathVariable UUID id) {
        List<ActivityData> activitiesList = this.activityService.getAllActivities(id);
        return ResponseEntity.ok(activitiesList);
    }

    @PostMapping("/{id}/links")
    public ResponseEntity<LinkResponse> registerLink(@PathVariable UUID id, @RequestBody LinkRequestPayload payload) {
        Optional<Trip> trip = this.repository.findById(id);
        if (trip.isPresent()) {
            Trip rawTrip = trip.get();
            LinkResponse linkResponse = this.linkService.registerLink(payload, rawTrip);

            return ResponseEntity.ok(linkResponse);

        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/allLinks")
    public ResponseEntity<List<LinkData>> getAllLinks (@PathVariable UUID id) {
        List<LinkData> linksList = this.linkService.getAllLinks(id);
        return ResponseEntity.ok(linksList);
    }

}

