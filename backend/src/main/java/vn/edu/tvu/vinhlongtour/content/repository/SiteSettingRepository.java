package vn.edu.tvu.vinhlongtour.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.tvu.vinhlongtour.content.domain.SiteSetting;

/**
 * Truy vấn bảng cấu hình trang web ({@code site_settings}).
 */
public interface SiteSettingRepository extends JpaRepository<SiteSetting, String> {
}
