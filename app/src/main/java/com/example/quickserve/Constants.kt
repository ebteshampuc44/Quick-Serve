package com.example.quickserve

object Constants {
    // Realtime Database References
    const val WORKER_APPLICATIONS = "worker_applications"
    const val WORKERS = "workers"
    const val USERS = "users"
    const val REVIEWS = "reviews"

    // Firestore Collections
    const val FIRESTORE_BOOKINGS = "bookings"
    const val FIRESTORE_NOTIFICATIONS = "notifications"

    // Worker Application Status
    const val STATUS_PENDING = "pending"
    const val STATUS_APPROVED = "approved"
    const val STATUS_REJECTED = "rejected"

    // Worker Status
    const val WORKER_AVAILABLE = "available"
    const val WORKER_BUSY = "busy"
    const val WORKER_OFFLINE = "offline"

    // Booking Status
    const val BOOKING_PENDING = "pending"
    const val BOOKING_CONFIRMED = "confirmed"
    const val BOOKING_COMPLETED = "completed"
    const val BOOKING_CANCELLED = "cancelled"

    // Service Categories
    val SERVICE_CATEGORIES = listOf(
        "Plumber",
        "Electrician",
        "AC & Appliance",
        "Cleaning",
        "Carpenter",
        "Women Salon",
        "Men Salon",
        "Pest Control"
    )
}

data class WorkerApplication(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val serviceType: String = "",
    val experience: String = "",
    val location: String = "",
    val chargePerHour: Int = 0,
    val status: String = Constants.STATUS_PENDING,
    val appliedAt: Long = System.currentTimeMillis(),
    val approvedAt: Long = 0,
    val workerId: String = "",
    val profileImageBase64: String = ""
)

data class Worker(
    val workerId: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val serviceType: String = "",
    val experience: String = "",
    val location: String = "",
    val chargePerHour: Int = 0,
    val rating: Float = 0f,
    val totalReviews: Int = 0,
    val status: String = Constants.WORKER_AVAILABLE,
    val joinedAt: Long = System.currentTimeMillis(),
    val profileImageBase64: String = "",
    val isTopRated: Boolean = false
)

// Firestore Booking Data Class
data class FirestoreBooking(
    val bookingId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val serviceId: String = "",
    val serviceName: String = "",
    val providerId: String = "",
    val providerName: String = "",
    val providerPrice: Int = 0,
    val address: String = "",
    val fullAddress: String = "",
    val date: String = "",
    val time: String = "",
    val problemDescription: String = "",
    val status: String = Constants.BOOKING_PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// Notification Data Class
data class NotificationData(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val bookingId: String = "",
    val bookingStatus: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)