package vn.edu.tvu.vinhlongtour.content.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.auth.repository.AccountRepository;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.common.PageResponse;
import vn.edu.tvu.vinhlongtour.content.domain.ContactMessage;
import vn.edu.tvu.vinhlongtour.content.domain.ContactStatus;
import vn.edu.tvu.vinhlongtour.content.repository.ContactMessageRepository;
import vn.edu.tvu.vinhlongtour.content.web.ContentDtos.ContactMessageResponse;
import vn.edu.tvu.vinhlongtour.content.web.ContentDtos.ContactReceived;
import vn.edu.tvu.vinhlongtour.content.web.ContentDtos.ContactRequest;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Tin nhắn liên hệ: khách gửi từ trang Liên hệ, admin xem và đánh dấu đã xử lý.
 * shortcut: chưa giới hạn số lần gửi theo IP, ai cũng gửi liên tục được; thêm giới hạn hoặc captcha khi chạy thật.
 */
@Service
@Transactional
public class ContactService {

    private final ContactMessageRepository messages;
    private final AccountRepository accounts;
    private final Clock clock;

    public ContactService(ContactMessageRepository messages, AccountRepository accounts, Clock clock) {
        this.messages = messages;
        this.accounts = accounts;
        this.clock = clock;
    }

    public ContactReceived submit(ContactRequest r) {
        String phone = blankToNull(r.phone());
        String email = blankToNull(r.email());
        if (phone == null && email == null) {
            throw ApiException.badRequest("CONTACT_REQUIRED", "Cần số điện thoại hoặc email để gọi lại");
        }
        ContactMessage m = messages.save(new ContactMessage(r.fullName().trim(), phone, email, blankToNull(r.subject()),
                r.message().trim(), LocalDateTime.now(clock)));
        return new ContactReceived(m.getId(), m.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public PageResponse<ContactMessageResponse> list(ContactStatus status, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = status == null ? messages.findAll(pageable) : messages.findByStatus(status, pageable);
        return PageResponse.of(result.map(ContactMessageResponse::from));
    }

    public ContactMessageResponse changeStatus(Long id, ContactStatus status, Long adminId) {
        ContactMessage m = messages.findById(id)
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND", "Không tìm thấy tin nhắn liên hệ"));
        m.markAs(status, accounts.getReferenceById(adminId), LocalDateTime.now(clock));
        return ContactMessageResponse.from(m);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
