package vn.edu.tvu.vinhlongtour.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

/**
 * Lớp cha của các danh mục chỉ có tên (khu vực, loại hình, dịch vụ).
 * Các lớp con kế thừa mã và tên, mỗi lớp con ứng với một bảng riêng trong CSDL.
 */
@MappedSuperclass
public abstract class NamedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    protected NamedItem() {
        // JPA cần constructor rỗng
    }

    protected NamedItem(String name) {
        rename(name);
    }

    public void rename(String name) {
        this.name = name.trim();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
