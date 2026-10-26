package com.zapiee.restaurantservice.service;

import com.zapiee.restaurantservice.client.AuthClient;
import com.zapiee.restaurantservice.client.MediaClient;
import com.zapiee.restaurantservice.dto.ApiResponse;
import com.zapiee.restaurantservice.dto.Request.RestaurantRegistrationRequest;
import com.zapiee.restaurantservice.dto.Request.SubscriptionStatusUpdate;
import com.zapiee.restaurantservice.dto.Response.*;
import com.zapiee.restaurantservice.enums.DocumentType;
import com.zapiee.restaurantservice.enums.RestaurantStatus;
import com.zapiee.restaurantservice.enums.SubscriptionStatus;
import com.zapiee.restaurantservice.exception.*;
import com.zapiee.restaurantservice.model.Address;
import com.zapiee.restaurantservice.model.Restaurant;
import com.zapiee.restaurantservice.model.RestaurantDocument;
import com.zapiee.restaurantservice.repository.RestaurantDocumentRepository;
import com.zapiee.restaurantservice.repository.RestaurantRepository;
import feign.FeignException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantDocumentRepository restaurantDocumentRepository;
    private final AuthClient authClient;
    private final MediaClient mediaClient;


    @Transactional
    public RestaurantResponse registerRestaurant(Long ownerId, RestaurantRegistrationRequest request) {
        log.info("Restaurant registration started for ownerId={}", ownerId);

        getAndValidateOwner(ownerId);
        validateRestaurantRegistration(ownerId, request);

        Restaurant restaurant = Restaurant.builder()
                .ownerId(ownerId)
                .businessType(request.getBusinessType().trim())
                .businessName(request.getBusinessName().trim())
                .foodCategory(request.getFoodCategory())
                .ownerName(request.getOwnerName().trim())
                .businessContact(request.getBusinessContact().trim())
                .businessEmail(request.getBusinessEmail().trim().toLowerCase())
                .isActive(false)
                .restaurantStatus(RestaurantStatus.PENDING_REVIEW)
                .subscriptionStatus(SubscriptionStatus.PENDING)
                .build();

        Address address = Address.builder()
                .country(request.getCountry().trim())
                .state(request.getState().trim())
                .city(request.getCity().trim())
                .area(request.getArea().trim())
                .pincode(request.getPincode().trim())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .geofenceRadiusMeters(request.getGeofenceRadiusMeters())
                .restaurant(restaurant)
                .build();

        restaurant.setAddress(address);

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        authClient.assignRoleOwnerToUser(ownerId);

        log.info("Restaurant saved. restaurantId={}, ownerId={}",
                savedRestaurant.getId(), ownerId);

        return RestaurantResponse.builder()
                .id(savedRestaurant.getId())
                .ownerId(savedRestaurant.getOwnerId())
                .businessType(savedRestaurant.getBusinessType())
                .businessName(savedRestaurant.getBusinessName())
                .ownerName(savedRestaurant.getOwnerName())
                .businessContact(savedRestaurant.getBusinessContact())
                .businessEmail(savedRestaurant.getBusinessEmail())
                .logoUrl(savedRestaurant.getLogoUrl())
                .latitude(savedRestaurant.getAddress().getLatitude())
                .longitude(savedRestaurant.getAddress().getLongitude())
                .geofenceRadiusMeters(savedRestaurant.getAddress().getGeofenceRadiusMeters())
                .restaurantStatus(savedRestaurant.getRestaurantStatus())
                .subscriptionStatus(savedRestaurant.getSubscriptionStatus())
                .isActive(savedRestaurant.getIsActive())
                .createdAt(savedRestaurant.getCreatedAt())
                .build();
    }

    @Transactional
    public RestaurantResponse uploadDocument(Long ownerId, Long restaurantId,
                                 DocumentType documentType, MultipartFile file) {

        if (documentType == null) {
            throw new BadRequestException("Document type is required.");
        }

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select a document file.");
        }

        getAndValidateOwner(ownerId);

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Restaurant not found: " + restaurantId));

        if (!ownerId.equals(restaurant.getOwnerId())) {
            throw new BadRequestException(
                    "This restaurant does not belong to this owner.");
        }

        RestaurantDocument document = RestaurantDocument.builder()
                .restaurant(restaurant)
                .documentType(documentType)
                .fileUrl(null)
                .verified(false)
                .createdAt(LocalDateTime.now())
                .build();

        RestaurantDocument savedDocument = restaurantDocumentRepository.saveAndFlush(document);

        Long documentId = savedDocument.getId();

        if (documentId == null) {
            throw new IllegalStateException(
                    "Document ID was not generated.");
        }

        ApiResponse<String> mediaResponse =
                mediaClient.uploadIdentityDocument(
                        file, documentId,
                        ownerId, restaurantId
                );

        if (mediaResponse == null || mediaResponse.getData() == null
                || mediaResponse.getData().isBlank()) {
            throw new ServiceUnavailableException("Media Service did not return a document URL.");
        }

        String documentUrl = mediaResponse.getData().trim();

        savedDocument.setFileUrl(documentUrl);
        savedDocument.setUpdatedAt(LocalDateTime.now());

        restaurantDocumentRepository.save(savedDocument);

        return buildRestaurantResponse(restaurant);
    }

    @Transactional
    public RestaurantResponse changeRestaurantStatus(Long restaurantId, RestaurantStatus newStatus) {

        log.info("Changing restaurant status. restaurantId={}, newStatus={}",
                restaurantId, newStatus);

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Restaurant not found with id: " + restaurantId));

        restaurant.setRestaurantStatus(newStatus);

        if (newStatus == RestaurantStatus.APPROVED || newStatus == RestaurantStatus.ACTIVE) {
            restaurant.setIsActive(true);
        } else {
            restaurant.setIsActive(false);   // REJECTED, SUSPENDED, or DEACTIVATED
        }

        Restaurant updatedRestaurant = restaurantRepository.save(restaurant);

        log.info("Restaurant status updated. restaurantId={}, status={}, isActive={}",
                updatedRestaurant.getId(), updatedRestaurant.getRestaurantStatus(),
                updatedRestaurant.getIsActive());

        return buildRestaurantResponse(restaurant);
    }


    @Transactional(Transactional.TxType.SUPPORTS)
    public RestaurantResponse getRestaurantByOwnerId(Long ownerId) {
        log.info("Fetching restaurant for ownerId={}", ownerId);
        Restaurant restaurant = restaurantRepository.findByOwnerId(ownerId)
                .orElseThrow(() ->
                        new RestaurantNotFoundException("Restaurant not found."));

        return buildRestaurantResponse(restaurant);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public RestaurantResponse getRestaurantById(Long restaurantId) {
        log.info("Fetching restaurant by id={}", restaurantId);
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() ->
                        new RestaurantNotFoundException(
                                "Restaurant not found with id : " + restaurantId));
        return buildRestaurantResponse(restaurant);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public RestaurantInfoResponse restaurantById(Long restaurantId) {
        log.info("Fetching restaurant by id={}", restaurantId);
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(
                                "Restaurant not found with id : " + restaurantId));
        return getRestaurantResponse(restaurant);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<RestaurantResponse> getAllActiveRestaurants() {
        log.info("Fetching all active restaurants.");
        return restaurantRepository.findByIsActiveTrue()
                .stream()
                .map(this::buildRestaurantResponse)
                .toList();
    }


    @Transactional(Transactional.TxType.SUPPORTS)
    public List<RestaurantResponse> getAllInactiveRestaurants() {
        log.info("Fetching all inactive restaurants.");
        return restaurantRepository.findByIsActiveFalse()
                .stream()
                .map(this::buildRestaurantResponse)
                .toList();
    }


    @Transactional()
    public RestaurantStatusInfo getRestaurantStatus(Long restaurantId) {

        if (restaurantId == null) {
            throw new BadRequestException("Restaurant ID is required.");
        }

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(
                                "Restaurant not found with id: " + restaurantId));

        return RestaurantStatusInfo.builder()
                .restaurantId(restaurant.getId())
                .status(RestaurantStatusInfo.Status.valueOf(
                        restaurant.getRestaurantStatus().name()))
                .build();
    }


    @Transactional(Transactional.TxType.SUPPORTS)
    public List<RestaurantResponse> getAllRestaurants() {
        log.info("Fetching all restaurants.");
        return restaurantRepository.findAll()
                .stream()
                .map(this::buildRestaurantResponse)
                .toList();
    }

    public void activateRestaurant(Long restaurantId) {
        log.info("Activating restaurant={}", restaurantId);
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() ->
                        new RestaurantNotFoundException(
                                "Restaurant not found with id : " + restaurantId));

        validateRestaurantActivation(restaurant);
        restaurant.setIsActive(true);
        restaurantRepository.save(restaurant);
        log.info("Restaurant activated successfully.");
    }

    public void deactivateRestaurant(Long restaurantId) {
        log.info("Deactivating restaurant={}", restaurantId);
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() ->
                        new RestaurantNotFoundException(
                                "Restaurant not found with id : " + restaurantId));
        restaurant.setIsActive(false);
        restaurantRepository.save(restaurant);
        log.info("Restaurant deactivated.");
    }


    public void updateSubscriptionStatus(Long restaurantId, SubscriptionStatusUpdate request) {
        log.info("Updating subscription status for restaurant={}", restaurantId);
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() ->
                        new RestaurantNotFoundException(
                                "Restaurant not found with id : " + restaurantId));

        if (request.getStatus().equals(SubscriptionStatus.ACTIVE)){
            restaurant.setActivePlanId(request.getActivePlanId());
            restaurant.setActiveSubscriptionId(request.getActiveSubscriptionId());
            restaurant.setIsActive(true);
            restaurant.setIsSubscriptionActive(true);
            restaurant.setSubscriptionEndEpoch(request.getSubscriptionEndEpoch());
            restaurant.setSubscriptionStatus(SubscriptionStatus.ACTIVE);

            restaurantRepository.save(restaurant);
            return;
        }

        restaurant.setIsSubscriptionActive(false);
        restaurant.setSubscriptionStatus(request.getStatus());
        restaurantRepository.save(restaurant);
        log.info("Subscription updated successfully.");
    }



    public void updateRazorpayCustomerId(Long restaurantId, String razorpayCustomerId) {
        log.info("Updating Razorpay customer id for restaurant={}", restaurantId);
        if (!StringUtils.hasText(razorpayCustomerId)) {
            throw new BadRequestException("Razorpay customer id is required.");
        }

        Restaurant restaurant = getRestaurantEntity(restaurantId);
        restaurant.setRazorpayCustomerId(razorpayCustomerId);
        restaurantRepository.save(restaurant);
    }



    private void validateRestaurantRegistration(Long ownerId, RestaurantRegistrationRequest request) {

        if (ownerId == null) {
            throw new IllegalArgumentException("Invalid authenticated user.");
        }

        if (request == null) {
            throw new BadRequestException(
                    "Restaurant registration request is required.");
        }

        if (restaurantRepository.existsByOwnerId(ownerId)) {
            log.warn("Owner {} already has a restaurant.", ownerId);
            throw new RestaurantAlreadyExistsException(
                    "You have already registered a restaurant."
            );
        }

        String email = request.getBusinessEmail().trim().toLowerCase();
        if (restaurantRepository.existsByBusinessEmail(email)) {
            log.warn("Business email already exists: {}", email);
            throw new RestaurantAlreadyExistsException(
                    "Business email already exists."
            );
        }

        String contact = request.getBusinessContact().trim();
        if (restaurantRepository.existsByBusinessContact(contact)) {
            log.warn("Business contact already exists: {}", contact);
            throw new RestaurantAlreadyExistsException(
                    "Business contact already exists."
            );
        }

        validateRequest(request);
    }

    private void validateRestaurantActivation(Restaurant restaurant) {

        if (Boolean.TRUE.equals(restaurant.getIsActive())) {
            throw new BadRequestException("Restaurant is already active.");
        }

//        if (!Boolean.TRUE.equals(restaurant.getIsSubscriptionActive())) {
//            throw new BadRequestException(
//                    "Subscription must be active before activating restaurant."
//            );
//        }

    }


    private void validateRequest(RestaurantRegistrationRequest request) {

        if (!StringUtils.hasText(request.getBusinessName())) {
            throw new BadRequestException("Business name is required.");
        }

        if (!StringUtils.hasText(request.getBusinessType())) {
            throw new BadRequestException("Business type is required.");
        }

        if (!StringUtils.hasText(request.getOwnerName())) {
            throw new BadRequestException("Owner name is required.");
        }

        if (!StringUtils.hasText(request.getCountry())) {
            throw new BadRequestException("Country is required.");
        }

        if (!StringUtils.hasText(request.getState())) {
            throw new BadRequestException("State is required.");
        }

        if (!StringUtils.hasText(request.getCity())) {
            throw new BadRequestException("City is required.");
        }

        if (!StringUtils.hasText(request.getPincode())) {
            throw new BadRequestException("PinCode is required.");
        }
    }


    private RestaurantResponse buildRestaurantResponse(Restaurant restaurant) {
        Address address = restaurant.getAddress();
        return RestaurantResponse.builder()
                .id(restaurant.getId())
                .ownerId(restaurant.getOwnerId())
                .businessType(restaurant.getBusinessType())
                .businessName(restaurant.getBusinessName())
                .ownerName(restaurant.getOwnerName())
                .businessContact(restaurant.getBusinessContact())
                .businessEmail(restaurant.getBusinessEmail())
                .logoUrl(restaurant.getLogoUrl())
                .restaurantStatus(restaurant.getRestaurantStatus())
                .subscriptionStatus(restaurant.getSubscriptionStatus())
                .restaurantStatus(restaurant.getRestaurantStatus())
                .isActive(restaurant.getIsActive())
                .country(address != null ? address.getCountry() : null)
                .state(address != null ? address.getState() : null)
                .city(address != null ? address.getCity() : null)
                .area(address != null ? address.getArea() : null)
                .pincode(address != null ? address.getPincode() : null)
                .latitude(address != null ? address.getLatitude() : null)
                .longitude(address != null ? address.getLongitude() : null)
                .geofenceRadiusMeters(address != null ? address.getGeofenceRadiusMeters() : null)
                .createdAt(restaurant.getCreatedAt())
                .updatedAt(restaurant.getUpdatedAt())
                .build();
    }

    private RestaurantInfoResponse getRestaurantResponse(Restaurant restaurant) {
        Address address = restaurant.getAddress();
        return RestaurantInfoResponse.builder()
                .restaurantId(restaurant.getId())
                .latitude(address != null ? address.getLatitude() : null)
                .longitude(address != null ? address.getLongitude() : null)
                .build();
    }

    private Restaurant getRestaurantEntity(Long restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() ->
                        new RestaurantNotFoundException(
                                "Restaurant not found with id : " + restaurantId));
    }

    private void getAndValidateOwner(Long ownerId) {

        try {
            UserInfo user = authClient.getUserById(ownerId).getData();

            if (ownerId == null || ownerId <= 0) {
                throw new BadRequestException("A valid owner ID is required.");
            }

            if (user == null) {
                throw new ResourceNotFoundException("User not found with id: " + ownerId);
            }

            if (user.getId() == null) {
                throw new ResourceNotFoundException("Invalid user information received from Auth Service.");
            }

            if (!user.getId().equals(ownerId)) {
                throw new BadRequestException("Authenticated user information is invalid.");
            }

            log.info("User verified successfully from Auth Service. userId={}", ownerId);

        } catch (FeignException.NotFound ex) {
            log.warn("User not found in Auth Service. userId={}", ownerId);
            throw new ResourceNotFoundException("User not found with id: " + ownerId);

        } catch (FeignException ex) {
            log.error("Auth Service communication failed for userId={}", ownerId, ex);
            throw new ServiceUnavailableException("Auth Service is currently unavailable.");
        }
    }


}