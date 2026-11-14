package vn.edu.tvu.vinhlongtour.catalog.domain;

import org.junit.jupiter.api.Test;
import vn.edu.tvu.vinhlongtour.common.ApiException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm tra các quy tắc nghiệp vụ nằm trong lớp domain của danh mục, không cần CSDL.
 */
class CatalogDomainTest {

    // ---------- Khoảng tuổi ----------

    @Test
    void ageRangesSharingAnAgeOverlap() {
        AgeRange child = new AgeRange(5, 11);

        assertThat(child.overlaps(new AgeRange(11, 20))).isTrue();   // chung tuổi 11
        assertThat(child.overlaps(new AgeRange(0, 4))).isFalse();    // sát nhau nhưng không chung tuổi nào
        assertThat(child.overlaps(new AgeRange(6, 7))).isTrue();     // nằm gọn bên trong
        assertThat(child.contains(11)).isTrue();
        assertThat(child.contains(12)).isFalse();
    }

    @Test
    void ageRangeWithMinAboveMaxIsRejected() {
        assertCode(() -> new AgeRange(12, 5), "INVALID_AGE_RANGE");
        assertCode(() -> new AgeRange(-1, 5), "INVALID_AGE_RANGE");
    }

    // ---------- Phụ thu: đa hình theo cách tính ----------

    @Test
    void eachSurchargeMethodCountsQuantityItsOwnWay() {
        assertThat(SurchargeMethod.PER_PERSON.quantity(4, null)).isEqualTo(4);
        assertThat(SurchargeMethod.PER_BOOKING.quantity(4, null)).isEqualTo(1);
        assertThat(SurchargeMethod.PER_UNIT.quantity(4, 2)).isEqualTo(2);
        assertCode(() -> SurchargeMethod.PER_UNIT.quantity(4, 0), "INVALID_SURCHARGE_QUANTITY");
    }

    @Test
    void surchargeAmountIsUnitPriceTimesQuantity() {
        Surcharge insurance = new Surcharge("Bảo hiểm", SurchargeMethod.PER_PERSON, 20_000, null);

        assertThat(insurance.amountFor(3, null)).isEqualTo(60_000);
    }

    // ---------- Chính sách hủy ----------

    private static CancellationPolicy policyFrom(LocalDate effectiveFrom) {
        return new CancellationPolicy("Chính sách 2026", effectiveFrom, null, List.of(
                new PenaltyTier(0, 2, 100),
                new PenaltyTier(3, 6, 50),
                new PenaltyTier(7, 14, 20)));
    }

    @Test
    void penaltyFollowsTheTierCoveringDaysBeforeDeparture() {
        CancellationPolicy policy = policyFrom(LocalDate.of(2026, 1, 1));

        assertThat(policy.penaltyPercentFor(1)).isEqualTo(100);
        assertThat(policy.penaltyPercentFor(3)).isEqualTo(50);
        assertThat(policy.penaltyPercentFor(14)).isEqualTo(20);
        assertThat(policy.penaltyPercentFor(30)).isZero();     // hủy sớm, ngoài mọi mốc: không phạt
        assertThat(policy.penaltyPercentFor(-1)).isEqualTo(100); // đã qua ngày khởi hành
    }

    @Test
    void overlappingTiersAreRejected() {
        assertCode(() -> new CancellationPolicy("Sai", LocalDate.now(), null,
                List.of(new PenaltyTier(0, 5, 100), new PenaltyTier(5, 10, 50))), "INVALID_PENALTY_TIERS");
        assertCode(() -> new PenaltyTier(0, 3, 120), "INVALID_PENALTY_TIERS");
    }

    @Test
    void policyCannotBeChangedOnceEffective() {
        LocalDate today = LocalDate.of(2026, 11, 14);
        CancellationPolicy effective = policyFrom(today);
        CancellationPolicy future = policyFrom(today.plusDays(1));

        assertCode(() -> effective.update("Đổi", today, null, List.of(new PenaltyTier(0, 1, 100)), today), "POLICY_ALREADY_EFFECTIVE");
        future.update("Đổi tên", today.plusDays(2), null, List.of(new PenaltyTier(0, 1, 100)), today);
        assertThat(future.getName()).isEqualTo("Đổi tên");
    }

    // ---------- Giờ mở cửa ----------

    @Test
    void openingHoursMustStartBeforeTheyEnd() {
        assertCode(() -> new TimeWindow(LocalTime.of(17, 0), LocalTime.of(8, 0)), "INVALID_OPENING_HOURS");
        assertCode(() -> new TimeWindow(LocalTime.of(8, 0), LocalTime.of(8, 0)), "INVALID_OPENING_HOURS");
    }

    private static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(code));
    }
}
