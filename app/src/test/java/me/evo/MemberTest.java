package me.evo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemberTest {
    @Test
    void getsetTest(){
        Member member = new Member();
        member.setName("EVO");
        Assertions.assertEquals("EVO",member.getName());
    }
}

