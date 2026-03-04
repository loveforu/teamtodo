package com.teamtodo.app.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FirestoreManagerTest {

    @Test
    public void buildSampleTeamDocId_truncatesAndSanitizes() {
        String uid = "abcDEF-123_456!@#this_part_is_very_long";
        String id = FirestoreManager.buildSampleTeamDocId(uid);
        assertEquals("sample-team-abcDEF-123_456this_p", id);
    }

    @Test
    public void buildSampleTeamDocId_handlesNull() {
        String id = FirestoreManager.buildSampleTeamDocId(null);
        assertEquals("sample-team-anon", id);
    }
}

