package vn.edu.tvu.vinhlongtour.content.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.tvu.vinhlongtour.common.ApiException;
import vn.edu.tvu.vinhlongtour.content.domain.SettingKey;
import vn.edu.tvu.vinhlongtour.content.domain.SiteSetting;
import vn.edu.tvu.vinhlongtour.content.repository.SiteSettingRepository;

import java.util.EnumMap;
import java.util.Map;

/**
 * Cấu hình trang web (hotline, địa chỉ, mạng xã hội, các trang chính sách). Mục nào chưa nhập thì trả chuỗi rỗng,
 * để frontend luôn nhận đủ các khóa.
 */
@Service
@Transactional
public class SiteSettingService {

    static final int MAX_LENGTH = 50_000;

    private final SiteSettingRepository settings;

    public SiteSettingService(SiteSettingRepository settings) {
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public Map<SettingKey, String> all() {
        Map<SettingKey, String> result = new EnumMap<>(SettingKey.class);
        for (SettingKey key : SettingKey.values()) {
            result.put(key, "");
        }
        settings.findAll().forEach(s -> result.put(SettingKey.valueOf(s.getKey()), s.getValue()));
        return result;
    }

    /** Chỉ đổi các mục có trong yêu cầu; mục không gửi giữ nguyên. */
    public Map<SettingKey, String> update(Map<SettingKey, String> changes) {
        changes.forEach((key, raw) -> {
            String value = raw == null ? "" : raw.strip();
            if (value.length() > MAX_LENGTH) {
                throw ApiException.badRequest("SETTING_TOO_LONG", key + " dài quá " + MAX_LENGTH + " ký tự");
            }
            if (key.isLink() && !value.isEmpty() && !value.matches("^https?://\\S+$")) {
                throw ApiException.badRequest("INVALID_LINK", key + " phải là đường dẫn bắt đầu bằng http:// hoặc https://");
            }
            settings.findById(key.name()).ifPresentOrElse(s -> s.change(value), () -> settings.save(new SiteSetting(key, value)));
        });
        return all();
    }
}
