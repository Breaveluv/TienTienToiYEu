package com.example.tientienyouthmanagement;

import com.example.tientienyouthmanagement.model.YouthMember;
import com.example.tientienyouthmanagement.repository.YouthMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class YouthMemberRepositoryTest {

    @Autowired
    private YouthMemberRepository repository;

    @Test
    void testInitialDataLoaded() {
        long count = repository.count();
        assertTrue(count >= 8, "Initial sample members should be loaded into database");
    }

    @Test
    void testSearchByKeyword() {
        List<YouthMember> results = repository.searchMembers("Nguyễn Văn An", null, null);
        assertFalse(results.isEmpty());
        assertEquals("Nguyễn Văn An", results.get(0).getFullName());
    }

    @Test
    void testSearchByVillage() {
        List<YouthMember> results = repository.searchMembers(null, "Xóm 1 (Thôn Tiền Tiến)", null);
        assertFalse(results.isEmpty());
        for (YouthMember m : results) {
            assertEquals("Xóm 1 (Thôn Tiền Tiến)", m.getVillage());
        }
    }

    @Test
    void testTopVolunteers() {
        List<YouthMember> top = repository.findTop5ByOrderByVolunteerDaysDesc();
        assertFalse(top.isEmpty());
        assertTrue(top.size() <= 5);
        if (top.size() > 1) {
            assertTrue(top.get(0).getVolunteerDays() >= top.get(1).getVolunteerDays());
        }
    }

    @Test
    void testCounts() {
        long local = repository.countByResidenceStatusIgnoreCase("Tại thôn");
        assertTrue(local > 0);
        long totalVolunteers = repository.sumVolunteerDays();
        assertTrue(totalVolunteers > 0);
    }
}
