package com.example.tientienyouthmanagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "summer_fund_donations")
public class SummerFundDonation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receipt_code", length = 50, unique = true)
    private String receiptCode; // Mã phiếu thu (VD: QHH-2026-001)

    @Column(name = "donor_name", length = 150, nullable = false)
    private String donorName; // Họ và tên người / đơn vị ủng hộ

    @Column(name = "donor_title", length = 100)
    private String donorTitle; // Danh xưng: Mạnh thường quân, Cựu đoàn viên, Bà con nhân dân, Doanh nghiệp...

    @Column(name = "village_or_unit", length = 150)
    private String villageOrUnit; // Xóm 1, Xóm 2, Xóm 3, Con em làm ăn xa quê, Doanh nghiệp...

    @Column(name = "amount", nullable = false)
    private Long amount; // Số tiền ủng hộ (VNĐ)

    @Column(name = "item_donation", length = 255)
    private String itemDonation; // Hiện vật nếu có (VD: 5 quả bóng đá, 2 thùng sữa tươi, 100 suất bánh kẹo)

    @Column(name = "donation_purpose", length = 150)
    private String donationPurpose; // Mục đích: Trại hè thiếu nhi, Giải bóng đá hè, Đêm hội văn nghệ, Khen thưởng hè...

    @Column(name = "payment_method", length = 50)
    private String paymentMethod; // Tiền mặt, Chuyển khoản VietQR, Hiện vật

    @Column(name = "donation_date")
    private LocalDate donationDate; // Ngày ủng hộ

    @Column(name = "note", columnDefinition = "TEXT")
    private String note; // Lời nhắn gửi / Lời chúc
}
