package vn.edu.tvu.vinhlongtour.content.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.tvu.vinhlongtour.content.domain.ContactMessage;
import vn.edu.tvu.vinhlongtour.content.domain.ContactStatus;

/**
 * Truy vấn bảng tin nhắn liên hệ ({@code contact_messages}).
 */
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    Page<ContactMessage> findByStatus(ContactStatus status, Pageable pageable);

    long countByStatus(ContactStatus status);
}
