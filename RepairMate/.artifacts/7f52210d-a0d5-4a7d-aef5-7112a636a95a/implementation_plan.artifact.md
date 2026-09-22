# Implementation Plan - RepairMate Features & Enhancements

This plan describes the implementation of role-based access control, the **Admin Dashboard**, the greyed-out **Laptop Repair Booking (Coming Soon)** feature, and additional domain extensions (Priority and Internal Notes) for client repairs.

## User Review Required

> [!IMPORTANT]
> - **Role-Based Access Control**: Users with an email ending in `@quickfix.co.za` will automatically be recognized as administrators. They will be directed to the dedicated Admin Dashboard upon login.
> - **Permissions & Safety**: The Admin Dashboard will focus entirely on managing customer-submitted repairs. It will **not** contain any creation features, hidden routes, or booking buttons, satisfying the restriction that administrators cannot create or book repairs.
> - **Firestore Rules**: We will update `firestore.rules` to permit read and update access to administrators matching the `@quickfix.co.za` domain, enforcing security on the backend.

## Proposed Changes

### Data and Domain Layer

#### [MODIFY] [Booking.kt](file:///C:/Users/Student/Downloads/RepairMate/app/src/main/java/za/co/quickfix/repairmate/data/local/Booking.kt)
- Add `priority: String = "Medium"` and `internalNotes: String = ""` to the `Booking` entity to enrich repair tracking.

#### [MODIFY] [BookingDao.kt](file:///C:/Users/Student/Downloads/RepairMate/app/src/main/java/za/co/quickfix/repairmate/data/local/BookingDao.kt)
- Add `observeAllBookings(): Flow<List<Booking>>` to fetch all customer requests for the administrator.

#### [MODIFY] [FirestoreModels.kt](file:///C:/Users/Student/Downloads/RepairMate/app/src/main/java/za/co/quickfix/repairmate/data/remote/FirestoreModels.kt)
- Make `where` nullable in `StructuredQuery` so it can support collection-wide scans for administrators.

#### [MODIFY] [FirestoreMapper.kt](file:///C:/Users/Student/Downloads/RepairMate/app/src/main/java/za/co/quickfix/repairmate/data/remote/FirestoreMapper.kt)
- Update `toFsDocument()` and `toBooking()` to map the new `priority` and `internalNotes` fields.
- Add `allBookingsQuery(): RunQueryRequest` for collection-wide fetching.

#### [MODIFY] [BookingRepository.kt](file:///C:/Users/Student/Downloads/RepairMate/app/src/main/java/za/co/quickfix/repairmate/data/repo/BookingRepository.kt)
- Update `refresh(userId: String)` or add an admin-aware switch: if the user is an admin, call `api.queryBookings` with `allBookingsQuery()` to update the local database with all client requests.
- Add `updateBookingDetails(booking: Booking): Boolean` to allow admins to sync priority and internal notes back to Firestore.

#### [MODIFY] [AuthRepository.kt](file:///C:/Users/Student/Downloads/RepairMate/app/src/main/java/za/co/quickfix/repairmate/data/auth/AuthRepository.kt)
- Add `val isAdmin: Boolean get() = email.endsWith("@quickfix.co.za")` to `AppUser`.

### Presentation / UI Layer

#### [MODIFY] [strings.xml](file:///C:/Users/Student/Downloads/RepairMate/app/src/main/res/values/strings.xml) & [values-zu/strings.xml](file:///C:/Users/Student/Downloads/RepairMate/app/src/main/res/values-zu/strings.xml)
- Add strings for the Admin Dashboard title, statistics labels, search/filter inputs, sorting, internal notes, and the "Coming Soon" laptop repair feature.

#### [MODIFY] [RepairMateRoot.kt](file:///C:/Users/Student/Downloads/RepairMate/app/src/main/java/za/co/quickfix/repairmate/ui/RepairMateRoot.kt)
- Update navigation so that if `user.isAdmin` is true, it renders the admin navigation flow, ensuring separate client and admin boundaries.

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/Student/Downloads/RepairMate/app/src/main/java/za/co/quickfix/repairmate/ui/booking/HomeScreen.kt)
- Add a prominent, greyed-out **Laptop Repair Booking** card labeled **"Coming Soon"** in the customer view, displaying a short message explaining the feature is coming soon and preventing click interactions.

#### [NEW] AdminDashboardScreen.kt
- Create a professional dashboard screen for administrators showing:
  - Summary statistics cards (Total, Pending, Active, Completed).
  - Search bar and filtering tabs by Status.
  - Sorting dropdown/controls (by Date, Status, or Priority).
  - List of all client-submitted repairs with clear status badges and priority labels.

#### [MODIFY] [BookingDetailScreen.kt](file:///C:/Users/Student/Downloads/RepairMate/app/src/main/java/za/co/quickfix/repairmate/ui/booking/BookingDetailScreen.kt)
- Enhancing detail screen to display Priority and a text field for Internal Notes + status updates if the logged-in user is an administrator.

### Security / Backend Config

#### [MODIFY] [firestore.rules](file:///C:/Users/Student/Downloads/RepairMate/firestore.rules)
- Update security rules to grant read/update privileges if `request.auth.token.email.endsWith('@quickfix.co.za')`.

## Verification Plan

### Automated Tests
- Run `./gradlew testDebugUnitTest` to make sure existing tests pass and no regression occurs.

### Manual Verification
- Deploy app to emulator.
- Log in as a client (e.g., `client@test.com`) and verify the **Laptop Repair Booking (Coming Soon)** greyed-out card is visible and disabled.
- Log in as an administrator (e.g., `manager@quickfix.co.za`) and verify they are directed to the **Admin Dashboard** with full filtering, sorting, internal notes editing, and statistics, with absolutely no creation button available.
