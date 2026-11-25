-- Đổi cổng thanh toán trực tuyến từ VNPay sang payOS.
-- payments_chk_2 là tên MySQL tự đặt cho ràng buộc method trong V6 (ràng buộc thứ 2 của bảng payments).
alter table payments drop check payments_chk_2;
update payments set method = 'PAYOS' where method = 'VNPAY';
alter table payments add constraint payments_method_chk check (method in ('PAYOS', 'BANK_TRANSFER', 'CASH'));
