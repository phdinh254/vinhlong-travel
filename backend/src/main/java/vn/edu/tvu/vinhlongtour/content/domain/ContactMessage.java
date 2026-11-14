package vn.edu.tvu.vinhlongtour.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import vn.edu.tvu.vinhlongtour.auth.domain.Account;

import java.time.LocalDateTime;

/**
 * Tin nhắn khách gửi từ trang Liên hệ (xin tư vấn, hỏi về đơn...). Phải có số điện thoại hoặc email để gọi lại.
 */
@Entity
@Table(name = "contact_messages")
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(length = 15)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(length = 200)
    private String subject;

    @Column(nullable = false, length = 3000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContactStatus status = ContactStatus.NEW;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime handledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "handled_by")
    private Account handledBy;

    protected ContactMessage() {
    }

    public ContactMessage(String fullName, String phone, String email, String subject, String message, LocalDateTime now) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.subject = subject;
        this.message = message;
        this.createdAt = now;
    }

    /** Đánh dấu đã xử lý (ghi ai, lúc nào) hoặc đặt lại là mới. */
    public void markAs(ContactStatus status, Account admin, LocalDateTime now) {
        this.status = status;
        this.handledAt = status == ContactStatus.HANDLED ? now : null;
        this.handledBy = status == ContactStatus.HANDLED ? admin : null;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public ContactStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getHandledAt() {
        return handledAt;
    }

    public Account getHandledBy() {
        return handledBy;
    }
}
