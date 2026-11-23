package vn.edu.tvu.vinhlongtour.content.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Một mục cấu hình trang web (khóa thuộc {@link SettingKey}, giá trị dạng chữ).
 */
@Entity
@Table(name = "site_settings")
public class SiteSetting {

    @Id
    @Column(name = "setting_key", length = 40)
    private String key;

    @Column(nullable = false, columnDefinition = "mediumtext")
    private String value;

    protected SiteSetting() {
    }

    public SiteSetting(SettingKey key, String value) {
        this.key = key.name();
        this.value = value;
    }

    public void change(String value) {
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
