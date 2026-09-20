package com.smartrebook.booking.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "confirmation_number", nullable = false, unique = true, length = 30)
    private String confirmationNumber;

    // Lazy: the member is not needed on most booking reads (availability/change screens
    // only need the booking + items). Loaded explicitly via @EntityGraph where required.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 200)
    private String destination;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus status;

    // Optimistic locking: protects against two concurrent change operations racing on the
    // same booking. See RebookOrchestrator / the concurrent-change-in-progress test.
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Fetched explicitly with a JOIN FETCH query (BookingRepository.findWithItemsByConfirmationNumber)
    // rather than left EAGER, to avoid an accidental N+1 when listing bookings without needing items.
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BookingItem> items = new ArrayList<>();

    protected Booking() {
    }

    public Booking(String confirmationNumber, Member member, String destination,
                    LocalDate checkInDate, LocalDate checkOutDate, BigDecimal totalAmount,
                    BookingStatus status) {
        this.confirmationNumber = confirmationNumber;
        this.member = member;
        this.destination = destination;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addItem(BookingItem item) {
        items.add(item);
        item.setBooking(this);
    }

    public Long getId() {
        return id;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public Member getMember() {
        return member;
    }

    public String getDestination() {
        return destination;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<BookingItem> getItems() {
        return items;
    }
}
