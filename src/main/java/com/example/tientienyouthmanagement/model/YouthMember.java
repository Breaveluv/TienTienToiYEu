package com.example.tientienyouthmanagement.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "youth_members")
public class YouthMember {

    @Id
    @Column(length = 50, nullable = false, unique = true)
    private String id;               // Mã đoàn viên (VD: DV-TT01)

    @Column(name = "full_name", length = 150, nullable = false)
    private String fullName;         // Họ và tên

    @Column(name = "birth_year", length = 10)
    private String birthYear;        // Năm sinh

    @Column(length = 10)
    private String gender;           // Giới tính (Nam/Nữ)

    @Column(length = 100)
    private String village;          // Xóm / Cụm dân cư trong Thôn Tiền Tiến

    @Column(length = 100)
    private String position;         // Chức vụ

    @Column(name = "union_card_number", length = 50)
    private String unionCardNumber;  // Số thẻ đoàn viên

    @Column(name = "join_date", length = 30)
    private String joinDate;         // Ngày kết nạp Đoàn

    @Column(name = "residence_status", length = 50)
    private String residenceStatus;  // Tình trạng sinh hoạt ("Tại thôn", "Đi làm ăn xa", "Sinh viên học xa", "Tạm vắng")

    @Column(length = 30)
    private String phone;            // Số điện thoại

    @Column(length = 50)
    private String classification;   // Xếp loại cuối năm ("Xuất sắc", "Khá", "Trung bình")

    @Column(length = 500)
    private String avatar;           // Ảnh đại diện

    @Column(name = "activity_notes", columnDefinition = "TEXT")
    private String activityNotes;    // Ghi chú hoạt động ở Thôn Tiền Tiến

    @Column(name = "volunteer_days")
    private int volunteerDays;       // Số buổi tham gia Ngày Chủ nhật xanh
}
